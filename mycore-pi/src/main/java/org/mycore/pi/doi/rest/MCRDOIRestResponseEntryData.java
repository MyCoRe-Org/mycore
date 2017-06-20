package org.mycore.pi.doi.rest;

public class MCRDOIRestResponseEntryData {
    public MCRDOIRestResponseEntryData(String format, MCRDOIRestResponseEntryDataValue value) {
        this.format = format;
        this.value = value;
    }

    String format;

    MCRDOIRestResponseEntryDataValue value;

    public MCRDOIRestResponseEntryDataValue getValue() {
        return value;
    }

    public String getFormat() {
        return format;
    }
}
