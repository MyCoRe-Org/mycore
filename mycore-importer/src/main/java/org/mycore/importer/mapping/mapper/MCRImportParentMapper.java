package org.mycore.importer.mapping.mapper;

import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.metadata.MCRImportLinkIDResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;

public class MCRImportParentMapper extends MCRImportAbstractMapper {

    public String getType() {
        return "parent";
    }

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        MCRImportMetadataResolver resolver = createResolverInstance();

        if(resolver != null && resolver instanceof MCRImportLinkIDResolver) {
            Element parentElement = resolver.resolve(map, fields);
            importObject.setParent(parentElement);
        }
    }

}
