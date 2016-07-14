package org.hits.parsing

/**
 * Created by bittkomk on 18/08/14.
 */
class Parse {

    static createParserBinding(args) {

        def parserBinding = new Binding()

        parserBinding.pf = new ParsingFramework()
        parserBinding.eb = new EntityDescriptorFactoryBuilder()
        parserBinding.efb = new ExportFormatFactoryBuilder()
        parserBinding.hfb = new HeaderFormatFactoryBuilder()
        parserBinding.eDefHandler = new EntityDefinitionHandler()

        parserBinding.entity = { eargs, closure -> parserBinding.eb.entity(eargs, closure) }
        parserBinding.exportFormat = { closure -> parserBinding.ef = parserBinding.efb.exportFormat(closure) }
        parserBinding._embedProvenanceInformation_ = false
        parserBinding._exportUniqueEntities_ = false
        parserBinding.export = { filename -> parserBinding.pf.toXLS(filename, parserBinding.ef, parserBinding._embedProvenanceInformation_, parserBinding._exportUniqueEntities_) }
        parserBinding.loadWorkbook = { filename -> parserBinding.pf.loadWorkbook(filename) }
        parserBinding.useSheet = { sheetName -> parserBinding.pf.useSheet(sheetName) }
        parserBinding.transpose = { parserBinding.pf.transpose() }
        parserBinding.parse = { closure -> parserBinding.pf.parse(closure, parserBinding.hf) }
        parserBinding.saveJSON = { filename, fullOutput = true, linksToProps = false ->
            def json = parserBinding.pf.toJSONString(fullOutput, linksToProps)
            try {
                new File(filename).withWriter { writer -> writer << json }
            } catch (e) {
                println "eror saving json to $filename"
                println e
            }
        }
        parserBinding.saveEntities = { filename = "", linksToProps = false -> parserBinding.saveJSON.call(filename, false, linksToProps) }
        parserBinding.saveEntitiesWithLinksAsProperties = { filename = "" -> parserBinding.saveEntities.call(filename, true) }
        parserBinding.printJSON = { fullOutput = true, linksToProps = false -> println parserBinding.pf.toJSONString(fullOutput, linksToProps) }
        parserBinding.printEntities = { linksToProps = false -> parserBinding.printJSON.call(false, linksToProps) }
        parserBinding.printEntitiesWithLinksAsProperties = { -> parserBinding.printEntities.call(true) }
        parserBinding.args = (args && args.size() > 1) ? args[1..-1] : []
        parserBinding.headerFormat = { closure -> parserBinding.hf = parserBinding.hfb.headerFormat(closure) }
        parserBinding.createEntityDefinitions = { Closure closure = null ->
            parserBinding.eDefHandler.buildDefinitionsFromDescriptors(parserBinding.pf.getEntityDescriptors());
            if (closure) {
                parserBinding.eDefHandler.refine(closure)
            } else {
                parserBinding.eDefHandler
            }
        }

        parserBinding = TransformationsLibrary.addToBinding(parserBinding)
        parserBinding = RulesLibrary.addToBinding(parserBinding)

        parserBinding.FIRST = 1
        parserBinding.SECOND = 2
        parserBinding.THIRD = 3
        parserBinding.FOURTH = 4
        parserBinding.FIFTH = 5
        parserBinding.SIXTH = 6
        parserBinding.SEVENTH = 7
        parserBinding.STRICT = MatchingStyle.STRICT
        parserBinding.EXACT = MatchingStyle.STRICT
        parserBinding.PREFIX = MatchingStyle.PREFIX
        parserBinding.SUFFIX = MatchingStyle.SUFFIX
        parserBinding.INFIX = MatchingStyle.INFIX
        parserBinding.SUBSTRING = MatchingStyle.INFIX

        if (parserBinding.args) {
            def cli = new CliBuilder()
            cli.inputFile(args: 1, argName: 'file', 'excel file to parse')
            cli.sheet(args: 1, argName: 'sheet', 'sheet in workbook to parse')
            cli.outputFile(args: 1, argName: 'file', 'excel file to export parsed data to')
            cli.jsonOutputFile(args: 1, argName: 'file', 'filename for json export of entries')
            cli.embedProvenanceInformation(args: 0, 'boolean switch to trigger embedding of provenance information')

            def options = cli.parse(parserBinding.args)

            parserBinding._inputFile_ = options.inputFile ?: "NOT DEFINED"
            parserBinding._sheet_ = options.sheet ?: "NOT DEFINED"
            parserBinding._outputFile_ = options.outputFile ?: "NOT DEFINED"
            parserBinding._jsonOutputFile_ = options.jsonOutputFile ?: "NOT DEFINED"
            parserBinding._embedProvenanceInformation_ = options.embedProvenanceInformation
        }

        parserBinding
    }

    static main(args) {

        if (args) {
            def parserBinding = createParserBinding(args)
            new GroovyShell(parserBinding).evaluate(new File(args[0]))
            if(parserBinding._jsonOutputFile_ != "NOT DEFINED") {
                parserBinding.saveEntitiesWithLinksAsProperties.call(parserBinding._jsonOutputFile_)
            }
        } else {
            println "Usage: Parse.jar <parserDefinition> <additional arguments>"
        }


    }

}
