package org.mycore.importer.mapping.mapper;

import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;
import org.mycore.importer.mapping.resolver.uri.MCRImportURIResolverMananger;

/**
 * This class is used to set the id of an mcrobject.
 * 
 * @author Matthias Eichner
 */
public class MCRImportIdMapper extends MCRImportAbstractMapper {

    protected MCRImportFieldValueResolver fieldResolver;

    @Override
    public String getType() {
        return "id";
    }

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        // create a new field resolver
        fieldResolver = new MCRImportFieldValueResolver(fields);

        // value
        String id = map.getAttributeValue("value");
        if(id != null)
            id = fieldResolver.resolveFields(id);
        else
            id = fieldResolver.getNotUsedFields().get(0).getValue();

        // resolver
        String resolver = map.getAttributeValue("resolver");
        if(resolver != null && !resolver.equals("")) {
            // try to resolve the uri to get the new value
            id = MCRImportURIResolverMananger.getInstance().resolveURI(resolver, id);
        }
        importObject.setId(id);
    }
}