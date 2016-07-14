package org.hits.parsing

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
        //println "test rule $rule"
        if(rule.antecedent(entity)) {
            //println "apply consequence of rule $rule"
            rule.consequence(entity)
            if(!entity.provenanceInformation) {
                entity.provenanceInformation = []
            }
            entity.provenanceInformation << new ProvenanceInformation(about: rule.consequenceEffects, actions: [rule.name], agents: ["Parser", "RuleEngine"], sources: ["parsed property"])
        }
        else{
            if(rule.alternative){
                //println "apply alternative of rule $rule"
                rule.alternative(entity)
                if(!entity.provenanceInformation) {
                    entity.provenanceInformation = []
                }
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

