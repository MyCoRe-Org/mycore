package org.mycore.importer.mapping.mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.importer.MCRImportField;
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
        if(!MCRImportMappingManager.getInstance().getConfig().isUseDerivates()) {
            LOGGER.warn("Try to map derivates, but use derivates is set to false in configuration.");
            return;
        }
        super.map(importObject, record, map);

        // get the correct value from the derivateId-attribute
        String valueText = map.getAttributeValue("derivateId");

        List<MCRImportDerivate> derivateList = MCRImportMappingManager.getInstance().getDerivateList();

        // go through all fields to support multi derivate linking
        for(MCRImportField field : fields) {
            List<MCRImportField> singleFieldList = new ArrayList<MCRImportField>();
            singleFieldList.add(field);
            MCRImportFieldValueResolver fieldValueResolver = new MCRImportFieldValueResolver(singleFieldList);
            String derivateId = fieldValueResolver.resolveFields(valueText);    

            if(!fieldValueResolver.isCompletelyResolved() || derivateId == null || derivateId.equals("")) {
                LOGGER.debug("Couldnt resolve derivate id " + derivateId);
                return;
            }

            // find the derivate
            MCRImportDerivate derivate = getDerivateById(derivateId, derivateList);

            if(derivate == null) {
                LOGGER.error("Couldnt find derivate id '" + derivateId + "' in the MCRImportDerivate list!" +
                             " Check if you call 'setDerivateList' in the MCRImportMappingManager!");
                return;
            }

            // add the current object to the linkmeta list
            derivate.setLinkedObject(importObject.getId());
            LOGGER.debug("Successfully linked mcr object '" + importObject.getId() +
                         "' to derivate '" + derivate.getDerivateId() + "'!");
        }
    }

    private MCRImportDerivate getDerivateById(String id, List<MCRImportDerivate> derivateList) {
        for(MCRImportDerivate derivate : derivateList) {
            if(derivate.getDerivateId().equals(id))
                return derivate;
        }
        return null;
    }

}