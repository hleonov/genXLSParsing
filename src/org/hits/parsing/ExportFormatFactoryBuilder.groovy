package org.hits.parsing


class ExportFormatFactory extends AbstractFactory {

    public boolean isLeaf() {
        false
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        def exportFormat = new ExportFormat()
        exportFormat.headerFormat = [:]
        exportFormat.entityToSpreadsheetMappings = [:]
        exportFormat
    }

}

class HeaderFormatFactory extends AbstractFactory {

    public boolean isLeaf() {
        false
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        new HeaderFormat(sheetName: attributes.forSheet)
    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object headerFormat) {
        if(parent != null && parent instanceof ExportFormat){
            parent.headerFormat[headerFormat.sheetName] = headerFormat
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}

class HeaderRowFactory extends AbstractFactory {

    public boolean isLeaf() {
        false
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        new HeaderRow(descriptor: value)
    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object headerRow) {
        if(parent != null && parent instanceof HeaderFormat){
            headerRow.row = parent.headerRows?.max { it.row }?.row?.plus(1) ?: 1
            parent.headerRows << headerRow
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }
}

class HeaderColumnFactory extends AbstractFactory {

    public boolean isLeaf() {
        true
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

        if(value && !attributes){
            new HeaderColumn(label: value)

        }
        else{
            new HeaderColumn()
        }

    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object headerColumn) {
        if(parent != null && parent instanceof HeaderRow){
            headerColumn.column = parent.headerColumns?.max { it.column }?.column?.plus(1) ?: 1
            parent.headerColumns << headerColumn
        }
    }
}

class EntityToSpreadsheetMappingFactory extends AbstractFactory {

    public boolean isLeaf() {
        false
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        new EntityToSpreadsheetMapping(sheetName: attributes.toSheet, entityKind: attributes.entity)
    }

    /*public void setParent(FactoryBuilderSupport builder, Object parent, Object entityToSpreadsheetMapping) {
        if(parent != null && parent instanceof ExportFormat){
            parent.entityToSpreadsheetMappings[entityToSpreadsheetMapping.entityKind] = entityToSpreadsheetMapping
        }
    }*/

    public void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object entityToSpreadsheetMapping){
        if(parent != null && parent instanceof ExportFormat){
            // add standard propertyToColumnMappings for columns not mapped explicitly
            def hf = parent.headerFormat[entityToSpreadsheetMapping.sheetName]

            if(hf){
                hf.headerRows.each { hr ->
                    hr.headerColumns.each { hc ->
                        if(!entityToSpreadsheetMapping.propertyToColumnMappings.find { ptcm -> ptcm.columnHeader == hc.label }){
                            //println "create default property to column mapping: ${hc.label} --> ${hc.label}"
                            entityToSpreadsheetMapping.propertyToColumnMappings << new PropertyToColumnMapping(property: hc.label, columnHeader: hc.label)
                        }
                    }
                }
            }

            parent.entityToSpreadsheetMappings[entityToSpreadsheetMapping.entityKind] = entityToSpreadsheetMapping
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}

class PropertyToColumnMappingFactory extends AbstractFactory {

    public boolean isLeaf() {
        true
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        new PropertyToColumnMapping(property: value, columnHeader: attributes.to)
    }

    public void setParent(FactoryBuilderSupport builder, Object parent, Object propertyToColumnMapping) {
        if(parent != null && parent instanceof EntityToSpreadsheetMapping){
            parent.propertyToColumnMappings << propertyToColumnMapping
        }
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes){
        false // das verhindet das initialisieren des neunen HeaderFormat Objektes mit den im Builder spezifizierten Attributen
        // sinnvoll, wenn andere attributnamen für den builder verwendet werden sollen...
    }

}

class ExportFormatFactoryBuilder extends FactoryBuilderSupport {
    public ExportFormatFactoryBuilder(boolean init = true){
        super(init)
    }

    def registerObjectFactories() {
        registerFactory("exportFormat", new ExportFormatFactory())
        registerFactory("headerFormat", new HeaderFormatFactory())
        registerFactory("row", new HeaderRowFactory())
        registerFactory("col", new HeaderColumnFactory())
        registerFactory("map", new EntityToSpreadsheetMappingFactory())
        registerFactory("property", new PropertyToColumnMappingFactory())
    }
}
