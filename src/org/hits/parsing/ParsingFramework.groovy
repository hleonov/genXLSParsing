package org.hits.parsing

import groovy.json.JsonSlurper

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson
import static groovy.json.JsonOutput.toJson
import static groovy.json.JsonOutput.toJson

/**
 * Created by bittkomk on 18/08/14.
 */

class ParsingFramework {

    def workbook
    def sheet
    def sheetName
    def parsedEntities = []
    def entityDescriptors = []

    def loadWorkbook(filename) {
        workbook = ExcelHelper.loadWorkbook(filename)
        this
    }

    def useSheet(sheetName) {
        this.sheetName = sheetName
        sheet = ExcelHelper.getSheet(sheetName, workbook)
        this
    }

    // creates a transposed copy of the sheet in use
    def transpose() {
        //println "transpose data of $sheetName"

        def transposedSheetName = "transposed_$sheetName"
        def transposedSheet = workbook.createSheet(transposedSheetName)

        // do the funky chicken dance
        def data = []

        sheet.each { row ->
            row.each { cell ->
                data << [row: cell.getRowIndex(), col: cell.getColumnIndex(), cell: cell]
            }
        }

        def transposedData = data.collect { [row: it.col, col: it.row, cell: it.cell] }

        transposedData.each { td ->
            def row = transposedSheet.getRow(td.row) ?: transposedSheet.createRow(td.row)
            def transposedCell = row.getCell(td.col) ?: row.createCell(td.col)
            ExcelHelper.copyCell(td.cell, transposedCell)
        }

        //ExcelHelper.saveWorkbook(workbook, "testTranspose.xlsx")

        sheetName = transposedSheetName
        sheet = transposedSheet

        this

    }

    def checkParsedEntities() {
        parsedEntities.removeAll { entity ->
            def remove = !checkParsedEntity(entity)
            /*if(remove){
                println "WARNING: entity rejected due to missing necessary properties."
                println entity
            }*/
            remove
        }
    }

    def checkParsedEntity(entity) {
        if (!entity.necessaryProperties) {
            true
        } else {
            entity.necessaryProperties.collect { k -> entity[k] }.inject(true) { acc, val -> acc && val }
        }
    }

    def addParsedEntity(entity) {
        parsedEntities << entity
    }

    def getParsedEntities(kind, row = null) {
        if (!row) {
            parsedEntities.findAll { e -> e.kind == kind }
        } else {
            parsedEntities.findAll { e -> e.kind == kind && e.fromRow == row }
        }
    }

    def addEntityDescriptor(entityDescriptor) {
        entityDescriptors << entityDescriptor
    }

    def getEntityDescriptors() {
        entityDescriptors.unique()
        // maybe some merging will be necessary here for different entityDescriptors for one kind of entity
    }

    def dataRows(headerFormat = null) {
        // trenne header von daten
        // verarbeite header information
        // lese daten und annotiere sie mit header informationen

        //println headerFormat

        def dataRows = []

        def firstHeaderRow = headerFormat?.headerRows?.min { it?.row }?.row ?: 1
        def lastHeaderRow = headerFormat?.headerRows?.max { it?.row }?.row ?: 1
        def firstDataRow = lastHeaderRow + 1

        //println firstHeaderRow
        //println lastHeaderRow

        def header = [:].withDefault { [] }

        (firstHeaderRow..lastHeaderRow).each { ri ->
            def row = sheet.getRow(ri - 1)
            if (row) {
                row.each { cell ->
                    def ci = cell.getColumnIndex() + 1
                    header[ci] << [rowIndex: ri, colIndex: ci, value: ExcelHelper.readCell(cell),
                                   descriptor: (headerFormat?.headerRows?.find {
                                       ri == it?.row
                                   }?.descriptor ?: "header")]
                }
            }
        }

        //println "headers parsed: "
        //header.values().each { println it }

        sheet.each { row ->

            def ri = row.getRowNum() + 1

            if (ri >= firstDataRow) {

                def dataRow = new DataRow(parsingFramework: this, rowNumber: ri, header: header.values())

                row.each { cell ->
                    def ci = cell.getColumnIndex() + 1
                    dataRow.columns << new Column(rowIndex: ri, colIndex: ci, header: header[ci].collect {
                        it.value
                    }.join("."), fullHeader: header[ci], value: ExcelHelper.readCell(cell))
                }

                dataRows << dataRow
            }
        }

        dataRows

    }

    def askForLinkID(LinkingDescriptor ld, rowNumber, sourceEntity) {
        if (ld.entityID) { // find entity of the given kind with the given id
            parsedEntities.find { e -> e.kind == ld.entityKind && e."$ld.linkToProperty" == ld.entityID }?."${ld.linkToProperty}"
        }
        else {
            if (!ld.where) { // find entity of ld.entityKind in same row
                parsedEntities.find { e -> e.kind == ld.entityKind && e.fromRow == rowNumber }?."${ld.linkToProperty}"
            } else { // usere ld.where closure to detected matching entity of kind ld.entityKind
                //println "§§§§§ DEBUG: using ld.where to find links from $sourceEntity to targetEntity of kind ${ld.entityKind}"
                parsedEntities.find { targetEntity -> targetEntity.kind == ld.entityKind && ld.where(sourceEntity, targetEntity) }?."${ld.linkToProperty}"
            }
        }
    }

    def toJSONString(fullOutput = true, outputLinksAsProperties = false) {
        if (fullOutput) {
            prettyPrint(toJson(parsedEntities.groupBy { it.kind }))
            // this snippet from http://zefifier.wordpress.com/2012/10/04/recent-fav-way-to-pretty-print-a-graph-of-maps/
        } else {
            def entitiesForOutput = new JsonSlurper().parseText(toJson(parsedEntities)) // JSON based deep copy

            entitiesForOutput*.remove("fromRow")
            entitiesForOutput*.remove("provenanceInformation")
            entitiesForOutput*.remove("necessaryProperties")

            if (outputLinksAsProperties) {
                entitiesForOutput.each { efo ->
                    if (efo.linksTo) {
                        efo.linksTo.eachWithIndex { link, li ->
                            def name = link.name ?: "link${li + 1}"
                            efo."$name" = link.kind + ":" + link.id
                        }
                        efo.remove("linksTo")
                    }
                }
            }

            prettyPrint(toJson(entitiesForOutput.groupBy { it.kind }))
        }
    }

    def createAndAddHeaderToXLS(wb, ExportFormat exportFormat) {
        exportFormat.headerFormat.each { shName, hf ->
            def sheet = wb.getSheet(shName) ?: wb.createSheet(shName)
            hf.headerRows.eachWithIndex { hr, ri ->
                def row = sheet.getRow(ri) ?: sheet.createRow(ri)
                hr.headerColumns.eachWithIndex { hc, ci ->
                    //write header rows to sheet
                    def cell = row.getCell(ci) ?: row.createCell(ci)
                    cell.setCellValue(hc.label)
                }
            }
        }

        wb
    }

    def filterEntitiesForXLSExport(entities, exportUniqueEntities) {

        def returnEntities = entities

        if (exportUniqueEntities) {

            def entityKind = entities.first().kind

            if (exportUniqueEntities instanceof Boolean) {
                //println "§§§§§ filter entities of $entityKind for uniqueness of $idProperty"
                returnEntities = entities.unique(false) { it?.id } // do not mutate original entity list
            } else if (exportUniqueEntities instanceof List && exportUniqueEntities.any {
                it.split(":").first() == entityKind
            }) {
                def exportUniqueEntity = exportUniqueEntities.find { it.split(":").first() == entityKind }
                def idProperty = "id"
                if (exportUniqueEntity.contains(":")) {
                    idProperty = exportUniqueEntity.split(":")[1]
                }
                //println "§§§§§ filter entities of $entityKind for uniqueness of $idProperty"
                returnEntities = entities.unique(false) { it?."$idProperty" } // do not mutate original entity list
            }
        }

        returnEntities
    }

    def formatLinkForXLSExport(entity) {
        if (entity.linksTo) {
            entity."linksTo".collect { lt -> lt.kind + ":" + lt.id }.join(";")
        } else {
            "Link target not defined"
        }
    }

    def addProvenanceInformationToCell(provInfo, cell, width=20, height=20) {
        if (provInfo) {
            def commentString = provInfo.collect { pi -> "Produced from ${pi.sources.join(', ')} by ${pi.actions.join(', ')} transformations" }.join(";\n")
            //println "§§§§ commentString --> $commentString"
            ExcelHelper.addCommentToCell(cell, commentString, width, height)
        }
    }

    def toXLS(filename, ExportFormat exportFormat, includeProvenanceInformation = false, exportUniqueEntities = false) {

        def wb = ExcelHelper.createWorkbook()
        wb = createAndAddHeaderToXLS(wb, exportFormat)

        def columnIndexLookup = exportFormat.createColumnIndexLookup()
        println "columnIndexLookup $columnIndexLookup"

        exportFormat.entityToSpreadsheetMappings.each { entityKind, entityMapping ->
            // get sheet in wb by entityMapping.sheetName
            def sheetName = entityMapping.sheetName
            def sheet = wb.getSheet(sheetName) ?: wb.createSheet(sheetName)
            // get first number of header rows for this sheet --> dataRowOffset
            def dataRowOffset = exportFormat.headerFormat[sheetName].headerRows.size()

            def entities = filterEntitiesForXLSExport(getParsedEntities(entityKind), exportUniqueEntities)

            entities.eachWithIndex { entity, index ->
                // get row dataRowOffset + index for sheet or create if necessary
                def ri = dataRowOffset + index
                def row = sheet.getRow(ri) ?: sheet.createRow(ri)
                entityMapping.propertyToColumnMappings.each { propertyMapping ->
                    //println "writing property ${propertyMapping.property} with value ${entity[propertyMapping.property]} to row ${dataRowOffset + index} / column with header ${propertyMapping.columnHeader} @ column with index ${columnIndexLookup[sheetName][propertyMapping.columnHeader]}"
                    def ci = columnIndexLookup[sheetName][propertyMapping.columnHeader]
                    if(ci == null) { println "impossible null for ci with columnIndexLookup[$sheetName][${propertyMapping.columnHeader}]"}
                    def value = null
                    // special case: linksTo
                    if (propertyMapping.property == "linksTo") {
                        value = formatLinkForXLSExport(entity)
                    } else {
                        value = entity[propertyMapping.property]
                    }

                    if (value) {
                        def cell = row.getCell(ci) ?: row.createCell(ci)
                        cell.setCellValue(value)

                        if (includeProvenanceInformation) {
                            addProvenanceInformationToCell(entity.provenanceInformation.findAll { it.about == propertyMapping.property }, cell)
                        }
                    }
                }
            }
        }

        // save wb to file
        ExcelHelper.saveWorkbook(wb, filename)
    }

    def getProvenanceInformation() {
        []
    }

    def parse(Closure c, HeaderFormat hf) { // DSL helper method
        dataRows(hf).each { dr ->
            c.delegate = dr
            c()
            dr.postprocess()
            this.checkParsedEntities()
        }
        this
    }


}


