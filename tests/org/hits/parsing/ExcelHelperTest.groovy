package org.hits.parsing

import org.apache.poi.ss.usermodel.Cell
import spock.lang.Shared

import java.text.SimpleDateFormat

/**
 * Created by bittkomk on 18/08/14.
 */
class ExcelHelperTest extends spock.lang.Specification {

    def sheetName = "testSheet"
    @Shared def wb = ExcelHelper.createWorkbook()
    @Shared def sheet = wb.createSheet("testSheet")
    @Shared def row = sheet.createRow(1)
    @Shared def booleanCell = row.createCell(1, Cell.CELL_TYPE_BOOLEAN)
    @Shared def dateCell = row.createCell(2, Cell.CELL_TYPE_NUMERIC)
    @Shared def integerCell = row.createCell(3, Cell.CELL_TYPE_NUMERIC)
    @Shared def floatingPointCell = row.createCell(4, Cell.CELL_TYPE_NUMERIC)
    @Shared def df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a")

    def "getting a sheet by its name"() {
        expect: ExcelHelper.getSheet(sheetName, wb) == sheet
    }

    def "reading cells from sheet"(Cell cell, value) {
        setup:
        booleanCell.setCellValue(false)
        dateCell.setCellValue(df.format(new Date()))
        integerCell.setCellValue("42")
        floatingPointCell.setCellValue("42.24")

        expect:
        ExcelHelper.readCell(cell) == value
        ExcelHelper.readCell(cell).class == value.class

        where:
        cell                |   value
        booleanCell         |   false
        dateCell            |   df.format(new Date())
        integerCell         |   "42"
        floatingPointCell   |   "42.24"
    }
}
