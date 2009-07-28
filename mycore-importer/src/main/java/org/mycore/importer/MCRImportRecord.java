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
     * Returns all fields in a list of this record.
     * 
     * @return a list of fields
     */
    public List<MCRImportField> getFields() {
        return fields;
    }

    /**
     * Returns a field from the field list selected
     * by the given field id. If no field with the
     * given id was found, null will be returned.
     * 
     * @param fieldId the field id which identifies the field
     * @return a field from the field list.
     */
    public MCRImportField getFieldById(String fieldId) {
        for(MCRImportField field : fields) {
            if(field.getId().equals(fieldId))
                return field;
        }
        return null;
    }

    @Override
    public String toString() {
        String returnString = name + ": ";
        for(MCRImportField f : fields)
            returnString += f.toString() + ";";
        return returnString;
    }
}