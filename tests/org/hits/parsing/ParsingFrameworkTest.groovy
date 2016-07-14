package org.hits.parsing

import groovy.json.JsonSlurper
import spock.lang.Specification

/**
 * Created by bittkomk on 19/08/14.
 */
class ParsingFrameworkTest extends Specification {

    def pf = new ParsingFramework()
    def headerFormat = new HeaderFormatFactoryBuilder().headerFormat {
        row "l1"
        row "l2"
        row "l3"
    }
    def eb = new EntityDescriptorFactoryBuilder()
    def entity = { eargs, closure -> eb.entity(eargs, closure) } // context needed for parsing DSL below
    def parsingClosure = {

        parseEntity entity(ofKind: "entity1") {
            parseProperty necessary: "id", from: "id"
            parseProperty "id2", from: 1
            parseProperty "fulltitle", from: ["title", "additional title"]
            parseProperty "fulltitle2", from: [2, 3]

            parseProperty "sfxheader", from: "header@l1", of: "sfx"
            parseProperty "sthImportantAtL3", from: "header@l3", of: "sfx"

            applyTransformation "trans1", ofElements: { it += " via colIndex" }, onProperty: "id2"
            applyTransformation "trans2", ofList: { it?.join("--") }, onProperty: "fulltitle"
            applyTransformation "trans3", ofElements: { it?.toUpperCase() }, ofList: {
                (it?.join("--")) + " via colIndex"
            }, onProperty: "fulltitle2"
        }

        parseEntity entity(ofKind: "entity2") {
            parseProperty necessary: "id", from: 5
            parseProperty "funkadelic", from: 6
            parseProperty "velocity", from: 7
            parseProperty "velocity unit", from: 7

            def veloRegex = /(\d*)\s*(.*)/

            applyTransformation "trans6", ofElements: { value ->
                if ((m = value =~ veloRegex).matches()) {
                    return m.group(1)
                }; ""
            }, onProperty: "velocity"
            applyTransformation "trans7", ofElements: { value ->
                if ((m = value =~ veloRegex).matches()) {
                    return m.group(2)
                }; ""
            }, onProperty: "velocity unit"

            // ...

            linkTo "entity1" // default: entityInSameRow: true, optinal: withID: someID
        }
    }
    def efb = new ExportFormatFactoryBuilder()
    def exportFormat = efb.exportFormat {
        headerFormat (forSheet:"entity2") {
            row {
                col "id"
                col "velocity"
                col "velocity unit"
                col "very important"
                col "belongsTo"
            }
        }
        headerFormat (forSheet:"entity1") {
            row {
                col "id"
                col "full title"
                col "from l3 with love"
            }
        }
        map (entity: "entity2", toSheet:"entity2") {
            property "funkadelic", to: "very important"
            property "linksTo", to: "belongsTo"
        }

        map (entity: "entity1", toSheet:"entity1") {
            property "fulltitle2", to: "full title"
            property "sthImportantAtL3", to: "from l3 with love"
        }
    }

    def "loading workbook and setting sheet to use"() {
        when:
        pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data")

        then:
        pf.workbook != null
        pf.sheet != null
        pf.sheetName == pf.sheet.getSheetName()
    }

    def "getting datarows out of xlsx without headerFormat"() {
        when:
        def dataRows = pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").dataRows()

        then:
        dataRows != null
        dataRows instanceof List<DataRow>
        dataRows.size() == 5
        dataRows.first().rowNumber == 2 // first row is taken as the default header
        dataRows.first().columns != null
        dataRows.first().columns instanceof List<Column>
        dataRows.first().columns.size() == 20
        dataRows.first().columns.first().header == "id"
        dataRows.first().columns.first().value == "l2"
    }

    def "getting datarows out of xlsx with headerFormat"() {
        when:
        def dataRows = pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").dataRows(headerFormat)

        then:
        dataRows != null
        dataRows instanceof List<DataRow>
        dataRows.size() == 3
        dataRows.first().rowNumber == 4
        dataRows.first().header.flatten().find { h -> h.descriptor == "l2" && h.rowIndex == 2 }
        dataRows.first().columns != null
        dataRows.first().columns instanceof List<Column>
        dataRows.first().columns.size() == 20
        dataRows.first().columns.first().header == "id.l2.l3"
        dataRows.first().columns.first().value == "156"
    }

    def "transposing sheet and extracting datarows out of it"() {
        when:
        def dataRows = pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").transpose().dataRows()

        then:
        pf.sheet.getSheetName() == "transposed_data"
        dataRows.size() == 19
        dataRows.first().rowNumber == 2
        dataRows.first().columns.size() == 6
        dataRows.first().columns.first().header == "id"
        dataRows.first().columns.first().value == "title"
    }

    def "parsing sheet with parsing DSL and headerFormat"() {
        when:
        pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").parse(parsingClosure, headerFormat)
        def entities1 = pf.getParsedEntities("entity1")
        def entities2 = pf.getParsedEntities("entity2")

        then:
        pf.parsedEntities.size() != 0
        entities1.size() == 2 // necessary id is missing for one datarow
        entities2.size() == 3
        entities1.first().id == "156"
        entities1.first().id2 == "156 via colIndex"
        entities1.first().fulltitle == "first one--add me"
        entities1.first().fulltitle2 == "FIRST ONE--ADD ME via colIndex"
        entities1.first().sfxheader == "sfx"
        entities1.first().sthImportantAtL3 == "IMPORTANZE"
        entities2.first().id == "651"
        entities2.first().funkadelic == "yes"
        entities2.first().velocity == "10"
        entities2.first()."velocity unit" == "km/h"
        entities2.first().linksTo.find { li -> li.kind == "entity1" && li.id == "156" }
    }

    def "exporting parsed entities to JSON -- full output, standard"() {
        when:
        def json = new JsonSlurper().parseText(pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").parse(parsingClosure, headerFormat).toJSONString())

        then:
        json != null
        json["entity1"].size() == 2
        json["entity2"].size() == 3

        json["entity1"].first().fromRow == 4
        json["entity1"].first().id == "156"
        json["entity1"].first().necessaryProperties == ["id"]
        json["entity1"].first().provenanceInformation != []
        json["entity1"].first().linksTo == null

        json["entity2"].first().fromRow == 4
        json["entity2"].first().id == "651"
        json["entity2"].first().necessaryProperties == ["id"]
        json["entity2"].first().provenanceInformation != []
        json["entity2"].first().linksTo == [[kind:"entity1", id:"156"]]

    }

    def "exporting parsed entities to JSON -- just the entities"() {
        when:
        def json = new JsonSlurper().parseText(pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").parse(parsingClosure, headerFormat).toJSONString(false))

        then:
        json != null
        json["entity1"].size() == 2
        json["entity2"].size() == 3

        json["entity1"].first().fromRow == null
        json["entity1"].first().id == "156"
        json["entity1"].first().necessaryProperties == null
        json["entity1"].first().provenanceInformation == null
        json["entity1"].first().linksTo == null

        json["entity2"].first().fromRow == null
        json["entity2"].first().id == "651"
        json["entity2"].first().necessaryProperties == null
        json["entity2"].first().provenanceInformation == null
        json["entity2"].first().linksTo == [[kind:"entity1", id:"156"]]

    }

    def "exporting parsed entities to JSON -- just the entities, links as properties"() {
        when:
        def json = new JsonSlurper().parseText(pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").parse(parsingClosure, headerFormat).toJSONString(false, true))

        then:
        json != null
        json["entity1"].size() == 2
        json["entity2"].size() == 3

        json["entity1"].first().fromRow == null
        json["entity1"].first().id == "156"
        json["entity1"].first().necessaryProperties == null
        json["entity1"].first().provenanceInformation == null
        json["entity1"].first().linksTo == null

        json["entity2"].first().fromRow == null
        json["entity2"].first().id == "651"
        json["entity2"].first().necessaryProperties == null
        json["entity2"].first().provenanceInformation == null
        json["entity2"].first().linksTo == null
        json["entity2"].first().link1 == "entity1:156"

    }

    def "exporting to XLS with custom export format"() {
        when:
        def outFileName = "test-resources/parsed_testdata.xlsx"
        pf.loadWorkbook("test-resources/testdata.xlsx").useSheet("data").parse(parsingClosure, headerFormat).toXLS(outFileName, exportFormat, true)
        def outFile = new File(outFileName)

        then:
        outFile.exists()
        outFile.length() > 0
        ExcelHelper.loadWorkbook(outFileName).getSheet("entity1")
        ExcelHelper.loadWorkbook(outFileName).getSheet("entity2")
        ExcelHelper.loadWorkbook(outFileName).getSheet("entity2").getRow(0).collect { cell -> ExcelHelper.readCell(cell) } == ["id", "velocity", "velocity unit", "very important", "belongsTo"]
        ExcelHelper.loadWorkbook(outFileName).getSheet("entity2").getRow(1).collect { cell -> ExcelHelper.readCell(cell) } == ["651", "10", "km/h", "yes", "entity1:156"]
    }

    def "filtering entities for XLS export"() {
        setup:
        def entities1 = [[id:1, name:"best", kind:"e1"], [id:2, name:"test", kind:"e1"], [id:3, name:"ever", kind:"e1"], [id:4, name:"ever", kind:"e1"]]
        def entities1Doubled = entities1 + entities1
        def entities2 = [[id:1, name:"best", kind:"e2"], [id:2, name:"test", kind:"e2"], [id:3, name:"ever", kind:"e2"], [id:3, name:"ever!", kind:"e2"]]

        when:
        def filteredEntities1 = pf.filterEntitiesForXLSExport(entities1, true)
        def filteredEntities2 = pf.filterEntitiesForXLSExport(entities1Doubled, true)
        def filteredEntities3 = pf.filterEntitiesForXLSExport(entities1Doubled, false)
        def filteredEntities4 = pf.filterEntitiesForXLSExport(entities1Doubled, ["e1", "e2", "e3"])
        def filteredEntities5 = pf.filterEntitiesForXLSExport(entities1Doubled, ["e2", "e3"])
        def filteredEntities6 = pf.filterEntitiesForXLSExport(entities1, ["e1:name", "e2:id", "e3"])
        def filteredEntities7 = pf.filterEntitiesForXLSExport(entities2, ["e1:name", "e2:id", "e3"])
        def filteredEntities8 = pf.filterEntitiesForXLSExport(entities2, ["e1:name", "e2", "e3"])

        then:
        entities1Doubled.size() == entities1.size() * 2
        filteredEntities1.size() == entities1.size() // already unique
        filteredEntities2.size() == entities1.size() // reduced to entities1
        filteredEntities3.size() == entities1Doubled.size() // do not filter -- makes only sense in the whole of toXLS, refactor?
        filteredEntities4.size() == entities1.size() // more fine-grained control over which kind of entities to filter (on which property, see below)
        filteredEntities5.size() == entities1Doubled.size() // kind "e1" is not in list
        filteredEntities6.size() == entities1.size() - 1 // filter on property name; name:'ever' is non-unique
        filteredEntities7.size() == entities2.size() - 1 // id:3 is non-unique
        filteredEntities8.size() == filteredEntities7.size() // id is default field to filter on
    }

    def "creating headers and adding them to workbook"() {
        setup:
        def wb = ExcelHelper.createWorkbook()

        when:
         pf.createAndAddHeaderToXLS(wb, exportFormat)

        then:
        wb.getSheet("entity1").getRow(0).collect { cell -> ExcelHelper.readCell(cell) } == ["id", "full title", "from l3 with love"]
        wb.getSheet("entity2").getRow(0).collect { cell -> ExcelHelper.readCell(cell) } == ["id", "velocity", "velocity unit", "very important", "belongsTo"]
    }

    def "formatting linkTo property for XLS export"(entity, formattedLink) {
        expect:
        formattedLink == pf.formatLinkForXLSExport(entity)
        where:
        entity                          |   formattedLink
        [id:1, kind: "e1"]              |   "Link target not defined"
        [id:1, kind: "e1", linksTo: []] |   "Link target not defined"
        [id:2, kind: "e1", linksTo: [[kind: "other", id:16]]] | "other:16"
        [id:2, kind: "e1", linksTo: [[kind: "other", id:16], [kind:'other other', id: 32]]] | "other:16;other other:32"
    }

    def "checking parsed entities for necessary properties"() {
        setup:
        def np = ["id", "name"]
        def entities = [[id:1, name:"joe", kind:"e1", fromRow:1, necessaryProperties:np],
                        [id:2, name:"jane", kind:"e1", fromRow:2, necessaryProperties:np],
                        [id:3, kind:"e1", fromRow:3, necessaryProperties:np],
                        [name:"rudy", kind:"e1", fromRow:4, necessaryProperties:np]]
        entities.each { e -> pf.addParsedEntity(e) }

        when:
        def parsedEntities = pf.getParsedEntities("e1")
        def parsedEntityFromRow4 = pf.getParsedEntities("e1", 4)
        pf.checkParsedEntities() // check for necessary properties; filters out entities with missing necessary properties
        def checkedParsedEntities = pf.getParsedEntities("e1")
        def checkedParsedEntityFromRow4 = pf.getParsedEntities("e1", 4)

        then:
        parsedEntities.size() == 4
        parsedEntityFromRow4.find { it.name == "rudy" && it.fromRow == 4 }
        checkedParsedEntities.size() == 2 // removed entities from fromRow:3 --> missing name and fromRow:4 --> missing id
        !checkedParsedEntityFromRow4 // entity from row 4 was filtered out
    }

    def "asking for a linking id to an entity"() {
        setup:
        def e1 = [id:1, name:"joe", kind:"e1", fromRow:1]
        def e2 = [id:2, name:"jane", kind:"e1", fromRow:2]
        def e3 = [id:3, value:123, kind:"e2", fromRow:1]
        def e4 = [id:4, value:456, kind:"e2", fromRow:2]
        [e1, e2, e3, e4].each { e -> pf.addParsedEntity(e) }
        def ld1 = new LinkingDescriptor(entityKind: "e2")
        def ld2 = new LinkingDescriptor(entityKind: "e2", where: { src, dest -> dest.value == 456} )
        def ld3 = new LinkingDescriptor(entityKind: "e2", linkToProperty: "value")
        def ld4 = new LinkingDescriptor(entityKind: "e2", where: { src, dest -> dest.value > src.value}, linkToProperty: "value" )

        when:
        def link1 = pf.askForLinkID(ld1, 1, e1) // get id as id to link to an entity of kind "e2" in the same row
        def link2 = pf.askForLinkID(ld2, 1, e1) // get id as id to link to an entity of kind "e2" that has value == 456
        def link3 = pf.askForLinkID(ld3, 1, e1) // get value as id to link to an entity of kind "e2" in the same row
        def link4 = pf.askForLinkID(ld4, 1, e3) // get value as id to link to an entity of kind "e2" with higher value than linking source (= e3)

        then:
        link1 == e3.id
        link2 == e4.id
        link3 == e3.value
        link4 == e4.value


    }
}

