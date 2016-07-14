package org.hits.parsing;

/**
 * Created by bittkomk on 18/08/14.
 */

class HeaderColumn {
    def label
    def column

    String toString() {
        "HeaderColumn for column $column with label $label"
    }
}

class HeaderRow {
    def row
    def descriptor
    List<HeaderColumn> headerColumns = []

    String toString() {
        "HeaderRow for row $row with descriptor $descriptor" +
                (headerColumns ? " with columns: " + headerColumns.collect { it.toString() }.join(",") : "")
    }
}

class HeaderFormat {
    def sheetName
    def numRows
    List<HeaderRow> headerRows = []

    String toString() {
        "HeaderFormat for sheet $sheetName" +
                (headerRows ? " with rows: " + headerRows.collect { it.toString() }.join("\n") : "")
    }
}

