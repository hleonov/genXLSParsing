package org.hits.parsing

import spock.lang.Specification

/**
 * Created by bittkomk on 21/08/14.
 */
class ExportFormatTest extends Specification {

    def efb = new ExportFormatFactoryBuilder()
    def exportFormat = efb.exportFormat {
        headerFormat (forSheet:"entity2") {
            row {
                col "id"
                col "velocity"
                col "velocity unit"
                col "very important"
                col "belongsTo"
            }
        }
        headerFormat (forSheet:"entity1") {
            row {
                col "id"
                col "full title"
                col "from l3 with love"
            }
        }
        map (entity: "entity2", toSheet:"entity2") {
            property "funkadelic", to: "very important"
            property "linksTo", to: "belongsTo"
        }

        map (entity: "entity1", toSheet:"entity1") {
            property "fulltitle2", to: "full title"
            property "sthImportantAtL3", to: "from l3 with love"
        }
    }

    def "creating columnIndexLookup for headerFormat"() {
        when:
        def columnIndexLookup = exportFormat.createColumnIndexLookup()

        then:
        columnIndexLookup["entity1"]
        columnIndexLookup["entity2"]
        columnIndexLookup["entity1"]["id"] == 0
        columnIndexLookup["entity1"]["full title"] == 1
        columnIndexLookup["entity1"]["from l3 with love"] == 2
        columnIndexLookup["entity2"]["belongsTo"] == 4
    }
}
