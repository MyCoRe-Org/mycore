package org.mycore.importer;

/**
 * A field is the latest information unit for the convert process.
 * It is simply described by a value and an id.
 * 
 * @author Matthias Eichner
 */
public class MCRImportField {

    private String id;
    private String value;

    public MCRImportField(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "(" + id + ": " + value + ")";
    }
}