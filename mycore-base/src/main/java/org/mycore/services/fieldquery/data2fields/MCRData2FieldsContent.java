package org.mycore.services.fieldquery.data2fields;

import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRData2FieldsContent extends MCRIndexEntryBuilder {
    public MCRData2FieldsContent(String index, MCRContent content, MCRObjectID id) {
        entry.setEntryID(id.toString());
        String type = id.getTypeId();
        if ("derivate".equals(type)) {
            MCRFieldsSelector selector = new MCRFieldsSelectorBase(index, "derivate", "derivateMetadata");
            slaves.add(new MCRData2FieldsXML(content, selector));
        } else {
            MCRFieldsSelector selector = new MCRFieldsSelectorBase(index, type, "objectMetadata");
            slaves.add(new MCRData2FieldsXML(content, selector));
            selector = new MCRFieldsSelectorBase(index, type, "objectCategory");
            slaves.add(new MCRData2FieldsXML(content, selector));
        }
    }
}
