package org.hits.parsing

/**
 * Created by bittkomk on 18/08/14.
 */
class ProvenanceInformation {
    def about
    def sources	= []
    def agents = ["Parser"]
    def actions = []

    String toString() { "Provenance of $about: derived from $sources using $actions" }
}

