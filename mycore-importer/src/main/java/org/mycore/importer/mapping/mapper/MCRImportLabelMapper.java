package org.mycore.importer.mapping.mapper;

import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;
import org.mycore.importer.mapping.resolver.uri.MCRImportURIResolverMananger;

public class MCRImportLabelMapper extends MCRImportAbstractMapper {

    protected MCRImportFieldValueResolver fieldResolver;

    public String getType() {
        return "label";
    }

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        

        // create a new field resolver
        fieldResolver = new MCRImportFieldValueResolver(fields);

        // value
        String labelValue = map.getAttributeValue("value");
        if(labelValue != null)
            labelValue = fieldResolver.resolveFields(labelValue);
        else
            labelValue = fieldResolver.getNotUsedFields().get(0).getValue();

        // resolver
        String resolver = map.getAttributeValue("resolver");
        if(resolver != null && !resolver.equals("")) {
            // try to resolve the uri to get the new value
            labelValue = MCRImportURIResolverMananger.getInstance().resolveURI(resolver, labelValue);
        }
        importObject.setLabel(labelValue);
    }
}