package org.hits.parsing

import spock.lang.Specification

/**
 * Created by bittkomk on 19/08/14.
 */
class DataRowTest extends Specification {

    def pf = new ParsingFramework()
    def eb = new EntityDescriptorFactoryBuilder()

    // some constants to simulate the binding used for processing the DSL scripts
    // --> this test suites provides some documentation about the parsing DSL
    final FIRST = 1
    final SECOND = 2
    final THIRD = 3
    final FOURTH = 4
    final FIFTH = 5
    final SIXTH = 6
    final SEVENTH = 7
    final STRICT = MatchingStyle.STRICT
    final EXACT = MatchingStyle.STRICT
    final PREFIX = MatchingStyle.PREFIX
    final SUFFIX = MatchingStyle.SUFFIX
    final INFIX = MatchingStyle.INFIX
    final SUBSTRING = MatchingStyle.INFIX

    // Name | Age | Profession | Pet | Pet Species
    def simpleHeader = [
                        1: [[rowIndex: 1, colIndex: 1, value: "Name", descriptor: "header"]],
                        2: [[rowIndex: 1, colIndex: 2, value: "Age", descriptor: "header"]],
                        3: [[rowIndex: 1, colIndex: 3, value: "Profession", descriptor: "header"]],
                        4: [[rowIndex: 1, colIndex: 4, value: "Pet", descriptor: "header"]],
                        5: [[rowIndex: 1, colIndex: 5, value: "Pet Species", descriptor: "header"]]
    ]
    // John Doe | 33 | Artist | Grumpy | Cat
    def dr1Columns = [
                      new Column(rowIndex: 2, colIndex: 1, value: "John Doe", header: "Name", fullHeader: simpleHeader[1]),
                      new Column(rowIndex: 2, colIndex: 2, value: "33", header: "Age", fullHeader: simpleHeader[2]),
                      new Column(rowIndex: 2, colIndex: 3, value: "Artist", header: "Profession", fullHeader: simpleHeader[3]),
                      new Column(rowIndex: 2, colIndex: 4, value: "Grumpy", header: "Pet", fullHeader: simpleHeader[4]),
                      new Column(rowIndex: 2, colIndex: 5, value: "Cat", header: "Pet Species", fullHeader: simpleHeader[5])
    ]
    def dr1 = new DataRow(parsingFramework: pf, rowNumber: 2, header: simpleHeader.values(), columns: dr1Columns)
    // Jane Doe | 22 | Software Developer
    def dr2Columns = [
            new Column(rowIndex: 3, colIndex: 1, value: "Jane Doe", header: "Name", fullHeader: simpleHeader[1]),
            new Column(rowIndex: 3, colIndex: 2, value: "22", header: "Age", fullHeader: simpleHeader[2]),
            new Column(rowIndex: 3, colIndex: 3, value: "Software Developer", header: "Profession", fullHeader: simpleHeader[3])
            //new Column(rowIndex: 3, colIndex: 4, value: "Grumpy", header: "Pet", fullHeader: simpleHeader[4]),
            //new Column(rowIndex: 3, colIndex: 5, value: "Cat", header: "Pet Species", fullHeader: simpleHeader[5])
    ]
    def dr2 = new DataRow(parsingFramework: pf, rowNumber: 3, header: simpleHeader.values(), columns: dr2Columns)
    // Hans Wurst | 44 | Hans Wurst | Friedrich | Sausage dog
    def dr3Columns = [
            new Column(rowIndex: 4, colIndex: 1, value: "Hans Wurst", header: "Name", fullHeader: simpleHeader[1]),
            new Column(rowIndex: 4, colIndex: 2, value: "44", header: "Age", fullHeader: simpleHeader[2]),
            new Column(rowIndex: 4, colIndex: 3, value: "Hans Wurst", header: "Profession", fullHeader: simpleHeader[3]),
            new Column(rowIndex: 4, colIndex: 4, value: "Friedrich", header: "Pet", fullHeader: simpleHeader[4]),
            new Column(rowIndex: 4, colIndex: 5, value: "Sausage Dog", header: "Pet Species", fullHeader: simpleHeader[5])
    ]
    def dr3 = new DataRow(parsingFramework: pf, rowNumber: 4, header: simpleHeader.values(), columns: dr3Columns)

    def petDescriptor = eb.entity(ofKind: "pet") {
        parseProperty "id", from: ["pet", "pet species"]
        parseProperty "name", from: "pet"
        parseProperty "species", from: "pet species"

        applyTransformation "create id", ofElements: { it.toUpperCase() }, ofList: { it.reverse().join("-") }, onProperty: "id"
    }


    // Treatment | Treatment | Treatment
    // Kind      | Value     | Substance
    //           | mmol/s    | Glucose
    def complexHeader = [
            1: [[rowIndex: 1, colIndex: 1, value: "Treatment", descriptor: "level1"],
                [rowIndex: 2, colIndex: 1, value: "Kind", descriptor: "level2"],
                [rowIndex: 3, colIndex: 1, value: "", descriptor: "level3"]],
            2: [[rowIndex: 1, colIndex: 2, value: "Treatment", descriptor: "level1"],
                [rowIndex: 2, colIndex: 2, value: "Value", descriptor: "level2"],
                [rowIndex: 3, colIndex: 2, value: "mmol/s", descriptor: "level3"]],
            3: [[rowIndex: 1, colIndex: 3, value: "Treatment", descriptor: "level1"],
                [rowIndex: 2, colIndex: 3, value: "Substance", descriptor: "level2"],
                [rowIndex: 3, colIndex: 3, value: "Glucose", descriptor: "level3"]],
            4: [[rowIndex: 1, colIndex: 4, value: "Treatment", descriptor: "level1"],
                [rowIndex: 2, colIndex: 4, value: "Value", descriptor: "level2"],
                [rowIndex: 3, colIndex: 4, value: "hours", descriptor: "level3"]]
    ]

    // Glucose Pulse | 120 |
    def dr4Columns = [
            new Column(rowIndex: 4, colIndex: 1, value: "Glucose Pulse", header: "Treatment.Kind", fullHeader: complexHeader[1]),
            new Column(rowIndex: 4, colIndex: 2, value: "120", header: "Treatment.Value.mmols/s", fullHeader: complexHeader[2]),
            new Column(rowIndex: 4, colIndex: 3, value: "", header: "Treatment.Substance.Glucose", fullHeader: complexHeader[3]),
            new Column(rowIndex: 4, colIndex: 4, value: "1.5", header: "Treatment.Value.hours", fullHeader: complexHeader[4])
    ]

    def dr4 = new DataRow(parsingFramework: pf, rowNumber: 4, header: complexHeader.values(), columns: dr4Columns)

    // name | age | profession | name | age | profession | name | age
    def columnGroupsByPatternHeader = [
            1: [[rowIndex: 1, colIndex: 1, value: "name", descriptor: "header"]],
            2: [[rowIndex: 1, colIndex: 2, value: "age", descriptor: "header"]],
            3: [[rowIndex: 1, colIndex: 3, value: "profession", descriptor: "header"]],
            4: [[rowIndex: 1, colIndex: 4, value: "name", descriptor: "header"]],
            5: [[rowIndex: 1, colIndex: 5, value: "age", descriptor: "header"]],
            6: [[rowIndex: 1, colIndex: 6, value: "profession", descriptor: "header"]],
            7: [[rowIndex: 1, colIndex: 7, value: "name", descriptor: "header"]],
            8: [[rowIndex: 1, colIndex: 8, value: "age", descriptor: "header"]]
    ]

    // John Doe | 33 | Artist | Jane Doe | 22 | Software Developer | Hans Wurst | 44
    def dr5Columns = [
            new Column(rowIndex: 2, colIndex: 1, value: "John Doe", header: "name", fullHeader: columnGroupsByPatternHeader[1]),
            new Column(rowIndex: 2, colIndex: 2, value: "33", header: "age", fullHeader: columnGroupsByPatternHeader[2]),
            new Column(rowIndex: 2, colIndex: 3, value: "Artist", header: "profession", fullHeader: columnGroupsByPatternHeader[3]),
            new Column(rowIndex: 2, colIndex: 4, value: "Jane Doe", header: "name", fullHeader: columnGroupsByPatternHeader[4]),
            new Column(rowIndex: 2, colIndex: 5, value: "22", header: "age", fullHeader: columnGroupsByPatternHeader[5]),
            new Column(rowIndex: 2, colIndex: 6, value: "Software Developer", header: "profession", fullHeader: columnGroupsByPatternHeader[6]),
            new Column(rowIndex: 2, colIndex: 7, value: "Hans Wurst", header: "name", fullHeader: columnGroupsByPatternHeader[7]),
            new Column(rowIndex: 2, colIndex: 8, value: "44", header: "age", fullHeader: columnGroupsByPatternHeader[8])
    ]
    def dr5 = new DataRow(parsingFramework: pf, rowNumber: 2, header: columnGroupsByPatternHeader.values(), columns: dr5Columns)

    // treatment1 | treatment1 | treatment2 | treatment3 | treatment3 | treatment4
    // substance  | value      | substance  | substance  | value      | value
    // coffee     | 12 cups    | cookies    | tea        | 24 cups    | dozens
    def columnGroupsByRegexHeader = [
            1: [[rowIndex: 1, colIndex: 1, value: "treatment1", descriptor: "treatment"],
                [rowIndex: 2, colIndex: 1, value: "substance", descriptor: "property"]],
            2: [[rowIndex: 1, colIndex: 2, value: "treatment1", descriptor: "treatment"],
                [rowIndex: 2, colIndex: 2, value: "value", descriptor: "property"]],
            3: [[rowIndex: 1, colIndex: 3, value: "treatment2", descriptor: "treatment"],
                [rowIndex: 2, colIndex: 3, value: "substance", descriptor: "property"]],
            4: [[rowIndex: 1, colIndex: 4, value: "treatment3", descriptor: "treatment"],
                [rowIndex: 2, colIndex: 4, value: "substance", descriptor: "property"]],
            5: [[rowIndex: 1, colIndex: 5, value: "treatment3", descriptor: "treatment"],
                [rowIndex: 2, colIndex: 5, value: "value", descriptor: "property"]],
            6: [[rowIndex: 1, colIndex: 6, value: "treatment4", descriptor: "treatment"],
                [rowIndex: 2, colIndex: 6, value: "value", descriptor: "property"]]
    ]

    // coffee | 12 cups | cookies | tea | 24 cups | dozens
    def dr6Columns = [
           new Column(rowIndex: 3, colIndex: 1, value: "coffee", header: "treatment1.substance", fullHeader: columnGroupsByRegexHeader[1]),
           new Column(rowIndex: 3, colIndex: 2, value: "12 cups", header: "treatment1.value", fullHeader: columnGroupsByRegexHeader[2]),
           new Column(rowIndex: 3, colIndex: 3, value: "cookies", header: "treatment2.substance", fullHeader: columnGroupsByRegexHeader[3]),
           new Column(rowIndex: 3, colIndex: 4, value: "tea", header: "treatment3.substance", fullHeader: columnGroupsByRegexHeader[4]),
           new Column(rowIndex: 3, colIndex: 5, value: "24 cups", header: "treatment3.value", fullHeader: columnGroupsByRegexHeader[5]),
           new Column(rowIndex: 3, colIndex: 6, value: "dozens", header: "treatment4.value", fullHeader: columnGroupsByRegexHeader[6])
    ]

    def dr6 = new DataRow(parsingFramework: pf, rowNumber: 3, header: columnGroupsByRegexHeader.values(), columns: dr6Columns)

    def "parsing one entity with three properties"() {
        setup:
        def personDescriptor1 = eb.entity(ofKind: "person") {
            parseProperty "name", from: "Name"
            parseProperty "age", from: "Age"
            parseProperty "profession", from: "Profession"
        }

        def newEntity = [kind: "person", fromRow: 2, provenanceInformation: [], necessaryProperties: []]

        when:
        personDescriptor1.properties.each { pd ->
            newEntity = dr1.parseProperty(pd, newEntity)
        }

        then:
        newEntity.name == "John Doe"
        newEntity.age == "33"
        newEntity.profession == "Artist"
        newEntity.provenanceInformation != []
        newEntity.necessaryProperties == []
    }

    def "parsing one entity with three properties one of them necessary"() {
        setup:
        def personDescriptor2 = eb.entity(ofKind: "person") {
            parseProperty necessary:"name", from: "Name" // this makes "name" a necessary property
            parseProperty "age", from: "Age"
            parseProperty "profession", from: "Profession"
        }

        def newEntity = [kind: "person", fromRow: 2, provenanceInformation: [], necessaryProperties: []]

        when:
        personDescriptor2.properties.each { pd ->
            newEntity = dr1.parseProperty(pd, newEntity)
        }

        then:
        newEntity.name == "John Doe"
        newEntity.age == "33"
        newEntity.profession == "Artist"
        newEntity.provenanceInformation != []
        newEntity.necessaryProperties == ["name"]
    }

    def "parsing two entities with three properties using different matching styles"() {
        setup:
        def personDescriptor3 = eb.entity(ofKind: "person") {
            parseProperty "name", from: "Na" // standard matching is PREFIX matching, so this will match to header column "Name"
            parseProperty "age", from: "ge", matchingStyle: SUFFIX // standard matching style can be overwritten, e.g. to SUFFIX matching, will match "Age"
            parseProperty "profession", from: "ofess", matchingStyle: SUBSTRING // matching SUBSTRING, will mach header column "Profession"
        }
        // the default matching style can be changed per entity to parse
        def personDescriptor4 = eb.entity(ofKind: "person", matchingStyle: SUBSTRING) {
            parseProperty "name", from: "am"
            parseProperty "age", from: "Age", matchingStyle: EXACT // the overwritten entity default matching style can be overwritten too
            parseProperty "profession", from: "ofessio"
        }
        def newEntity1 = [kind: "person", fromRow: 2, provenanceInformation: [], necessaryProperties: []]
        def newEntity2 = [kind: "person", fromRow: 2, provenanceInformation: [], necessaryProperties: []]

        when:
        personDescriptor3.properties.each { pd ->
            newEntity1 = dr1.parseProperty(pd, newEntity1)
        }
        personDescriptor4.properties.each { pd ->
            newEntity2 = dr1.parseProperty(pd, newEntity2)
        }


        then:
        newEntity1.name == "John Doe"
        newEntity1.age == "33"
        newEntity1.profession == "Artist"
        newEntity2.name == newEntity1.name
        newEntity2.age == newEntity1.age
        newEntity2.profession == newEntity1.profession
    }

    def "parsing one entity with three properties using column indices"() {
        setup:
        // when knowing the exact column order it might be also convenient to access parsing targets by their column index (1 based)
        def personDescriptor5 = eb.entity(ofKind: "person") {
            parseProperty "name", from: 1
            parseProperty "age", from: 2
            parseProperty "profession", from: 3
        }

        def newEntity = [kind: "person", fromRow: 2, provenanceInformation: [], necessaryProperties: []]

        when:
        personDescriptor5.properties.each { pd ->
            newEntity = dr1.parseProperty(pd, newEntity)
        }

        then:
        newEntity.name == "John Doe"
        newEntity.age == "33"
        newEntity.profession == "Artist"
        newEntity.provenanceInformation != []
        newEntity.necessaryProperties == []
    }

    def "parsing two columns into one property and applying transformations"() {
        setup:
        def personDescriptor6 = eb.entity(ofKind: "person") {
            parseProperty "id", from: [1, "age"] // it's possible to parse values from different locations
            parseProperty "name", from: "name" // btw. matching is not case sensitive
            parseProperty "age", from: "AGE"
            parseProperty "profession", from: 3

            // property id is parsed from two columns; the result will be a list of two elements [name, age]
            // the DSL offers a mechanism to postprocess this list by applying transformations
            // this transforms every element in the list and then applies an additional transformation on the whole list
            applyTransformation "create id", ofElements: { it.toUpperCase() }, ofList: { it.join("-") }, onProperty: "id"
            // single element properties can also be transformed
            applyTransformation "double age", ofElements: { (it.toInteger() * 2).toString() }, onProperty: "age"
        }

        def newEntity = [kind: "person", fromRow: 2, provenanceInformation: [], necessaryProperties: []]

        when:
        personDescriptor6.properties.each { pd ->
            newEntity = dr1.parseProperty(pd, newEntity)
        }

        then:
        newEntity.id == "JOHN DOE-33"
        newEntity.name == "John Doe"
        newEntity.age == "66"
        newEntity.profession == "Artist"
        newEntity.provenanceInformation != []
        newEntity.necessaryProperties == []
    }

    def "linking to parsed entities in the same row"() {
        setup:
        def personDescriptor7 = eb.entity(ofKind: "person") {
            parseProperty "id", from: ["name", "age"]
            parseProperty "name", from: "name"
            parseProperty "age", from: "age"
            parseProperty "profession", from: "profession"

            applyTransformation "create id", ofElements: { it.toUpperCase() }, ofList: { it.join("-") }, onProperty: "id"

            linkTo "pet" // this will try to create link to an entity of kind "pet" in the same row
        }

        when:
        [dr1, dr2, dr3].each { dr ->
            dr.parseEntity(personDescriptor7)
            dr.parseEntity(petDescriptor)
            dr.postprocess()
        }
        pf.checkParsedEntities()
        def person1 = pf.getParsedEntities("person", 2).first()
        def person2 = pf.getParsedEntities("person", 3).first()
        def person3 = pf.getParsedEntities("person", 4).first()

        then:
        person1.linksTo == [[kind:"pet", id:"CAT-GRUMPY"]]
        !person2.linksTo
        person3.linksTo == [[kind:"pet", id:"SAUSAGE DOG-FRIEDRICH"]]
    }

    def "making links to parsed entities necessary"() {
        setup:
        def personDescriptor8 = eb.entity(ofKind: "person") {
            parseProperty "id", from: ["name", "age"]
            parseProperty "name", from: "name"
            parseProperty "age", from: "age"
            parseProperty "profession", from: "profession"

            applyTransformation "create id", ofElements: { it.toUpperCase() }, ofList: { it.join("-") }, onProperty: "id"

            linkTo necessary:"pet" // this will try to create a necessary link to an entity of kind "pet" in the same row
        }

        when:
        [dr1, dr2, dr3].each { dr ->
            dr.parseEntity(personDescriptor8)
            dr.parseEntity(petDescriptor)
            dr.postprocess()
        }
        pf.checkParsedEntities()
        def person1 = pf.getParsedEntities("person", 2)
        def person2 = pf.getParsedEntities("person", 3)
        def person3 = pf.getParsedEntities("person", 4)

        then:
        person1.first().linksTo == [[kind:"pet", id:"CAT-GRUMPY"]]
        !person2 // no necessary link means that this person got removed from the set of parsed entities
        person3.first().linksTo == [[kind:"pet", id:"SAUSAGE DOG-FRIEDRICH"]]
    }

    def "making links to a specific id"() {
        setup:
        def personDescriptor8 = eb.entity(ofKind: "person") {
            parseProperty "id", from: ["name", "age"]
            parseProperty "name", from: "name"
            parseProperty "age", from: "age"
            parseProperty "profession", from: "profession"

            applyTransformation "create id", ofElements: { it.toUpperCase() }, ofList: { it.join("-") }, onProperty: "id"

            linkTo necessary:"pet", withID: "SAUSAGE DOG-FRIEDRICH" // this will try to create a necessary link to an entity of kind "pet" with the given id
        }

        when:
        dr3.parseEntity(personDescriptor8)
        dr3.parseEntity(petDescriptor)
        dr3.postprocess()
        pf.checkParsedEntities()
        def person2 = pf.getParsedEntities("person", 4)

        then:
        person2 // although there is no pet in the same row there is a pet specified by a given id (which exists in the list of parsed entities) -- so the necessary condition is met
        person2.first().linksTo == [[kind:"pet", id:"SAUSAGE DOG-FRIEDRICH"]]
    }

    def "finding link target by specifying a where clause. naming a link"() {
        setup:
        def personDescriptor9 = eb.entity(ofKind: "person") {
            parseProperty "id", from: ["name", "age"]
            parseProperty "name", from: "name"
            parseProperty "age", from: "age"
            parseProperty "profession", from: "profession"

            applyTransformation "create id", ofElements: { it.toUpperCase() }, ofList: { it.join("-") }, onProperty: "id"

            // links can be named too; this name will then be used for the jsonExport if links are to be exported as properties
            linkTo necessary:"pet", where: { src, target -> target.species == "Cat" }, name: "catlink" // this will try to create a necessary link to an entity of kind "pet" where the given where-clause is matched
        }

        when:
        [dr1, dr2, dr3].each { dr ->
            dr.parseEntity(personDescriptor9)
            dr.parseEntity(petDescriptor)
            dr.postprocess()
        }
        pf.checkParsedEntities()
        def person2 = pf.getParsedEntities("person", 3)

        then:
        person2 // although there is no pet in the same row there is a pet that meets the specified where-clause -- so the necessary condition is met
        person2.first().linksTo == [[kind:"pet", id:"CAT-GRUMPY", name:"catlink"]]
    }

    def "parsing using a complex header"() {
        setup:
        def treatmentDescriptor1 = eb.entity(ofKind: "treatment") {
            // complex header can be accessed by using a row1.row2.row3... format.
            // standard matching-style is prefix matching, so it's possible to use just the first n header rows to search for a match
            parseProperty "kind of treatment", from: "Treatment.Kind"
            // by default values are parsed from the cells *below* the header definition
            // (the default case is equivalent to: parseProperty: "prop", from: "column", of: "what to match")
            // sometimes it can be required to parse values out of (parts of) the header
            // then the location is specified in the from attribute
            // either "header" or "header@{one of your header row descriptors}"
            // in this case the of: attribute needs to be present to store the information about the match to search for
            parseProperty "substance", from: "header@level3", of: "Substance", matchingStyle: INFIX // change matchingStyle to have cleaner matching targets
            parseProperty "value", from: "Treatment.Value" // this from attribute actually matches *two* columns. default is take the first one. can be overwritten using take: , see below
            parseProperty "value unit", from: "header@level3", of: "Treatment.Value"
            // sometimes it is necessary to match exactly against a certain element of the header
            parseProperty "time", from: "level2=Value", take: SECOND, matchingStyle: EXACT // takes the second matching column
            parseProperty "time unit", from: "header@level3", of: "level2=Value", take: SECOND, matchingStyle: EXACT
        }

        def newEntity = [kind: "treatment", fromRow: 4, provenanceInformation: [], necessaryProperties: []]

        when:
        treatmentDescriptor1.properties.each { pd ->
            newEntity = dr4.parseProperty(pd, newEntity)
        }

        then:
        newEntity."kind of treatment" == "Glucose Pulse"
        newEntity.substance == "Glucose"
        newEntity.value == "120"
        newEntity."value unit" == "mmol/s"
        newEntity.time == "1.5"
        newEntity."time unit" == "hours"
    }

    def "Generating a columnGroup by using repeating chunks of columns"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(start: 1, size: 3, repetitions: 3)

        when:
        def columnGroups = dr5.createColumnGroups(cgDescriptor)

        then:
        columnGroups != []
        columnGroups.size() == 2 // last group is not complete so its not included
        columnGroups.every { cg -> cg instanceof ColumnGroup }
        columnGroups.every { cg -> cg.columns.size() == 3 }
        columnGroups.every { cg -> cg.columns[0].header == "name" && cg.columns[1].header == "age" && cg.columns[2].header == "profession" }
        columnGroups.columns[0][0].value == "John Doe"
        columnGroups.columns[0][1].value == "33"
        columnGroups.columns[0][2].value == "Artist"
    }

    def "Parsing data from a columnGroup generated by using repeating chunks of columns"() {
        setup:
        def personDescriptor = eb.entity(ofKind: "person") {
            parseProperty "name", from: 1 // in the context of columnGroups these indices are relative to the start of a columnGroup: 1 refers to the first column of each columnGroup
            parseProperty "age", from: 2
            parseProperty "profession", from: 3
        }
        def cgDescriptor = new ColumnGroupDescriptor(start: 1, size: 3, repetitions: 3)

        when:
        dr5.createColumnGroups(cgDescriptor).each { cg -> cg.parseEntity(personDescriptor) }
        def entities = pf.getParsedEntities("person")

        then:
        entities.size() == 2 // last group is not complete so its not included in the list of columnGroups
        entities[0].name == "John Doe"
        entities[1].name == "Jane Doe"
        entities[0].age == "33"
        entities[1].age == "22"
        entities[0].profession == "Artist"
        entities[1].profession == "Software Developer"
    }

    def "Generating a columnGroup by using a header pattern"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(headerPattern: ["name", "age", "profession"])

        when:
        def columnGroups = dr5.createColumnGroups(cgDescriptor)

        then:
        columnGroups != []
        columnGroups.size() == 2 // last group is not complete so its not included
        columnGroups.every { cg -> cg instanceof ColumnGroup }
        columnGroups.every { cg -> cg.columns.size() == 3 }
        columnGroups.every { cg -> cg.columns[0].header == "name" && cg.columns[1].header == "age" && cg.columns[2].header == "profession" }
        columnGroups.columns[0][0].value == "John Doe"
        columnGroups.columns[0][1].value == "33"
        columnGroups.columns[0][2].value == "Artist"
    }

    def "Parsing data from a columnGroup generated by using a header pattern"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(headerPattern: ["name", "age", "profession"])

        def personDescriptor = eb.entity(ofKind: "person") {
            parseProperty "name", from: 1 // in the context of columnGroups these indices are relative to the start of a columnGroup: 1 refers to the first column of each columnGroup
            parseProperty "age", from: 2
            parseProperty "profession", from: 3
        }

        when:
        dr5.createColumnGroups(cgDescriptor).each { cg -> cg.parseEntity(personDescriptor) }
        def entities = pf.getParsedEntities("person")

        then:
        entities.size() == 2 // last group is not complete so its not included in the list of columnGroups
        entities[0].name == "John Doe"
        entities[1].name == "Jane Doe"
        entities[0].age == "33"
        entities[1].age == "22"
        entities[0].profession == "Artist"
        entities[1].profession == "Software Developer"
    }


    def "Generating a columnGroup by using regular expressions"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(regexPatterns: [/treatment\d+\.substance/, /treatment\d+\.value/])

        when:
        def columnGroups = dr6.createColumnGroups(cgDescriptor)

        then:
        columnGroups != []
        columnGroups.size() == 3
        columnGroups.columns[0][0].value == "coffee"
        columnGroups.columns[0][1].value == "12 cups"
        columnGroups.columns[1][0].value == "cookies"
        columnGroups.columns[1][1].value == "24 cups"
        columnGroups.columns[2][0].value == "tea"
        columnGroups.columns[2][1].value == "dozens"
        /*
        This strange grouping is the result of not having defined a groupBy element. The default algorithm uses transpose() to combine the lists of matched column values
        Because of some missing elements the example data given leads to the following lists of matched column values: [coffee, cookies, tea] and [12 cups, 24 cups, dozens] which gives the transposed pairs:
        [[coffee, 12 cups], [cookies, 24 cups], [tea, dozens]]
        This of course is not the intended grouping of the example data. Cf. the next test to see how to use groupBy to extract the right groups
        WARNING: always use groupBy if your data is not completely regular
         */
    }

    def "Generating a columnGroup by using regular expressions with groupBy"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(regexPatterns: [/treatment\d+\.substance/, /treatment\d+\.value/], groupBy: "header@treatment")

        when:
        def columnGroups = dr6.createColumnGroups(cgDescriptor)

        then:
        columnGroups != []
        columnGroups.size() == 4
        columnGroups.columns[0][0].value == "coffee"
        columnGroups.columns[0][1].value == "12 cups"
        columnGroups.columns[1][0].value == "cookies"
        !columnGroups.columns[1][1] // no value for treatment2
        columnGroups.columns[2][0].value == "tea"
        columnGroups.columns[2][1].value == "24 cups"
        columnGroups.columns[3][0].value == "dozens"
        !columnGroups.columns[3][1] // no substance for treatment4
    }

    def "Generating a columnGroup by using named regular expressions with groupBy"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(regexPatterns: [substance:/treatment\d+\.substance/, value:/treatment\d+\.value/], groupBy: "header@treatment")

        when:
        def columnGroups = dr6.createColumnGroups(cgDescriptor)

        then:
        columnGroups != []
        columnGroups.size() == 4
        columnGroups.columns[0].find { c -> c.name == "substance" }.value == "coffee" // columns within a columnGroup are named according to the keys of the regexPatterns map
        columnGroups.columns[0].find { c -> c.name == "value" }.value == "12 cups"
        columnGroups.columns[1].find { c -> c.name == "substance" }.value == "cookies"
        !columnGroups.columns[1].find { c -> c.name == "value" } // no value for treatment2
        columnGroups.columns[2].find { c -> c.name == "substance" }.value == "tea"
        columnGroups.columns[2].find { c -> c.name == "value" }.value == "24 cups"
        !columnGroups.columns[3].find { c -> c.name == "substance" } // no substance for treatment4
        columnGroups.columns[3].find { c -> c.name == "value" }.value == "dozens"
    }

    def "Parsing data from a columnGroup generated by using named regular expressions with groupBy"() {
        setup:
        def cgDescriptor = new ColumnGroupDescriptor(regexPatterns: [substance:/treatment\d+\.substance/, value:/treatment\d+\.value/], groupBy: "header@treatment")

        def treatmentDescriptor = eb.entity(ofKind: "treatment") {
            parseProperty "id", from: "header@treatment", of: 1
            parseProperty "substance", from: "substance" // names of columns in columnGroups can be used to refer to source columns for the parsing of properties
            parseProperty "value", from: "value"
        }

        when:
        dr6.createColumnGroups(cgDescriptor).each { cg -> cg.parseEntity(treatmentDescriptor) }
        def entities = pf.getParsedEntities("treatment")

        then:
        entities.size() == 4
        entities[0].id == "treatment1"
        entities[0].substance == "coffee"
        entities[0].value == "12 cups"
        entities[1].id == "treatment2"
        entities[1].substance == "cookies"
        !entities[1].value
        entities[2].id == "treatment3"
        entities[2].substance == "tea"
        entities[2].value == "24 cups"
        entities[3].id == "treatment4"
        !entities[3].substance
        entities[3].value == "dozens"
    }
}
