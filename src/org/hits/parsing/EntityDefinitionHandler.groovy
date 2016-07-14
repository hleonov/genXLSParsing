package org.hits.parsing

import groovy.json.JsonSlurper

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

class EntityDefinition {
    def kind
    def properties = []

    def buildPropertiesFromPropertyDescriptors(List<PropertyDescriptor> propertyDescriptors) {
        propertyDescriptors.unique { pd -> pd.name }.each { pd ->
            properties += [name: pd.name, required: pd.isNecessary, displayed: true, searchable: true, typeInformation: [], constraints: []]
        }

    }

    def buildPropertiesFromLinkingDescriptors(List<LinkingDescriptor> linkingDescriptors) {
        linkingDescriptors.eachWithIndex { ld, i ->
            def name = ld.name ?: "link${i+1}"
            properties += [name: name, required: ld.isNecessary, displayed: false, searchable: false, typeInformation: [], constraints: []]
        }
    }
}


class EntityDefinitionHandler {

    List<EntityDefinition> entityDefinitions = []

    private entityDefinitionToRefine = null

    def refine(value, Closure closure){
        define(value) {}
        refine(closure)
    }

    def refine(Closure closure){
        closure.delegate = this
        closure()
        this
    }

    def define(value, Closure closure){
        def entityKind

        if(value instanceof String){
            entityKind = value
        }
        else if(value instanceof Map && value.entity) {
            entityKind = value.entity
        }
        else {
            println "ERROR in defining entity. Entity kind needed"
            return -1
        }

        entityDefinitionToRefine = entityDefinitions.find { ed -> ed.kind == entityKind }

        if(!entityDefinitionToRefine){
            println "There is no definition for entities of kind $entityKind"
            println "Creating one ..."
            def newEntityDefinition = new EntityDefinition(kind:entityKind)
            entityDefinitions += newEntityDefinition
            entityDefinitionToRefine = newEntityDefinition
        }

        closure.delegate = this
        closure()

        entityDefinitionToRefine
    }

    def updateProperty(propertyToRefine, args){
        args.each { k, v ->
            switch(k){
                case "necessary": propertyToRefine.required = true; break
                case ["required", "displayed", "searchable"] : propertyToRefine."$k" = v; break;
                default: "$k is not a known property of properties"
            }
        }
        propertyToRefine
    }

    def createProperty(name, args) {
        def newProperty = updateProperty([name:name, required: false, searchable: true, displayed: true], args)
        entityDefinitionToRefine.properties += newProperty
        newProperty
    }

    def property(Map args) {
        def name = args.name ?: args.necessary
        if(name){

            def propertyToRefine = entityDefinitionToRefine.properties.find { prop -> prop.name == name }

            if(propertyToRefine) { // update
                println "updating $args"
                updateProperty(propertyToRefine, args)
            }
            else { // create
                println "creating $args"
                createProperty(name, args)
            }

        }
        else{
            println "No property name specified."
        }
    }

    def property(Map args, String value){
        args.name = value
        property(args)
    }

    def parsePropertyRefinement(args, action){
        if(args instanceof String){
            action(args)
        }
        else if(args instanceof Map){
            if(args.property && args.property instanceof String){
                action(args.property)
            }
            else if(args.properties && args.properties instanceof List<String>) {
                args.properties.each { property -> action(property) }
            }
        }
        else {
            println "Error parsing property refinement for $args"
            println "Skipping ..."
        }
    }

    def findAndSetProperty(name, property, value) {
        def propertyToRefine = entityDefinitionToRefine.properties.find { prop -> prop.name == name }

        if(propertyToRefine){
            propertyToRefine."$property" = value
        }
        else{
            println "There is no property $property for entities of kind ${entityDefinitionToRefine.kind}"
            println "Skipping ..."
        }
    }

    def display(String name){
        findAndSetProperty(name, "displayed", true)
    }

    def display(Map args){
        parsePropertyRefinement(args, this.&display)
    }

    def doNotDisplay(String name){
        findAndSetProperty(name, "displayed", false)
    }

    def doNotDisplay(Map args){
        parsePropertyRefinement(args, this.&doNotDisplay)
    }

    def searchable(String name) {
        findAndSetProperty(name, "searchable", true)
    }

    def searchable(Map args){
        parsePropertyRefinement(args, this.&searchable)
    }

    def notSearchable(String name) {
        findAndSetProperty(name, "searchable", false)
    }

    def notSearchable(Map args) {
        parsePropertyRefinement(args, this.&notSearchable)
    }

    def required(String name){
        findAndSetProperty(name, "required", true)
    }

    def required(Map args){
        parsePropertyRefinement(args, this.&required)
    }

    def notRequired(String name){
        findAndSetProperty(name, "required", false)
    }

    def notRequired(Map args){
        parsePropertyRefinement(args, this.&notRequired)
    }

    def buildDefinitionFromDescriptor(EntityDescriptor entityDescriptor) {
        def eDef = new EntityDefinition(kind: entityDescriptor.kind)
        eDef.buildPropertiesFromPropertyDescriptors(entityDescriptor.properties)
        eDef.buildPropertiesFromLinkingDescriptors(entityDescriptor.links)
        entityDefinitions += eDef
        this
    }

    def buildDefinitionsFromDescriptors(List<EntityDescriptor> entityDescriptors){
        entityDefinitions = []

        if(entityDescriptors) {
            entityDescriptors.each { eDsc ->
                buildDefinitionFromDescriptor(eDsc)
            }
        }

        this
    }

    def saveTo(filename) {
        toJSON(filename)
        this
    }

    def toJSON(filename) {
        def jsonString = prettyPrint(toJson(entityDefinitions.groupBy { it.kind }))

        if(filename){
            try{
                new File(filename).withWriter { writer -> writer << jsonString }
            }catch(exception){
                println "some error happened during EntityDefinition.toJSON($filename)"
                println exception
            }
        }

        jsonString
    }

    def fromJSON(filename) {
        try{
            def json = new JsonSlurper().parseText(new File(filename).text)
            entityDefinitions = []

            json.each { kind, data ->
                def eDef = new EntityDefinition(kind: kind, properties: data["properties"].flatten())
                entityDefinitions += eDef
            }

            this

        }catch(exception){
            println "some error happened during EntityDefinition.fromJSON($filename)"
            println exception
        }

        null
    }


    def checkAgainst(entity) {
        true
    }



}
