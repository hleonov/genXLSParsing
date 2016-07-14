package org.hits.parsing

class Column {
    def rowIndex
    def colIndex
    def colGroupIndex
    def header
    def fullHeader
    def value
    def name

    String toString() {
        "column at row $rowIndex / column $colIndex has header $header and value $value ${name ? 'named ' + name : ''}"
    }
}

class ColumnGroupDescriptor {
    def regexPatterns
    def start
    def size
    def repetitions
    def headerPattern
    def groupBy
}

class EntityParser {

    def parsingFramework
    List<Column> columns = []
    int rowNumber
    def isColumnGroup = false
    def header


    def parseEntity() {}
    def parseProperty() {}
    def parseLink() {}

    String toString() {
        columns.collect { it.toString() }.join("\n")
    }

}

class DataRow extends EntityParser {

    def identityTransformation = new Transformation()
    def stuffToPostprocess = []
    def dataRowParent = this

    def parseEntity(EntityDescriptor ed) {
        def newEntity = [kind: ed.kind, fromRow: rowNumber, provenanceInformation: [], necessaryProperties: []]

        ed.properties.each { pd -> parseProperty(pd, newEntity) }
        ed.links.each { ld -> dataRowParent.stuffToPostprocess << [name: "link", entity: newEntity, action: {parseLink(ld, newEntity)}] }
        ed.rules.each { rule -> dataRowParent.stuffToPostprocess << [name: rule.name, entity: newEntity, action: {RuleEngine.applyRule(newEntity, rule)}] }


        parsingFramework.addParsedEntity(newEntity)
        parsingFramework.addEntityDescriptor(ed)
    }

    def postprocess(){
        //println "do some post-processing..."
        //println "${stuffToPostprocess.size()} things to do!"
        stuffToPostprocess.each { stp ->
            stp.action()
        }
    }

    def buildMatchingString(str, style) {
        def prefix = /(?i)^/
        def suffix = /$/
        switch (style) {
            case MatchingStyle.PREFIX:
                suffix = /.*$/; break
            case MatchingStyle.INFIX:
                prefix = /(?i)^.*/; suffix = /.*$/; break
            case MatchingStyle.SUFFIX:
                prefix = /(?i)^.*/; break
        }

        if (str.contains("=")) {
            str = str.split("=")[1]
        }

        prefix + str + suffix
    }

    def buildHeaderString(col, from) {
        if (from.contains("=")) {
            def descriptor = from.split("=")[0]
            col.fullHeader.find { h -> h.descriptor == descriptor }?.value ?: col.header
        } else {
            col.header
        }
    }

    def getProvenanceSuffix(where) {
        if(where.startsWith("header") && where.contains("@")) {
            "${where.split('@')[1]}"
        }
        else if(!where.startsWith("header")){
            "$rowNumber"
        }
        else {
            ""
        }
    }

    def parseColumn(col, where) {
        if (where.startsWith("header")) {
            if (where.contains("@")) { // parse specified position in header
                def descriptor = where.split("@")[1]
                col.fullHeader.find { h -> h.descriptor == descriptor }?.value ?: ""
            } else { // return simple string representation of header cells
                col.header
            }
        } else { // parse from cell
            col.value
        }
    }

    def searchColumn(PropertyDescriptor pd, colIndex) {
        def col = null
        def cols

        if(pd.from instanceof Number){
            col = columns.find { it."$colIndex" == pd.from }
        }
        else{ // pd.from instanceof String
            if(isColumnGroup && namedColumns){ // if we are in a columnGroup and have named columns try to match their names first
                //println "searching for column with name ${pd.from} in columnGroup"
                cols = columns?.findAll { it.name == pd.from }
                if(cols) { col = cols[pd.take - 1] }
            }
            if(!col){ // // perfom a kind of infix search that also allows for regex patterns (also applied when the above search for matches was not successful)
                cols = columns?.findAll { c -> buildHeaderString(c, pd.from) ==~ buildMatchingString(pd.from, pd.matchingStyle) }
                if(cols) { col = cols[pd.take - 1] }
            }
        }

        col
    }

    def parseProperty(PropertyDescriptor pd, entity) {

        if(pd.isNecessary){
            entity.necessaryProperties << pd.name
        }

        def where = pd.where.toLowerCase()

        def value = null
        def transform = pd.using ?: identityTransformation
        def colIndex = isColumnGroup ? "colGroupIndex" : "colIndex"

        def provenance = new ProvenanceInformation(about: pd.name, actions:[transform.toString()])
        def provenanceSource = { header -> "${parsingFramework.sheetName}:$header${(pd.take > 1) ? ':' + pd.take : ''}@${getProvenanceSuffix(where)}" }

        def searchColumn = this.&searchColumn.rcurry(colIndex)  // currying --> fixate colIndex parameter
        def parseColumn =  this.&parseColumn.rcurry(where) // currying --> fixate where parameter



        if(!(pd.from instanceof List)){
            def col = searchColumn(pd)

            if(col){
                value = transform.elementTransformation(parseColumn(col))
                provenance.sources += provenanceSource(col.header)
            }
        }
        else{ // process list

            def colIndices = pd.from.findAll { it instanceof Number }
            def headerLabelsOrNames = pd.from - colIndices

            def columnsToParse = []

            if(isColumnGroup && namedColumns){
                columnsToParse += columns.findAll { col -> headerLabelsOrNames.any { hl -> col.name == hl }}
            }

            if(!columnsToParse){
                columnsToParse += columns.findAll { col -> col."$colIndex" in colIndices ||
                        headerLabelsOrNames.any { hl -> buildHeaderString(col, pd.from) ==~ buildMatchingString(hl, pd.matchingStyle) } }
            }

            def parsedValues = columnsToParse.collect { col -> transform.elementTransformation(parseColumn(col)) }
            def provenanceSources = columnsToParse.collect { provenanceSource(it.header) }

            value = transform.listTransformation(parsedValues)
            provenance.sources += provenanceSources
        }

        if(value){
            entity."${pd.name}" = value
            entity.provenanceInformation << provenance
        }
        else {
            println "DEBUGGING: no value parsed for entity: $entity and property descriptor: $pd"
        }

        //println entity
        entity
    }

    def parseLink(LinkingDescriptor ld, entity) {

        if(ld.isNecessary){
            entity.necessaryProperties << "linksTo"
        }

        def newLink = null

        def id = parsingFramework.askForLinkID(ld, rowNumber, entity)
        if(id){
            newLink = [kind: ld.entityKind, id: id]
        }
        else{
            println "error getting link for $ld in row $rowNumber"
        }

        if(newLink){

            if(ld.name) {
                newLink.name = ld.name
            }

            if(entity.linksTo){
                entity.linksTo += newLink
            }
            else{
                entity.linksTo = [newLink]
            }
        }

        entity
    }

    def columnGroup(args, Closure c) { // DSL helper method
        createColumnGroups(new ColumnGroupDescriptor(args)).each { cg ->
            c.resolveStrategy = Closure.DELEGATE_FIRST // otherwise it will execute parseEntity on owner = this first!
            c.delegate = cg
            c()
        }
    }

    def createColumnGroups(ColumnGroupDescriptor cgd) {
        def columnGroups = []

        if(cgd.headerPattern){
            columnGroups = createHeaderPatternColumnGroups(cgd)
        }
        else if(cgd.start && cgd.size && cgd.repetitions){
            columnGroups = createFixedPatternColumnGroups(cgd)
        }
        else if(cgd.regexPatterns) {
            columnGroups = createRegexColumnGroups(cgd)
        }
        else {
            println "Don't know how to handle columnGroupDescriptor $cgd"
        }

        columnGroups
    }

    def createRegexColumnGroups(ColumnGroupDescriptor cgd) {

        def regexPatterns = cgd.regexPatterns
        def namesForRegexPatterns = [:]
        def namedRegexPatterns = false

        //converting named regexs
        if (regexPatterns instanceof Map) {
            regexPatterns.each { k, v -> namesForRegexPatterns[v] = k }
            regexPatterns = regexPatterns.values()
            namedRegexPatterns = true
        }

        groupRegexPatternMatches(cgd, collectRegexPatternMatches(regexPatterns, namesForRegexPatterns), namedRegexPatterns)
    }

    def groupRegexPatternMatches(ColumnGroupDescriptor cgd, Map allMatchedColumns, Boolean namedRegexPatterns) {
        def columnGroups = []

        // if groupBy is not defined use Groovy's transpose method on list of lists as default
        if (!cgd.groupBy) {
            allMatchedColumns.values().toList().transpose().each { matchedColumns ->
                matchedColumns.inject(1) { acc, val -> val.colGroupIndex = acc; ++acc }
                columnGroups << new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: matchedColumns, parsingFramework: parsingFramework, namedColumns: namedRegexPatterns)
            }
        } else {
            def allMatchedColumnsFlattened = allMatchedColumns.values().toList().flatten()
            def allMatchedColumnsGrouped

            if (cgd.groupBy.contains("@")) { // use defined part of header to perform grouping
                def descriptor = cgd.groupBy.split("@")[1]
                allMatchedColumnsGrouped = allMatchedColumnsFlattened.groupBy { mc ->
                    mc.fullHeader.find { fh -> fh.descriptor == descriptor }?.value
                }
            } else { // use all parts of header description to perform grouping
                allMatchedColumnsGrouped = allMatchedColumnsFlattened.groupBy { mc -> mc.header }
            }


            allMatchedColumnsGrouped.each { k, matchedColumns ->
                matchedColumns.inject(1) { acc, val -> val.colGroupIndex = acc; ++acc }
                columnGroups << new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: matchedColumns, parsingFramework: parsingFramework, namedColumns: namedRegexPatterns)
            }
        }

        columnGroups
    }

    def collectRegexPatternMatches(Collection regexPatterns, namesForRegexPatterns) {
        def allMatchedColumns = [:]

        // collect matching columns for each regex pattern
        regexPatterns.eachWithIndex { pattern, index ->

            def columnName = namesForRegexPatterns[pattern] // null if no names were defined

            def matchedColumns = columns.findAll { col -> col.header ==~ pattern }

            if (matchedColumns) {
                // inject name for that column and add to allMatchedColumns
                matchedColumns*.name = columnName
                allMatchedColumns[index] = matchedColumns
            } else {
                //if no matches were found for the given pattern, search directly in the list of headers in case there is a column without data to be mapped
                def matchedHeaders = header.findAll { h -> h.collect { it.value }.join(".") ==~ pattern }
                if (matchedHeaders) {
                    allMatchedColumns[index] = matchedHeaders.collect { mh ->
                        new Column(rowIndex: rowNumber, colIndex: mh.colIndex.first(),
                                header: mh.collect { it.value }.join("."),
                                fullHeader: mh,
                                value: "",
                                name: columnName)
                    }
                } else {
                    println "found no matching columns for $pattern"
                }
            }
        }

        allMatchedColumns
    }

    def createFixedPatternColumnGroups(ColumnGroupDescriptor cgd) {
        def sortedColumns = columns.sort { it.colIndex }
        def columnGroups = []

        cgd.repetitions.times { ri ->

            if (sortedColumns.size() >= (cgd.start - 1 + ri * cgd.size + cgd.size)) {
                def selectedColumns = []

                cgd.size.times { si ->
                    def tmpColumn = sortedColumns[cgd.start - 1 + ri * cgd.size + si]
                    tmpColumn.colGroupIndex = si + 1
                    selectedColumns << tmpColumn
                }

                columnGroups << new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: selectedColumns, parsingFramework: parsingFramework)
            }
        }

        columnGroups
    }

    def createHeaderPatternColumnGroups(ColumnGroupDescriptor cgd) {
        def patternLength = cgd.headerPattern.size()
        def patternsFound = []
        def columnHeaders = columns.collect { it.header }
        def columnsByColIndex = columns.groupBy { it.colIndex }

        //println "§§§§§§ columnsByColIndex " + columnsByColIndex
        //println columnHeaders

        def i = -1
        def offset = 0

        while ((i = Collections.indexOfSubList(columnHeaders, cgd.headerPattern)) >= 0) {

            def matchedColumns = columnHeaders[i..<(i + patternLength)].collect {
                new Column(header: it, colIndex: 0, colGroupIndex: 0, rowIndex: rowNumber, value: "")
            }
            matchedColumns.inject(1) { acc, val ->
                val.colIndex = offset + i + acc
                val.colGroupIndex = acc
                val.value = (columnsByColIndex[val.colIndex]?.value instanceof List) ? columnsByColIndex[val.colIndex]?.value?.first() : columnsByColIndex[val.colIndex]?.value
                //println columnsByColIndex[val.colIndex]

                ++acc
            }

            columnHeaders = (columnHeaders.size() > i + patternLength) ? columnHeaders[(i + patternLength)..-1] : []

            offset = offset + i + patternLength

            patternsFound << matchedColumns
        }


        patternsFound.collect { cols ->
            new ColumnGroup(dataRowParent: this, rowNumber: rowNumber, columns: cols, parsingFramework: parsingFramework)
        }
    }
}

class ColumnGroup extends DataRow {

    def isColumnGroup = true
    def namedColumns = false

}


