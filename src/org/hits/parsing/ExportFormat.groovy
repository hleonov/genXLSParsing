package org.hits.parsing;

class PropertyToColumnMapping {
    def property
    def columnHeader
    def columnIndex
}

class EntityToSpreadsheetMapping {
    def sheetName
    def entityKind
    List<PropertyToColumnMapping> propertyToColumnMappings = []
}

class ExportFormat {
    Map<String, HeaderFormat> headerFormat = [:] // format of headers of each sheet defined
    Map<String, List<EntityToSpreadsheetMapping>> entityToSpreadsheetMappings = [:] // entityKind is key

    def createColumnIndexLookup() {
        Map<String, Map<String, Integer>> columnIndexLookup = [:].withDefault { [:] }

        headerFormat.each { shName, hf ->
            hf.headerRows.each { hr ->
                hr.headerColumns.eachWithIndex { hc, ci ->
                    //create header label to column index information
                    columnIndexLookup."$shName"."${hc.label}" = ci
                }
            }
        }

        columnIndexLookup
    }
}