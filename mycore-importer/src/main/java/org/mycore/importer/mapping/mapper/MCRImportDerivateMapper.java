package org.mycore.importer.mapping.mapper;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.derivate.MCRImportDerivate;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;


/**
 * The import derivate mapper has a different behavior then other metadata
 * mappers. It doesnt map values or attributes to the current mcrobject.
 * The task is to read the derivateId, find the associated derivate and
 * add this object id to the linked object id list.
 * 
 * @author Matthias Eichner
 */
public class MCRImportDerivateMapper extends MCRImportAbstractMapper {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDerivateMapper.class);
    
    public String getType() {
        return "derivate";
    }

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);

        // get the correct value from the derivateId-attribute
        String derivateId = map.getAttributeValue("derivateId");

        MCRImportFieldValueResolver fieldValueResolver = new MCRImportFieldValueResolver(getFields());
        derivateId = fieldValueResolver.resolveFields(derivateId);

        if(derivateId == null || derivateId.equals("")) {
            return;
        }

        // find the derivate
        List<MCRImportDerivate> derivateList = MCRImportMappingManager.getInstance().getDerivateList();
        MCRImportDerivate derivate = getDerivateById(derivateId, derivateList);

        // add the current object to the linkmeta list
        derivate.addLinkedObjectId(importObject.getId());
    }

    private MCRImportDerivate getDerivateById(String id, List<MCRImportDerivate> derivateList) {
        for(MCRImportDerivate derivate : derivateList) {
            if(derivate.getDerivateId().equals(id))
                return derivate;
        }
        return null;
    }

}