package org.hits.parsing

class HeaderFormatFactoryBuilder extends FactoryBuilderSupport {
    public HeaderFormatFactoryBuilder(boolean init = true){
        super(init)
    }

    def registerObjectFactories() {
        registerFactory("headerFormat", new HeaderFormatFactory())
        registerFactory("row", new HeaderRowFactory())
    }
}
