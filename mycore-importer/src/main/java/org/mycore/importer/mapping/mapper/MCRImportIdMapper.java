package org.mycore.importer.mapping.mapper;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;

/**
 * This class is used to set the id of an mcrobject.
 * 
 * @author Matthias Eichner
 */
public class MCRImportIdMapper extends MCRImportAbstractMapper {

    private static final Logger LOGGER = Logger.getLogger(MCRImportIdMapper.class);
    
    protected MCRImportFieldValueResolver fieldResolver;

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
        else if(fieldResolver.getNotUsedFields().size() > 0)
            id = fieldResolver.getNotUsedFields().get(0).getValue();
        else {
            LOGGER.error("Id mapping failed for " + record);
            return;
        }
        // resolver
        String uri = map.getAttributeValue("resolver");
        if(uri != null && !uri.equals("")) {
            // maybe in the uri are some field values -> do a field resolve
            String resolvedUri = fieldResolver.resolveFields(uri);
            // try to resolve the uri to get the new value
            id = MCRImportMappingManager.getInstance().getURIResolverManager().resolveURI(resolvedUri, id);
        }
        importObject.setId(id);
    }
}