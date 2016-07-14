package org.hits.parsing

/**
 * Created by bittkomk on 15/09/14.
 */
class TransformationsLibrary {

    static matchAndTake = { regex, group, defaultValue = null ->
        new Transformation(name: "match with regex $regex and replace with matching group $group",
                elementTransformation: { value ->
                    def m = (value =~ regex)

                    if (m.matches()) {
                        //println "$value matches $regex"
                        try {
                            return m.group(group)
                        }catch(IndexOutOfBoundsException e){
                            println "error. group $group does not exist"
                        }
                    }

                    return (defaultValue instanceof String) ? defaultValue : value

                },
                listTransformation: { it })
    }

    static addToBinding(binding) {
        binding.matchAndTake = matchAndTake

        binding
    }
}
