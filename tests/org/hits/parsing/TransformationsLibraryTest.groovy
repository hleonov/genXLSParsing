package org.hits.parsing

import spock.lang.Specification

/**
 * Created by bittkomk on 15/09/14.
 */
class TransformationsLibraryTest extends Specification {

    def "Testing matchAndTake transformation"() {
        setup:
        def mat1 = TransformationsLibrary.matchAndTake(/(\d*)\s+(.*)/, 1)
        def mat2 = TransformationsLibrary.matchAndTake(/(\d*)\s+(.*)/, 2)
        def mat3 = TransformationsLibrary.matchAndTake(/(\d*)\s+(.*)/, 3)
        def mat4 = TransformationsLibrary.matchAndTake(/(\d*)\s+(.*)/, 1, "DEFAULT")
        def mat5 = TransformationsLibrary.matchAndTake(/(\d*)\s+(.*)/, 3, "DEFAULT")

        expect:
        mat1.elementTransformation("42 was a bug") == "42"
        mat2.elementTransformation("42 was a bug") == "was a bug"
        mat3.elementTransformation("42 was a bug") == "42 was a bug"
        mat4.elementTransformation("4224") == "DEFAULT"
        mat5.elementTransformation("42 was a bug") == "DEFAULT"
        mat1.listTransformation([1,2,3,4]) == [1,2,3,4]

    }
}
