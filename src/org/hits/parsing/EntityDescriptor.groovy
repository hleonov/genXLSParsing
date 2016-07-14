package org.hits.parsing


enum MatchingStyle {
    STRICT, PREFIX, INFIX, SUFFIX
}

class Transformation {
    String name = "Identity"
    Closure listTransformation = { it }
    Closure elementTransformation = { it }
    String onProperty // helping field for Builder

    String toString() { name }

}

class EntityDescriptor {
    def kind
    List<PropertyDescriptor> properties = []
    List<LinkingDescriptor> links = []
    List<Rule> rules = []
    def defaultMatchingStyle = MatchingStyle.PREFIX

    String toString() {
        "entity of kind $kind with ${properties.size()} properties and ${links.size()} links"
    }

    boolean equals(other){
        def eq = true

        eq &= (this.kind == other.kind)
        eq &= (this.properties.size() == other.properties.size())
        eq &= (this.links.size() == other.links.size())
        eq &= (this.properties.every { prop -> prop in other.properties })
        eq &= (this.links.every { link -> link in other.links })

        eq
    }
}

class PropertyDescriptor {
    def name
    def where // columns / header
    def from
    def take = 1
    Transformation using
    def isNecessary = false
    def matchingStyle

    /*def displayed = true
    def searchable = true
    def typeInformation = "String"
    def constraints*/

    String toString() {
        "property $name is located at $where : $from. transformed by $using"
    }

    boolean equals(other) {
        (this.name == other.name && this.isNecessary == other.isNecessary)
    }
}

class LinkingDescriptor {
    def name
    def entityKind
    def entityID
    def entityInSameRow
    def isNecessary = false
    def linkToProperty = "id"
    def where

    String toString() {
        "link to entity of kind $entityKind ${entityInSameRow ? 'in same row' : ''}"
    }

    boolean equals(other) {
        (this.entityKind == other.entityKind && this.isNecessary == other.isNecessary && this.entityInSameRow == other.entityInSameRow && this.linkToProperty == other.linkToProperty)
    }
}