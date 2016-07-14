package org.hits.parsing

import spock.lang.Specification

/**
 * Created by bittkomk on 09/09/14.
 */
class EntityDefinitionHandlerTest extends Specification {

    def edh = new EntityDefinitionHandler()
    def eb = new EntityDescriptorFactoryBuilder()


    def "Defining a new entity definition"() {
        when:
        def ed1 = edh.define("person") {}
        def ed2 = edh.define(entity: "person") {}
        def ed3 = edh.define(null) {}
        def ed4 = edh.define(plentity: "person") {}

        then:
        ed1 instanceof EntityDefinition
        ed1.kind == "person"
        ed1 == ed2
        ed3 == -1
        ed3 == ed4
    }

    def "Defining a new entity with some properties"() {
        when:
        def ed1 = edh.define("test") {
            property name: "default"
            property necessary: "required1"
            property name: "required2", required: true
            property name: "invisible", displayed: false
            property name: "invisible, unsearchable", displayed: false, searchable: false
        }

        then:
        ed1.kind == "test"
        ed1.properties.size() == 5
        ed1.properties.findAll { p -> p.name == "default" && p.required == false && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "required1" && p.required == true && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "required2" && p.required == true && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "invisible" && p.required == false && p.displayed == false && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "invisible, unsearchable" && p.required == false && p.displayed == false && p.searchable == false }.size() == 1
    }

    def "Creating an entity definition from an entity description for parsing"() {
        when:
        def testDescriptor = eb.entity(ofKind: "test"){
            parseProperty "default", from: 1
            parseProperty necessary: "required1", from: 2
            parseProperty "required2", from: 3
            parseProperty "invisible"
            parseProperty "invisible, unsearchable"
        }
        def ed1 = edh.buildDefinitionFromDescriptor(testDescriptor).entityDefinitions.first()

        then:
        ed1.kind == "test"
        ed1.properties.size() == 5
        // default assumptions, probably needing further refinement
        (ed1.properties - ed1.properties.find { p -> p.name == "required1"}).every { p -> p.required == false && p.displayed == true && p.searchable == true }
        ed1.properties.find { p -> p.name == "required1" }.required == true // necessary properties ==> required = true
    }

    def "Creating and refining an entity definition from an entity description for parsing"() {
        when:
        def testDescriptor = eb.entity(ofKind: "test"){
            parseProperty "default", from: 1
            parseProperty necessary: "required1", from: 2
            parseProperty "required2", from: 3
            parseProperty "invisible"
            parseProperty "invisible, unsearchable"
        }
        def ed1 = edh.buildDefinitionFromDescriptor(testDescriptor).refine("test") { // overwrite default assumptions for required, displayed and searchable
            required "required2" // same as required property: "required2"
            doNotDisplay properties: ["invisible", "invisible, unsearchable"]
            notSearchable property: "invisible, unsearchable"
        }.entityDefinitions.first()

        then:
        ed1.kind == "test"
        ed1.properties.size() == 5
        ed1.properties.findAll { p -> p.name == "default" && p.required == false && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "required1" && p.required == true && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "required2" && p.required == true && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "invisible" && p.required == false && p.displayed == false && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "invisible, unsearchable" && p.required == false && p.displayed == false && p.searchable == false }.size() == 1
    }

    def "Handling of links when creating an entity definition from an entity description for parsing"() {
        when:
        def testDescriptor = eb.entity(ofKind: "test") {
            linkTo "other1"
            linkTo necessary:"other2"
            linkTo "other3", name: "named link"
        }

        def ed1 = edh.buildDefinitionFromDescriptor(testDescriptor).entityDefinitions.first()

        then:
        ed1.kind == "test"
        ed1.properties.size() == 3
        // unnamed links get generated names assigned
        // the default values of required, displayed and searchable are different from the default values of properties
        ed1.properties.findAll { p -> p.name == "link1" && p.required == false && p.displayed == false && p.searchable == false }.size() == 1
        // but as for properties information about necessary items is preserved
        ed1.properties.findAll { p -> p.name == "link2" && p.required == true && p.displayed == false && p.searchable == false }.size() == 1
        // names of named links are preserved
        ed1.properties.findAll { p -> p.name == "named link" && p.required == false && p.displayed == false && p.searchable == false }.size() == 1
    }

    def "Exporting and importing entity definitions to JSON"() {
        setup:
        edh.define("test") {
            property name: "default"
            property necessary: "required1"
            property name: "required2", required: true
            property name: "invisible", displayed: false
            property name: "invisible, unsearchable", displayed: false, searchable: false
        }
        def edh2 = new EntityDefinitionHandler()

        when:
        edh.saveTo("test-resources/edef.json")
        edh2.fromJSON("test-resources/edef.json")
        def ed1 = edh2.entityDefinitions.first()

        then:
        ed1.kind == "test"
        ed1.properties.size() == 5
        ed1.properties.findAll { p -> p.name == "default" && p.required == false && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "required1" && p.required == true && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "required2" && p.required == true && p.displayed == true && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "invisible" && p.required == false && p.displayed == false && p.searchable == true }.size() == 1
        ed1.properties.findAll { p -> p.name == "invisible, unsearchable" && p.required == false && p.displayed == false && p.searchable == false }.size() == 1

    }
}
