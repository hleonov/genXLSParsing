package org.hits.parsing

import spock.lang.Specification

/**
 * Created by bittkomk on 19/08/14.
 */
class RuleEngineTest extends Specification {

    def entity1 = [id:1, name:"Test Me", age:-42]
    def entity2 = [id:2, name:"Test Mee", age:42]
    def rule1 = new Rule(name: "test rule 1", antecedent: { e -> e.name == "Test Me"}, consequence: { e -> e.name = e.name.toUpperCase() },
            consequenceEffects: "name", alternative: { e -> e.name = e.name.toLowerCase() }, alternativeEffects: "name")
    def rule1WithoutAlternative = new Rule(name: "test rule 1 without alternative", antecedent: { e -> e.name == "Test me"}, consequence: { e -> e.name = e.name.toUpperCase() },
            consequenceEffects: "name")
    def rule2 = new Rule(name: "test rule 2", antecedent: { e -> e.age < 0 }, consequence: { e -> e.age *= -1 },
            consequenceEffects: "age")
    def rule3 = new Rule(name: "test rule 3", antecedent: { e -> e.age == 42 }, consequence: { e -> e.age *= 10 },
            consequenceEffects: "age")

    def "applying rule -- consequence branch"() {
        when:
        RuleEngine.applyRule(entity1, rule1)

        then:
        entity1.name == "TEST ME"
        entity1.provenanceInformation != null
        entity1.provenanceInformation.first().about == "name"
        entity1.provenanceInformation.first().actions.contains("test rule 1")
    }

    def "applying rule -- alternative branch"() {
        when:
        RuleEngine.applyRule(entity2, rule1)

        then:
        entity2.name == "test mee"
        entity2.provenanceInformation != null
        entity2.provenanceInformation.first().about == "name"
        entity2.provenanceInformation.first().actions.contains("test rule 1")
    }

    def "applying rule -- antecedent false, no alternative defined"() {
        when:
        RuleEngine.applyRule(entity2, rule1WithoutAlternative)

        then:
        entity2.name == "Test Mee"
        entity2.provenanceInformaton == null
    }

    def "applying multiple rules"() {
        when:
        RuleEngine.applyRules(entity1, rule2, rule3)
        RuleEngine.applyRules(entity2, rule2, rule3)

        then:
        entity1.age == 420
        entity1.age == entity2.age
        entity1.provenanceInformation != null
        entity1.provenanceInformation.size() == 2
        entity1.provenanceInformation.every { it.about == "age" }
        entity1.provenanceInformation.collect { it.actions.first() } == [ "test rule 2", "test rule 3"]
    }

    def "applying multiple rules -- order matters"() {
        when:
        RuleEngine.applyRules(entity1, rule3, rule2)
        RuleEngine.applyRules(entity2, rule3, rule2)

        then:
        entity1.age == 42
        entity2.age == 420
        entity1.provenanceInformation.size() == 1
        entity2.provenanceInformation.size() == 1
        entity1.provenanceInformation.collect { it.actions.first() } == ["test rule 2"]
        entity2.provenanceInformation.collect { it.actions.first() } == ["test rule 3"]
    }
}
