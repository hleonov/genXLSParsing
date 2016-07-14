package org.hits.parsing

/**
 * Created by bittkomk on 15/09/14.
 */
class RulesLibrary {

    static injectProperty = { property, value ->
        new Rule(name: "inject new property $property with value ${(value instanceof Closure) ? 'calculated by closure' : value}",
                antecedent: { e -> true },
                consequence: { e -> e."$property" = (value instanceof Closure) ? value(e) : value },
                consequenceEffects: property)
    }

    static setPropertyNullIfNot = { property, condition ->
        new Rule(name: "check $property and set to null if check not positive",
                antecedent: { e -> condition(e) },
                consequence: { e -> e },
                alternative: { e -> e."$property" = null },
                alternativeEffects: property)
    }

    static addToBinding(binding) {
        binding.injectProperty = injectProperty
        binding.setPropertyNullIfNot = setPropertyNullIfNot

        binding
    }

}
