/*def niceBuilder = new XLSParsingBuilder("test.xlsx", headerRows: 1) { // definition of header format?
	useSheet("biosamples")
		.forEachDataRow
			.parseSpecimen(
				id from columns "A", "B" using { it.join("-") } is necessary
				name from column "B"
				belongsTo Strain given in column "C"
			)
			.parseSample(
				id from column "D" is necessary
				organismPart from column "E"
				belongsTo Specimen of same row 
			)
			.createColumnGroups(regex or range or group breaks or full list of lists)
				.forEachColumnGroup(range needs fixed size)
					.parseTreatment( // column for numbers are relative 1 based indices for each group
						treatmentType from header of column 1
						value from column 1 using { it.split(".")[0] }
						unit from column 1 using { it.split(".")[1] }
						substance from optional column 2 
						belongsTo Sample of the same row
					)
				// OR
				.useColumnGroup1()
					.parseTreatment(
						some format description
					 )
				.useColumnGroup2()
					.parseTreatment(
						another format description
					)
}

println niceBuilder.toJSON()
println niceBuilder.getProvenanceInformation()

def anotherNiceBuilder = new XLSExportFormatBuilder( some format definition goes here )

niceBuilder.exportToXLS(anotherNiceBuilder)*/

/*class Parser {

	def getCellValues(where) {
		println "get cell values from $where"
		// mockup
		if(where instanceof List){
			return ["Hello", "World", "List"]
		}
		else{
			return "Hello World String"
		}
	}

	def parseProperty(property){
		[from: { headerOrCol, where ->
			println "from $headerOrCol"		
			println "from $where"
			def cellValues = getCellValues(where)
			[using: { transformation ->
				//println "using" 
				//def t = transformation.rehydrate(this, this, this)
				println transformation(cellValues)
			}]
			context.currentEntity."$property" = cellValues
			println context
		}]

	//	entity
		//newEntity = parseEntityFromColumns(property, sentence, newEntity)
	}

	def context = [:]


	def parseSample(propertiesToParse){
		def newEntity = [kind: "Sample"]
		context.currentEntity = newEntity
		
		propertiesToParse.each { ptp ->
						
			//def code = ptp.rehydrate(this, this, this)
			def code = ptp.clone()
			code.setDelegate(this)
			//code.setResolveStrategy(Closure.DELEGATE_FIRST)
			code()
			println "finished this one!"
		}
	}
}

def simpleJoin = { a -> println "simpleJoin is exexcuted"; a.join("-")}

new Parser().parseSample(
[
 { parseProperty "greeting" from "columns", ["A", "B"] using simpleJoin },
 { parseProperty "gniteerg" from "columns", ["A", "B"] using { a -> a.join("-").reverse()} },
 { parseProperty "greetingStr" from "header", "A" using { a -> a.toUpperCase()} }
])*/

package hits.org.parsing

@Grab("org.apache.poi#poi;3.10-FINAL")
@Grab("org.apache.poi#poi-ooxml;3.10-FINAL")

import java.text.SimpleDateFormat

import groovy.util.*

import static groovy.json.JsonOutput.*

import org.apache.poi.xssf.usermodel.*
import org.apache.poi.ss.usermodel.*

println "HEREREERERER!"

class ExcelHelper {
	static loadWorkbook(filename){
		return "workbook"
	}

	static createWorkbook(){
		new XSSFWorkbook()
	}

	static saveWorkbook(workbook, filename){
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

	static final df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a") // 1/29/2009  1:00:00 PM	

	static readCell(cell) { 

		if(!cell) { return null }

		switch (cell.getCellType()) {
          	case Cell.CELL_TYPE_BOOLEAN: return cell.getBooleanCellValue()
            case Cell.CELL_TYPE_NUMERIC: 
            	if (DateUtil.isCellDateFormatted(cell)) {
                	return df.format(cell.getDateCellValue())
                } else {
                	cell.setCellType(Cell.CELL_TYPE_STRING)
                	def numberString = cell.getRichStringCellValue().getString()    
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
            case Cell.CELL_TYPE_STRING: return cell.getRichStringCellValue().getString()
            default: return null
        }
	}
}


class ParsingFramework {
	def workbook
	def sheet
	def parsedEntities = []

	def loadWorkbook(filename){
		workbook = ExcelHelper.loadWorkbook(filename)
		this
	}

	def useSheet(sheet){
		this.sheet = sheet //workbook.getSheetByName(sheet)
		this 
	}

	def addParsedEntity(entity) {
		parsedEntities << entity
	}

	def getParsedEntities(kind, row=null){
		if(!row){
			parsedEntities.findAll { e -> e.kind == kind }	
		}
		else{
			parsedEntities.findAll { e -> e.kind == kind && e.fromRow == row }
		}
		
	}
	
	def dataRows(){
		// trenne header von daten
		// verarbeite header information
		// lese daten und annotiere sie mit header informationen
		def ri = 2
		def dr1 = new DataRow(parsingFramework: this, rowNumber: ri)
		
		dr1.columns = [new Column(rowIndex: ri, colIndex: 1, header: "id", value: "156"),
					   new Column(rowIndex: ri, colIndex: 2, header: "title", value: "first one"),
					   new Column(rowIndex: ri, colIndex: 3, header: "additional title", value: "add me"),
					   new Column(rowIndex: ri, colIndex: 4, header: "sfx", value: "enabled"),
					   new Column(rowIndex: ri, colIndex: 5, header: "another id", value: "651"),
					   new Column(rowIndex: ri, colIndex: 6, header: "funkadelic", value: "yes"),
					   new Column(rowIndex: ri, colIndex: 7, header: "complex prop", value: "10 km/h"),
					   new Column(rowIndex: ri, colIndex: 8, header: "substance", value: "glucose"),
					   new Column(rowIndex: ri, colIndex: 9, header: "value", value: "0.5"),
					   new Column(rowIndex: ri, colIndex: 10, header: "störenfried", value: "und was für einer"),
					   new Column(rowIndex: ri, colIndex: 11, header: "treatment1.value", value:"42"),
					   new Column(rowIndex: ri, colIndex: 12, header: "treatment1.unit", value:"adams"),
					   new Column(rowIndex: ri, colIndex: 13, header: "substance", value: "fructose"),
					   new Column(rowIndex: ri, colIndex: 14, header: "value", value: "1.0"),
					   new Column(rowIndex: ri, colIndex: 15, header: "me", value: "a song"),
					   new Column(rowIndex: ri, colIndex: 16, header: "you", value: "a flute"),
					   new Column(rowIndex: ri, colIndex: 17, header: "mee", value: "a song song"),
					   new Column(rowIndex: ri, colIndex: 18, header: "youhu", value: "a flute flute"),
					   new Column(rowIndex: ri, colIndex: 19, header: "treatment2.value", value:"24"),
					   new Column(rowIndex: ri, colIndex: 20, header: "treatment2.unit", value:"douglas")
					  ]

		ri = 3
		def dr2 = new DataRow(parsingFramework: this, rowNumber: ri)
		
		dr2.columns = [new Column(rowIndex: ri, colIndex: 1, header: "id", value: "789"),
					   new Column(rowIndex: ri, colIndex: 2, header: "title", value: "second one"),
					   new Column(rowIndex: ri, colIndex: 3, header: "additional title", value: "add me"),
					   new Column(rowIndex: ri, colIndex: 4, header: "sfx", value: "whatever"),
					   new Column(rowIndex: ri, colIndex: 5, header: "another id", value: "987"),
					   new Column(rowIndex: ri, colIndex: 6, header: "funkadelic", value: "nepp"),
					   new Column(rowIndex: ri, colIndex: 7, header: "complex prop", value: "15000 m/h"),
					   new Column(rowIndex: ri, colIndex: 8, header: "substance", value: "glucose"),
					   new Column(rowIndex: ri, colIndex: 9, header: "value", value: "50"),
					   new Column(rowIndex: ri, colIndex: 10, header: "störenfried", value: "und was für einer"),
					   new Column(rowIndex: ri, colIndex: 11, header: "treatment1.value", value:"4200"),
					   new Column(rowIndex: ri, colIndex: 12, header: "treatment1.unit", value:"adams"),
					   new Column(rowIndex: ri, colIndex: 13, header: "substance", value: "fructose"),
					   new Column(rowIndex: ri, colIndex: 14, header: "value", value: "100.0"),
					   new Column(rowIndex: ri, colIndex: 15, header: "me", value: "a boomerang"),
					   new Column(rowIndex: ri, colIndex: 16, header: "you", value: "a cangoroo"),
					   new Column(rowIndex: ri, colIndex: 17, header: "mee", value: "a rangaboom"),
					   new Column(rowIndex: ri, colIndex: 18, header: "youhu", value: "a rooocang"),
					   new Column(rowIndex: ri, colIndex: 19, header: "treatment2.value", value:"2400"),
					   new Column(rowIndex: ri, colIndex: 20, header: "treatment2.unit", value:"douglas")
					  ]

		return [dr1, dr2]
	}

	def askForLinkID(LinkingDescriptor ld, rowNumber){
		parsedEntities.find { e -> e.kind == ld.entityKind && e.fromRow == rowNumber }?.id
	}

	def toJSON(){
		prettyPrint(toJson(parsedEntities.groupBy { it.kind }))  // this snippet from http://zefifier.wordpress.com/2012/10/04/recent-fav-way-to-pretty-print-a-graph-of-maps/
	}

	def toXLS(filename, ExportFormat exportFormat, includeProvenanceInformation=false){
		
		def wb = ExcelHelper.createWorkbook()
		Map<String, Map<String, Integer>> columnIndexLookup = [:].withDefault{ [:] }

		exportFormat.headerFormat.each { shName, hf ->			
			def sheet = wb.getSheet(shName) ?: wb.createSheet(shName)						

			hf.headerRows.eachWithIndex { hr, ri -> 
				def row = sheet.getRow(ri) ?: sheet.createRow(ri)
				hr.headerColumns.eachWithIndex { hc, ci -> 
					//create header label to column index information
					columnIndexLookup."$shName"."${hc.label}" = ci 
					//write header rows to sheet
					def cell = row.getCell(ci) ?: row.createCell(ci)
					cell.setCellValue(hc.label)
				}
			}
		}

		//println columnIndexLookup
		
		exportFormat.entityToSpreadsheetMappings.each { entityKind, entityMapping ->
			// get sheet in wb by entityMapping.sheetName
			def sheetName = entityMapping.sheetName
			def sheet = wb.getSheet(sheetName) ?: wb.createSheet(sheetName)
			// get first number of header rows for this sheet --> dataRowOffset
			def dataRowOffset = exportFormat.headerFormat[sheetName].headerRows.size() 

			//println "writing entities of kind $entityKind to sheet $sheetName"

			getParsedEntities(entityKind).eachWithIndex { entity, index -> 
				// get row dataRowOffset + index for sheet or create if necessary
				def ri = dataRowOffset + index
				def row = sheet.getRow(ri) ?: sheet.createRow(ri)
				entityMapping.propertyToColumnMappings.each { propertyMapping ->					
					//println "writing property ${propertyMapping.property} with value ${entity[propertyMapping.property]} to row ${dataRowOffset + index} / column with header ${propertyMapping.columnHeader} @ column with index ${columnIndexLookup[sheetName][propertyMapping.columnHeader]}"
					def ci = columnIndexLookup[sheetName][propertyMapping.columnHeader]
					def cell = row.getCell(ci) ?: row.createCell(ci)
					// special case: linksTo
					def value = (propertyMapping.property == "linksTo") ? entity."linksTo".kind + ":" + entity."linksTo".id : entity[propertyMapping.property]
					cell.setCellValue(value)

					if(includeProvenanceInformation){
						def provInfo = entity.provenanceInformation.findAll { it.about == propertyMapping.property }
						println "§§§§ provInfo for ${propertyMapping.property} ::: " + provInfo

						if(provInfo){
							def commentString = provInfo.collect { pi -> "Produced from ${pi.sources.join(', ')} by ${pi.actions.join(', ')} transformations" }.join(";\n")
							ExcelHelper.addCommentToCell(cell, commentString, 20, 20)
						}
					}
				}
			}
		}

		// save wb to file
		ExcelHelper.saveWorkbook(wb, filename)
	}

	def getProvenanceInformation(){
		[]
	}
}


class HeaderColumn {
	def label
	def column 
}

class HeaderRow {
	def row 
	def descriptor
	List<HeaderColumn> headerColumns = []
}

class HeaderFormat {
	def sheetName 
	def numRows
	List<HeaderRow> headerRows = []
}

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
}


class Rule {
	def name
	def antecedent
	def consequence
	def alternative
	def consequenceEffects = "global"
	def alternativeEffects = "global"

	String toString() { name }
}

class RuleEngine {

	static applyRule(entity, Rule rule) {
		println "test rule $rule"
		if(rule.antecedent(entity)) {
			println "apply consequence of rule $rule"
			rule.consequence(entity)
			entity.provenanceInformation << new ProvenanceInformation(about: rule.consequenceEffects, actions: [rule.name], agents: ["Parser", "RuleEngine"], sources: ["parsed property"])
		}
		else{
			if(rule.alternative){
				println "apply alternative of rule $rule"
				rule.alternative(entity)
				entity.provenanceInformation << new ProvenanceInformation(about: rule.alternativeEffects, actions: [rule.name], agents: ["Parser", "RuleEngine"], sources: ["parsed property"])
			}
		}
	}	

	static applyRules(entity, List<Rule> rules){
		rules.each { rule -> applyRule(entity, rule)}		
	}

	static applyRules(entity, Rule... rules){		
		applyRules(entity, rules.toList())
	}
}


class Column {
	def rowIndex
	def colIndex
	def colGroupIndex
	def header 
	def value 

	String toString() {
		"column at row $rowIndex / column $colIndex has header $header and value $value"
	}
}

class EntityDescriptor {
	def kind 
	List<PropertyDescriptor> properties = []
	List<LinkingDescriptor> links = []
	List<Rule> rules = []

	String toString() {
		"entity of kind $kind with ${properties.size()} properties and ${links.size()} links"
	}
}

class PropertyDescriptor {
	def name 
	def where // columns / header 
	def from 
	Transformation using 
	def isNecessary = false 	

	String toString() {
		"property $name is located at $where : $from. transformed by $using"
	}
}

class LinkingDescriptor {
	def entityKind
	def entityID 
	def entityInSameRow

	String toString() {
		"link to entity of kind $entityKind ${entityInSameRow ? 'in same row' : ''}"
	}
}

class ColumnGroupDescriptor {
	def regexPatterns
	def start
	def size
	def repetitions 	
	def headerPattern
}

class EntityParser {
	
	def parsingFramework
	List<Column> columns 
	int rowNumber
	def isColumnGroup = false 
	

	def parseEntity() {}
	def parseProperty() {}
	def parseLink() {}

}

class ProvenanceInformation {
	def about 
	def sources	= []
	def agents = ["Parser"]
	def actions = []

	String toString() { "Provenance of $about: derived from $sources using $actions" }
}

class Transformation {
	String name = "Identity"
	Closure listTransformation = { it }
	Closure elementTransformation = { it }
	String onProperty // helping field for Builder

	String toString() { name }
	
}


class DataRow extends EntityParser {

	def identityTransformation = new Transformation()
	def stuffToPostprocess = []
	
	def parseEntity(EntityDescriptor ed) {
		def newEntity = [kind: ed.kind, fromRow: rowNumber, provenanceInformation: []]

		ed.properties.each { pd -> parseProperty(pd, newEntity) }
		ed.links.each { ld -> stuffToPostprocess << [name: "link", entity: newEntity, action: {parseLink(ld, newEntity)}] }
		ed.rules.each { rule -> stuffToPostprocess << [name: rule.name, entity: newEntity, action: {RuleEngine.applyRule(newEntity, rule)}] }
		
		parsingFramework.addParsedEntity(newEntity)
	}

	def postprocess(){
		println "do some post-processing..."
		println "${stuffToPostprocess.size()} things to do!"
		stuffToPostprocess.each { stp -> 
			stp.action() 
			/*if(stp.name != "link"){
				println stp.entity.provenanceInformation.actions
				println stp.name
				stp.entity.provenanceInformation.actions << stp.name
				println stp.entity.provenanceInformation.actions
			}*/
		}
	}

	def parseProperty(PropertyDescriptor pd, entity) {
		// fallunterscheidung für pd.from: 1, [1,2,...], "A", ["A","B",...]
		// handhaben von pd.isNessary
		// wenn using Closure definiert, auf value(s) anwenden
		// zu entity hinzufügen
		
		/*println columns
		println pd.where
		println pd.from*/		

		def provenanceSource = { header -> "${parsingFramework.sheet}:$header@$rowNumber" }

		def where = pd.where.toLowerCase()

		if(where.startsWith("column")){
 			where = "value"
		}
		else{ // parse from header
			where = "header"
		}	

		def value = null
		def transform = pd.using ?: identityTransformation
		def colIndex = isColumnGroup ? "colGroupIndex" : "colIndex"

		def provenance = new ProvenanceInformation(about: pd.name, actions:[transform.toString()])

		if(!(pd.from instanceof List)){
			def col = null

			if(pd.from instanceof Number){
				col = columns.find { it."$colIndex" == pd.from }				
			}
			else{
				col = columns.find { it.header == pd.from }				
			}

			if(col){
				value = transform.elementTransformation(col."$where")
				provenance.sources += provenanceSource(col.header)
			}
		}
		else{ // process list
			//println "process list"
			
			def colIndices = pd.from.findAll { it instanceof Number }
			def headerLabels = pd.from - colIndices			

			def columnsToParse = columns.findAll { it."$colIndex" in colIndices || it.header in headerLabels }
			def parsedValues = columnsToParse.collect { transform.elementTransformation(it."$where") }
			def provenanceSources = columnsToParse.collect { provenanceSource(it.header) }
			
			/*println "§§§§ colIndices: $colIndices"
			println "§§§§ headerLabels: $headerLabels"
			println "§§§§ parsedValues: $parsedValues"*/

			value = transform.listTransformation(parsedValues)
			provenance.sources += provenanceSources
		}

		if(value){
			entity."${pd.name}" = value 
			entity.provenanceInformation << provenance					
		}
		else{
			if(pd.isNecessary){
				println "raise error in parsing necessary property"
			}
		}

		//println entity

	}

	def parseLink(LinkingDescriptor ld, entity) {
		// fallunterscheidung
		// wenn entityID gegeben, verwende diese
		// wenn entityRow gegeben, erfrage entityID
		// (sollte dies eine post-processing aktion sein? weil erst dann alle entitäten geparst wurden
		// trennung von entity-parsing und verlinkung)
		
		println "parse link for $ld"

		def newLink = null

		if(ld.entityID){
			newLink = [kind: ld.entityKind, id: ld.entityID]
			
		}		
		else{
			if(ld.entityInSameRow){
				def id = parsingFramework.askForLinkID(ld, rowNumber)
				if(id){
					newLink = [kind: ld.entityKind, id: id]
				}
				else{
					println "error getting link for $ld in row $rowNumber"
				}
			}
		}


		if(newLink){
			if(entity.linksTo){
				entity.linksTo << newLink
			}
			else{
				entity.linksTo = newLink
			}
		}
	}

	def createColumnGroups(ColumnGroupDescriptor cgd) {
		def columnGroups = []

		if(cgd.headerPattern){
			def patternLength = cgd.headerPattern.size()
			def patternsFound = []
			def columnHeaders = columns.collect { it.header }				
			def columnsByColIndex= columns.groupBy { it.colIndex }		

			//println "§§§§§§ columnsByColIndex " + columnsByColIndex
			//println columnHeaders

			def i = -1
			def offset = 0

			while((i = Collections.indexOfSubList(columnHeaders, cgd.headerPattern)) >= 0){	
						
				def matchedColumns = columnHeaders[i..<(i+patternLength)].collect{ new Column(header: it, colIndex:0, colGroupIndex:0, rowIndex: rowNumber, value: "") }				
				matchedColumns.inject(1) { acc, val -> 
					val.colIndex = offset + i + acc 
					val.colGroupIndex = acc
					val.value = (columnsByColIndex[val.colIndex]?.value instanceof List) ? columnsByColIndex[val.colIndex]?.value?.first() : columnsByColIndex[val.colIndex]?.value
					//println columnsByColIndex[val.colIndex]

					 ++acc
				}
				
				columnHeaders = (columnHeaders.size() > i+patternLength) ? columnHeaders[(i+patternLength)..-1] : []				
				
				offset = offset + i + patternLength
				
				patternsFound << matchedColumns
			}

			patternsFound.each { 
				columnGroups << new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: it, parsingFramework: parsingFramework)
			}

			//columnGroups.each { cg -> println cg.columns } 
			
		}
		else if(cgd.start && cgd.size && cgd.repetitions){
			println "parsing ${cgd.repetitions} columnGroups of size ${cgd.size} starting from ${cgd.start}"

			def sortedColumns = columns.sort { it.colIndex }

			cgd.repetitions.times { ri ->

				def selectedColumns = []

				cgd.size.times { si ->
					def tmpColumn = sortedColumns[cgd.start-1 + ri*cgd.size + si]
					tmpColumn.colGroupIndex = si + 1
					selectedColumns << tmpColumn
				}

				columnGroups << new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: selectedColumns, parsingFramework: parsingFramework)
			}
		}
		else if(cgd.regexPatterns){
			println "create column groups using regexPatterns"


			def allMatchedColumns = [:]

			cgd.regexPatterns.eachWithIndex { pattern, index ->

				def matchedColumns = []

				matchedColumns = columns.findAll { col -> col.header ==~ pattern }

				if(matchedColumns){
					allMatchedColumns[index] = matchedColumns
				}
			}


			allMatchedColumns.values().toList().transpose().each { matchedColumns ->
				matchedColumns.inject(1) { acc, val -> val.colGroupIndex = acc; ++acc }
				columnGroups << new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: matchedColumns, parsingFramework: parsingFramework)
			}


		}

		println columnGroups
		
		columnGroups
	}
}

class ColumnGroup extends DataRow {

	def isColumnGroup = true	
	def dataRowParent

	def parseEntity(EntityDescriptor ed) {
		def newEntity = [kind: ed.kind, fromRow: rowNumber, provenanceInformation: []]

		ed.properties.each { pd -> parseProperty(pd, newEntity) }
		ed.links.each { ld -> dataRowParent.stuffToPostprocess << [name: "link", entity: newEntity, action: {parseLink(ld, newEntity)}] }
		ed.rules.each { rule -> dataRowParent.stuffToPostprocess << [name: rule.name, entity: newEntity, action: {RuleEngine.applyRule(newEntity, rule)}] }
		

		parsingFramework.addParsedEntity(newEntity)
	}

}

// ##################### EXAMPLE #####################
// 
// 


class EntityDescriptorFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		false 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
		new EntityDescriptor(kind: attributes.ofKind)
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}

class PropertyDescriptorFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		true 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {		

		def pd = new PropertyDescriptor(where: "column")

		if(value) { pd.name = value }

		if(attributes.necessary) { pd.isNecessary = true; pd.name = attributes.necessary }

		if(attributes.from && attributes.from instanceof String && (attributes.from.startsWith("column") || attributes.from.startsWith("header"))){
			pd.where = attributes.from 
			pd.from = attributes.of
		}
		else{
			pd.from = attributes.from
		}

		pd

	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object propertyDescriptor) {
		if(parent != null && parent instanceof EntityDescriptor){
			parent.properties << propertyDescriptor
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}

class TransformationFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		false 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {		
		// make identity transformation {it} default if nothing different is specified
		new Transformation(name: value, elementTransformation: attributes.ofElements ?: {it}, listTransformation: attributes.ofList ?: {it}, onProperty: attributes.onProperty)
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object transformation) {
		if(parent != null && parent instanceof EntityDescriptor){
			parent.properties.findAll { prop -> prop.name == transformation.onProperty }.each { prop -> prop.using = transformation }
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}

class LinkingDescriptorFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		true 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {		
		def ld = new LinkingDescriptor(entityKind: value, entityInSameRow: true)
		if(attributes.withID) { ld.entityInSameRow = false; ld.entityID = attributes.withID }
		ld
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object linkingDescriptor) {
		if(parent != null && parent instanceof EntityDescriptor){
			parent.links << linkingDescriptor
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}


class RuleFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		true 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {		
		new Rule(name: value)
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object rule) {
		if(parent != null && parent instanceof EntityDescriptor){
			parent.rules << rule
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		true 
	}

}


class EntityDescriptorFactoryBuilder extends FactoryBuilderSupport {
	public EntityDescriptorFactoryBuilder(boolean init = true){
		super(init)
	}

	def registerObjectFactories() {
		registerFactory("entity", new EntityDescriptorFactory())		
		registerFactory("parseProperty", new PropertyDescriptorFactory())	
		registerFactory("applyTransformation", new TransformationFactory())	
		registerFactory("linkTo", new LinkingDescriptorFactory())
		registerFactory("applyRule", new RuleFactory())

	}
}





def entity1 = new EntityDescriptor(kind: "entity1")
entity1.properties = [
	new PropertyDescriptor(name: "id", where:"column", from: "id", isNecessary: true),
	new PropertyDescriptor(name: "id2", where:"column", from: 1, using: new Transformation(name: "trans1", elementTransformation:{ it += " via colIndex"})),
	new PropertyDescriptor(name: "fulltitle", where:"column", from: ["title", "additional title"], using: new Transformation(name: "trans2", listTransformation:{ it?.join("--") })),
	new PropertyDescriptor(name: "fulltitle2", where:"column", from: [2, 3], using: new Transformation(name: "trans3", elementTransformation: { it?.toUpperCase() }, listTransformation:{ (it?.join("--")) + " via colIndex" })),
	new PropertyDescriptor(name: "fulltitle3", where:"column", from: ["title", 3], using: new Transformation(name: "trans4", listTransformation:{ (it?.join("--")) + " mixed colIndex and header info" })),
	new PropertyDescriptor(name: "fulltitle4", where:"column", from: [3, "title"], using: new Transformation(name: "trans5", listTransformation:{ (it?.join("--")) + " mixed colIndex and header info the other way around" })),
	new PropertyDescriptor(name: "sfxheader", where:"header", from: "sfx")
]

def veloRegex = /(\d*)\s*(.*)/

def entity2 = new EntityDescriptor(kind: "entity2")
entity2.properties = [
	new PropertyDescriptor(name: "id", where: "column", from: 5, isNecessary: true),
	new PropertyDescriptor(name: "funkadelic", where: "column", from: 6),
	new PropertyDescriptor(name: "velocity", where:"column", from: 7, using: new Transformation(name: "trans6", elementTransformation:{ value -> if((m = value =~ veloRegex).matches()) { return m.group(1) }; ""})),
	new PropertyDescriptor(name: "velocity unit", where:"column", from: 7, using: new Transformation(name: "trans7", elementTransformation: { value -> if((m = value =~ veloRegex).matches()) { return m.group(2) }; ""}))
]
entity2.links = [
	new LinkingDescriptor(entityKind: "entity1", entityInSameRow: true)
]

def entity3 = new EntityDescriptor(kind: "treatment")
entity3.properties = [
	new PropertyDescriptor(name: "substance", where: "column", from: 1),
	new PropertyDescriptor(name: "value", where: "column", from: 2)
]
entity3.links = [
	new LinkingDescriptor(entityKind: "entity2", entityInSameRow: true)
]

def entity4 = new EntityDescriptor(kind: "strangestuff")
entity4.properties = [
	new PropertyDescriptor(name: "me(normalized)", where: "column", from: 1),
	new PropertyDescriptor(name: "you(normalized)", where: "column", from: 2)
]

def entity5 = new EntityDescriptor(kind: "strangestuff-me")
entity5.properties = [
	new PropertyDescriptor(name: "me(normalized)", where: "column", from: 1)	
]

def entity6 = new EntityDescriptor(kind: "strangestuff-you")
entity6.properties = [
	new PropertyDescriptor(name: "you(normalized)", where: "column", from: 2)	
]
entity6.links = [
	new LinkingDescriptor(entityKind: "strangestuff-me", entityInSameRow: true) // doesn't work; isn't unique either
]

def entity7 = new EntityDescriptor(kind: "treatment from regex cgs")
entity7.properties = [
	new PropertyDescriptor(name: "value", where: "column", from: 1),
	new PropertyDescriptor(name: "unit", where: "column", from: 2)
]
entity7.rules = [
	new Rule(name: "convert values to Integer", antecedent: { e -> true }, consequence: { e -> e.value = e.value as Integer}, consequenceEffects: "value"),	
	new Rule(name:"double values >= 100", antecedent:{e -> (e.value as Integer) >= 100}, consequence:{e -> e.value *= 2}, consequenceEffects: "value")
]






def exportFormat = new ExportFormat()
exportFormat.headerFormat = [
	"regexTreatments":new HeaderFormat(sheetName:"regexTreatments", headerRows: [
		new HeaderRow(row: 1, headerColumns: [
				new HeaderColumn(column:1, label:"value"),
				new HeaderColumn(column:2, label:"unit")
			])
		]),
	"entity2":new HeaderFormat(sheetName:"entity2", headerRows: [
		new HeaderRow(row: 1, headerColumns: [
				new HeaderColumn(column:1, label:"id"),				
				new HeaderColumn(column:2, label:"velocity"),
				new HeaderColumn(column:3, label:"velocity unit"),
				new HeaderColumn(column:4, label:"very important"), 
				new HeaderColumn(column:5, label:"belongsTo")
			])
		]),
	"entity1":new HeaderFormat(sheetName:"entity1", headerRows: [
		new HeaderRow(row: 1, headerColumns: [
				new HeaderColumn(column:1, label:"id"),
				new HeaderColumn(column:2, label:"full title")
			])
		])
]
exportFormat.entityToSpreadsheetMappings = [
	"treatment from regex cgs": new EntityToSpreadsheetMapping(sheetName: "regexTreatments", propertyToColumnMappings: [
			new PropertyToColumnMapping(property: "value", columnHeader: "value"),
			new PropertyToColumnMapping(property: "unit", columnHeader:"unit")
		]),
	"entity2": new EntityToSpreadsheetMapping(sheetName: "entity2", propertyToColumnMappings: [
			new PropertyToColumnMapping(property: "id", columnHeader: "id"),
			new PropertyToColumnMapping(property: "velocity", columnHeader: "velocity"),
			new PropertyToColumnMapping(property: "velocity unit", columnHeader: "velocity unit"),
			new PropertyToColumnMapping(property: "funkadelic", columnHeader: "very important"),
			new PropertyToColumnMapping(property: "linksTo", columnHeader: "belongsTo")
		]),
	"entity1": new EntityToSpreadsheetMapping(sheetName: "entity1", propertyToColumnMappings: [
			new PropertyToColumnMapping(property: "id", columnHeader: "id"),
			new PropertyToColumnMapping(property: "fulltitle2", columnHeader: "full title")
		])
]

class ExportFormatFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		false 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
		def exportFormat = new ExportFormat()
		exportFormat.headerFormat = [:]
		exportFormat.entityToSpreadsheetMappings = [:]
		exportFormat
	}

}

class HeaderFormatFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		false 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
		new HeaderFormat(sheetName: attributes.forSheet)
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object headerFormat) {
		if(parent != null && parent instanceof ExportFormat){
			parent.headerFormat[headerFormat.sheetName] = headerFormat
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}

class HeaderRowFactory extends AbstractFactory {

	public boolean isLeaf() {
		false 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {		
		new HeaderRow()
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object headerRow) {
		if(parent != null && parent instanceof HeaderFormat){
			headerRow.row = parent.headerRows?.max { it.row }?.row?.plus(1) ?: 1 
			parent.headerRows << headerRow
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}
}

class HeaderColumnFactory extends AbstractFactory {

	public boolean isLeaf() {
		true 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {		
		
		if(value && !attributes){
			 new HeaderColumn(label: value)

		}
		else{
			 new HeaderColumn()			
		}

	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object headerColumn) {
		if(parent != null && parent instanceof HeaderRow){
			headerColumn.column = parent.headerColumns?.max { it.column }?.column?.plus(1) ?: 1 
			parent.headerColumns << headerColumn
		}
	}	
}

class EntityToSpreadsheetMappingFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		false 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
		new EntityToSpreadsheetMapping(sheetName: attributes.toSheet, entityKind: attributes.entity)
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object entityToSpreadsheetMapping) {
		if(parent != null && parent instanceof ExportFormat){
			parent.entityToSpreadsheetMappings[entityToSpreadsheetMapping.entityKind] = entityToSpreadsheetMapping
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}

class PropertyToColumnMappingFactory extends AbstractFactory {
	
	public boolean isLeaf() {
		true 
	}

	public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
		new PropertyToColumnMapping(property: value, columnHeader: attributes.to)
	}

	public void setParent(FactoryBuilderSupport builder, Object parent, Object propertyToColumnMapping) {
		if(parent != null && parent instanceof EntityToSpreadsheetMapping){
			parent.propertyToColumnMappings << propertyToColumnMapping
		}
	}

	public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
		false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
		// sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
	}

}

class ExportFormatFactoryBuilder extends FactoryBuilderSupport {
	public ExportFormatFactoryBuilder(boolean init = true){
		super(init)
	}

	def registerObjectFactories() {
		registerFactory("exportFormat", new ExportFormatFactory())
		registerFactory("headerFormat", new HeaderFormatFactory())
		registerFactory("row", new HeaderRowFactory())
		registerFactory("col", new HeaderColumnFactory())
		registerFactory("map", new EntityToSpreadsheetMappingFactory())
		registerFactory("property", new PropertyToColumnMappingFactory())
	}
}





//@TODO 
//Add Creation of ColumnGroups (DONE)
//Add Parsing of ColumnGroups (DONE)
// Regex Patterns for ColumnGroups: regexPatterns = [/treatment\d.*value/, /treatment\d.*unit/ ... ] (DONE)
// Naming of parts of ColumnGroups (?)
//Add Tracking of Provenance Information (DONE)
//!! Add Input from real XLS data
//Add post-processing step to entity parsing/creation (maybe linking should go here; application of a rule-engine) (DONE)
//!! Add complex header handling (STARTED)
//Add Ouput to JSON for parsed entries (DONE)
//?? Add Output of Provenance Information in PROV-Ontology 
//Add Output to XLS of a specified Format (DONE)
//Add Parsing across sheets (entity.prop1 <-- sheet1:here + sheet2:there)
//Add horizontal parsing
//Think on embedding provenance information into the XLS sheet (tooltips? links?), cf. http://stackoverflow.com/questions/6609624/cell-comment-does-not-get-displayed-on-ms-office-though-it-comes-on-open-office
//(Comments as tooltips DONE)
//Create a nice DSL : Builders for ExportFormat, EntityDescriptor, more lightweight syntax for CreateColumnGroups
//GOAL: Successfully transform Sysmo Treatment Sheets to Stuart's Format with tracking of provenance information using the DSL

