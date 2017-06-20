package org.mycore.pi.doi.rest;

public class MCRDOIRestResponseEntryDataStringValue extends MCRDOIRestResponseEntryDataValue {
    public MCRDOIRestResponseEntryDataStringValue(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

}
