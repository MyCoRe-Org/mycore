package org.mycore.frontend.xeditor.mapper;

import java.util.Iterator;

import org.jdom2.Document;
import org.jdom2.ProcessingInstruction;
import org.jdom2.filter.Filters;

public class MCRMappingDecoder {

    private MCRFieldCoder coder = new MCRFieldCoder();

    private MCRFieldMapping mapping = new MCRFieldMapping();

    public MCRFieldMapping decode(Document xml) {
        xml.getDescendants(Filters.processinginstruction()).forEach(pi -> {
            MCRField field = coder.decode(pi);
            mapField(field);
        });

        removeMappingCode(xml);

        return mapping;
    }

    private void removeMappingCode(Document xml) {
        for (Iterator<ProcessingInstruction> iter = xml.getDescendants(Filters.processinginstruction()).iterator();
            iter.hasNext();) {
            iter.next();
            iter.remove();
        }
    }

    private void mapField(MCRField newField) {
        String fieldName = newField.getName();
        if (mapping.hasField(fieldName)) {
            MCRField field = mapping.getField(fieldName);
            field.addNode(newField.getNodes().get(0));
        }
        mapping.registerInName2Field(newField);
        mapping.registerInNode2Field(newField);
    }
}
