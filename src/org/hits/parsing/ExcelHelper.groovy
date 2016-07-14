package org.hits.parsing

import groovy.util.*

import java.text.SimpleDateFormat

import org.apache.poi.xssf.usermodel.*
import org.apache.poi.ss.usermodel.*

/**
 * Created by bittkomk on 18/08/14.
 */
class ExcelHelper {

    static final df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a") // 1/29/2009  1:00:00 PM
    static cellStylesMap = [:]

    static loadWorkbook(String filename){
        cellStylesMap = [:]
        WorkbookFactory.create(new File(filename))
    }

    static getSheet(sheetName, wb){
        wb.getSheet(sheetName)
    }

    static createWorkbook(){
        cellStylesMap = [:]
        new XSSFWorkbook()
    }

    static saveWorkbook(workbook, String filename){
        try {

            def fileOut = new FileOutputStream(filename)
            workbook.write(fileOut)
            fileOut.close()

        } catch (FileNotFoundException e) {
            e.printStackTrace()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    static readCell(cell) {

        if(!cell) { return null }

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN: return cell.getBooleanCellValue()
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return df.format(cell.getDateCellValue())
                } else {
                    cell.setCellType(Cell.CELL_TYPE_STRING)
                    String numberString = cell.getStringCellValue()
                    //println "@@@@ get numeric cell as string, numberString: $numberString"


                    if(numberString.contains(".") || numberString.contains(",")){
                        def value = new Float(numberString).toString()
                        //println "@@@@ return numeric cell as string, value: $value"
                        cell.setCellValue(value)
                        return value
                    }
                    else{
                        def value = new Integer(numberString).toString()
                        //println "@@@@ return numeric cell as string, value: $value"
                        cell.setCellValue(value)
                        return value
                    }
                }
            case Cell.CELL_TYPE_STRING: return cell.getStringCellValue() //.getString()
            case Cell.CELL_TYPE_FORMULA:
                try{
                    def evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator()
                    return evaluator.evaluate(cell).getStringValue()
                }catch(exception){
                    //println "exception $exception"
                    return cell.getCellFormula()
                }
            default: return null
        }
    }

    static copyCell(from, to, transposeReferences=false) {

        def toCellStyle = cellStylesMap[from.getCellStyle().hashCode()]

        if(!toCellStyle){
            toCellStyle = to.getSheet().getWorkbook().createCellStyle()
            toCellStyle.cloneStyleFrom(from.getCellStyle())
            cellStylesMap[from.getCellStyle().hashCode()] = toCellStyle
        }

        to.setCellStyle(toCellStyle)
        to.setCellType(from.getCellType())

        switch (from.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN: to.setCellValue(from.getBooleanCellValue()); break
            case Cell.CELL_TYPE_NUMERIC: to.setCellValue(from.getNumericCellValue()); break
            case Cell.CELL_TYPE_STRING: to.setCellValue(from.getRichStringCellValue()); break
            case Cell.CELL_TYPE_FORMULA: to.setCellFormula(from.getCellFormula()); break //println from.getCellFormula(); break
            case Cell.CELL_TYPE_BLANK: to.setCellValue(from.getStringCellValue()); break
            default: break //println "cell with cellType " + from.getCellType() + " was not parsed..."
        }

        if(from.getHyperlink()){
            def link = to.getSheet().getWorkbook().getCreationHelper().createHyperlink(from.getHyperlink().getType())
            link.setAddress(from.getHyperlink().getAddress())
            to.setHyperlink(link)
        }


        //def evaluator = to.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator()
        //evaluator.evaluateInCell(to)
    }

    static addCommentToCell(cell, commentString, width=1, height=3){
        def wb = cell.getSheet().getWorkbook()
        def factory = wb.getCreationHelper()

        def drawing = cell.getSheet().createDrawingPatriarch()

        def anchor = factory.createClientAnchor()
        anchor.setCol1(cell.getColumnIndex())
        anchor.setCol2(cell.getColumnIndex() + width)
        anchor.setRow1(cell.getRowIndex())
        anchor.setRow2(cell.getRowIndex() + height)

        def comment = drawing.createCellComment(anchor)
        def str = factory.createRichTextString(commentString)
        comment.setString(str)
        comment.setAuthor("genXlsParsingWithProvenance")

        cell.setCellComment(comment)

    }

}
