package org.mycore.sword.application;

public class MCRSwordLifecycleConfiguration {

    private String collection;

    public MCRSwordLifecycleConfiguration(String collection) {
        this.collection = collection;
    }

    public String getCollection() {
        return collection;
    }
}
