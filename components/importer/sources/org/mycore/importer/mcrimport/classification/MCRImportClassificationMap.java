package org.mycore.importer.mcrimport.classification;

import java.util.Hashtable;

public class MCRImportClassificationMap {

    protected String id;
    protected Hashtable<String, String> table;

    public MCRImportClassificationMap(String id) {
        this.id = id;
        this.table = new Hashtable<String, String>();
    }
    
    public void addPair(String importValue, String mycoreValue) {
        table.put(importValue, mycoreValue);
    }
    
    public void removePair(String importValue) {
        table.remove(importValue);
    }

    public String getMyCoReValue(String importValue) {
        return table.get(importValue);
    }
    
    public Hashtable<String, String> getTable() {
        return table;
    }
    public String getId() {
        return id;
    }
}