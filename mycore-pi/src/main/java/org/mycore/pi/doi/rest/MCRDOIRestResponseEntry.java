package org.mycore.pi.doi.rest;

public class MCRDOIRestResponseEntry {
    int index;

    String type;

    MCRDOIRestResponseEntryData data;

    int ttl;

    String timestamp;

    public int getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public MCRDOIRestResponseEntryData getData() {
        return data;
    }

    public int getTtl() {
        return ttl;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
