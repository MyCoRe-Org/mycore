package org.mycore.importer.mapping.mapper;

import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.metadata.MCRImportLinkIDResolver;

public class MCRImportDerivateMapper extends MCRImportAbstractMapper {

    public String getType() {
        return "derivate";
    }

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        MCRImportLinkIDResolver linkIdResolver = new MCRImportLinkIDResolver();

        if (linkIdResolver != null) {
            Element derElement = new Element("derobject");
            if (linkIdResolver.resolve(map, parseFields(), derElement))
                importObject.addDerivate(derElement);
        }
    }

}