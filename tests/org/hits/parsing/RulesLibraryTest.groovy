package org.hits.parsing

import spock.lang.Specification

/**
 * Created by bittkomk on 15/09/14.
 */
class RulesLibraryTest extends Specification {

    def "Testing injectProperty rule"() {
        setup:
        def ip1 = RulesLibrary.injectProperty("newProperty", "newValue")
        def ip2 = RulesLibrary.injectProperty("newProperty", { e -> e.name + "_newValue" })
        def e1 = [name: "Hans"]
        def e2 = [name: "Franz"]

        when:
        RuleEngine.applyRule(e1, ip1)
        RuleEngine.applyRule(e2, ip2)

        then:
        e1.newProperty == "newValue"
        e2.newProperty == "Franz_newValue"
    }

    def "Testing setPropertyNullIfNot rule"() {
        setup:
        def spnin1 = RulesLibrary.setPropertyNullIfNot("name", { e -> e.age > 50})
        def e1 = [name: "Hans", age: 49]
        def e2 = [name: "Franz", age: 51]

        when:
        RuleEngine.applyRule(e1, spnin1)
        RuleEngine.applyRule(e2, spnin1)

        then:
        !e1.name
        e2.name == "Franz"
    }
}
