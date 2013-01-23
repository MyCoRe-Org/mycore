package org.mycore.importer.mapping.mapper;

import org.jdom2.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.metadata.MCRImportLinkIDResolver;

public class MCRImportParentMapper extends MCRImportAbstractMapper {

    public String getType() {
        return "parent";
    }

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        MCRImportLinkIDResolver linkIdResolver = new MCRImportLinkIDResolver();

        if(linkIdResolver != null) {
            Element parentElement = new Element("parent");
            if(linkIdResolver.resolve(map, parseFields(), parentElement))
                importObject.setParent(parentElement);
        }
    }

}