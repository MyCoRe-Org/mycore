package org.mycore.importer.mapping.mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.importer.MCRImportField;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel1;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel2;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;

/**
 * This class provides default implementations for the <code>MCRImportMapper</code>
 * interface. It supports field parsing and dynamic creation of
 * <code>MCRImportMetadataResolver</code> instances.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRImportAbstractMapper implements MCRImportMapper {

    private static final Logger LOGGER = Logger.getLogger(MCRImportAbstractMapper.class);

    protected MCRImportObject importObject;
    protected Element map;
    protected MCRImportRecord record;
    protected List<MCRImportField> fields;

    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        this.importObject = importObject;
        this.record = record;
        this.map = map;
        this.fields = getFields();
    }

    /**
     * Parses the field attribute of the jdom map and add
     * each field to a list.
     * 
     * @return the generated field list
     */
    protected List<MCRImportField> parseFields() {
        ArrayList<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        String fieldsValue = map.getAttributeValue("fields");
        if(fieldsValue == null)
            return null;
        fieldsValue = fieldsValue.replace(" ", "");
        String[] fieldArray = fieldsValue.split(",");
        for(String fieldId : fieldArray) {
            MCRImportField field = record.getFieldById(fieldId);
            if(field != null)
                fieldList.add(field);
        }
        return fieldList;
    }

    /**
     * Returns all fields as list which are parsed from the
     * jdom map element.
     * 
     * @return a list of all fields of the map 
     */
    public List<MCRImportField> getFields() {
        if(fields == null)
            fields = parseFields();
        return fields;
    }

    /**
     * Returns a field from the field list specified by
     * the id.
     * 
     * @param fieldId the id of the field
     * @return a field from the parsed field list
     */
    public MCRImportField getFieldById(String fieldId) {
        for(MCRImportField field : getFields()) {
            if(fieldId.equals(field.getId()))
                return field;
        }
        return null;
    }

    /**
     * Creates an instance of <code>MCRImportMetadataResolver</code>. 
     * 
     * @return a new instance of <code>MCRImportMetadataResolver</code>
     */
    protected MCRImportMetadataResolver createResolverInstance() {       
        // get the type of the metadata element from the datamodel
        String type = null;
        MCRImportDatamodel dm = importObject.getDatamodel();
        String metadataName = map.getAttributeValue("to");
        if(metadataName == null || metadataName.equals("")) {
            LOGGER.error("No 'to'-attribute set in record " + record.getName() + " for fields " + map.getAttributeValue("fields"));
            return null;
        }

        // get the metadata resolver manager
        MCRImportMetadataResolverManager mrm = MCRImportMappingManager.getInstance().getMetadataResolverManager();

        if(dm instanceof MCRImportDatamodel1) {
            String className = ((MCRImportDatamodel1)dm).getClassName(metadataName);
            type = mrm.getTypeByClassName(className);
        } else if(dm instanceof MCRImportDatamodel2) {
            type = ((MCRImportDatamodel2)dm).getType(metadataName);
        }

        if(type == null) {
            LOGGER.error("Couldnt resolve metadata type for " + metadataName + " in datamodel " + dm.getPath());
            return null;
        }

        // create a new instance of the resolver 
        return mrm.createInstance(type);
    }
}