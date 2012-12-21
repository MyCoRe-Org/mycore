package org.mycore.importer;

import java.util.ArrayList;
import java.util.List;

/**
 * A record is the simplified convert abstraction of an mycore object.
 * It only has a name (something like 'person', 'document' ...)
 * and a list of fields. Each field contains a single information.
 * For example the id, the firstname, date of birth and so on.
 * A field is identified by its id.
 * 
 * @author Matthias Eichner
 */
public class MCRImportRecord {
    private String name;
    private List<MCRImportField> fields;

    public MCRImportRecord(String name) {
        this.name = name;
        fields = new ArrayList<MCRImportField>();
    }

    /**
     * Returns the name of the record.
     * 
     * @return the name of the record.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Adds a new field to this record.
     * 
     * @param field the new field
     */
    public void addField(MCRImportField field) {
        fields.add(field);
    }

    /**
     * Adds a list of fields to the record.
     * 
     * @param newFields the new list of fields
     */
    public void addFields(List<MCRImportField> newFields) {
        fields.addAll(newFields);
    }

    /**
     * Removes a field from the record.
     * 
     * @param field the field to remove
     */
    public void removeField(MCRImportField field) {
        fields.remove(field);
    }

    /**
     * Returns all fields in a list of this record.
     * 
     * @return a list of fields
     */
    public List<MCRImportField> getFields() {
        return fields;
    }

    /**
     * Returns the first field from the field list selected
     * by the given field id. If no field is found, null is
     * returned.
     * 
     * @param fieldId identifier of the field
     * @return first <code>MCRImportField<code> or null if nothing found
     */
    public MCRImportField getFieldById(String fieldId) {
        List<MCRImportField> fieldList = getFieldsById(fieldId);
        if(fieldList.isEmpty())
            return null;
        return fieldList.get(0);
    }

    /**
     * Returns the value of the first field from the field list
     * selected by the field id. If no field is found, null is
     * returned.
     * 
     * @param fieldId identifier of the field
     * @return value of the field or null if nothing found
     */
    public String getFieldValue(String fieldId) {
        MCRImportField field = getFieldById(fieldId);
        if(field != null)
            return field.getValue();
        return null;
    }

    /**
     * Returns a list of fields from the field list selected
     * by the given field id. If no field with the
     * given id was found, a empty list is returned.
     * 
     * @param fieldId identifier of the field
     * @return a list of fields which have the fieldId
     */
    public List<MCRImportField> getFieldsById(String fieldId) {
        List<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        for(MCRImportField field : fields) {
            List<MCRImportField> matchedFields = field.getFields(fieldId);
            if(!matchedFields.isEmpty())
                fieldList.addAll(matchedFields);
        }
        return fieldList;
    }

    @Override
    public String toString() {
        StringBuilder sBuf = new StringBuilder();
        sBuf.append(name).append(": (");
        for(MCRImportField f : fields)
            sBuf.append(f.toString()).append("; ");
        return sBuf.append(")").toString();
    }
}