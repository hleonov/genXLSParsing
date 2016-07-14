package org.hits.parsing


class EntityDescriptorFactory extends AbstractFactory {

    public boolean isLeaf() {
        false
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        def ed = new EntityDescriptor(kind: attributes.ofKind)

        if(attributes.matchingStyle) { ed.defaultMatchingStyle = attributes.matchingStyle }

        ed
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}

class PropertyDescriptorFactory extends AbstractFactory {

    public boolean isLeaf() {
        true
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

        def pd = new PropertyDescriptor(where: "column")

        if(value) { pd.name = value }

        if(attributes.necessary) { pd.isNecessary = true; pd.name = attributes.necessary }

        if(attributes.from && attributes.from instanceof String && !attributes.from.contains("=") && (attributes.from.startsWith("column") || attributes.from.startsWith("header"))){
            pd.where = attributes.from
            pd.from = attributes.of
        }
        else{
            pd.from = attributes.from
        }

        if(attributes.take) { pd.take = attributes.take }

        if(attributes.matchingStyle) { pd.matchingStyle = attributes.matchingStyle }

        pd

    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object propertyDescriptor) {
        if(parent != null && parent instanceof EntityDescriptor){
            if(!propertyDescriptor.matchingStyle) { propertyDescriptor.matchingStyle = parent.defaultMatchingStyle }
            parent.properties << propertyDescriptor
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}

class TransformationFactory extends AbstractFactory {

    public boolean isLeaf() {
        false
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        // make identity transformation {it} default if nothing different is specified
        if(value instanceof Transformation){
            def transformation = value
            transformation.onProperty = attributes.onProperty
            transformation
        }
        else{
            new Transformation(name: value, elementTransformation: attributes.ofElements ?: {it}, listTransformation: attributes.ofList ?: {it}, onProperty: attributes.onProperty)
        }
    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object transformation) {
        if(parent != null && parent instanceof EntityDescriptor){
            parent.properties.findAll { prop -> prop.name == transformation.onProperty }.each { prop -> prop.using = transformation }
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}

class LinkingDescriptorFactory extends AbstractFactory {

    public boolean isLeaf() {
        true
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        def ld = new LinkingDescriptor(entityInSameRow: true, isNecessary: false)

        if(value) { ld.entityKind = value }
        if(attributes.necessary) { ld.isNecessary = true; ld.entityKind = attributes.necessary }
        if(attributes.withID) { ld.entityInSameRow = false; ld.entityID = attributes.withID }
        if(attributes.property) { ld.linkToProperty = attributes.property }
        if(attributes.where) { ld.where = attributes.where }
        if(attributes.name) { ld.name = attributes.name }

        ld
    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object linkingDescriptor) {
        if(parent != null && parent instanceof EntityDescriptor){
            parent.links << linkingDescriptor
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}


class RuleFactory extends AbstractFactory {

    public boolean isLeaf() {
        true
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if(value instanceof Rule){
            value
        }
        else{
            new Rule(name: value)
        }
    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object rule) {
        if(parent != null && parent instanceof EntityDescriptor){
            parent.rules << rule
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        true
    }

}


class EntityDescriptorFactoryBuilder extends FactoryBuilderSupport {
    public EntityDescriptorFactoryBuilder(boolean init = true){
        super(init)
    }

    def registerObjectFactories() {
        registerFactory("entity", new EntityDescriptorFactory())
        registerFactory("parseProperty", new PropertyDescriptorFactory())
        registerFactory("applyTransformation", new TransformationFactory())
        registerFactory("linkTo", new LinkingDescriptorFactory())
        registerFactory("applyRule", new RuleFactory())
        registerFactory("applyAction", new RuleFactory())

    }
}
