package org.mycore.iiif.image.impl;

public class MCRIIIFImageNotFoundException extends Exception {

    private String identifier;

    public MCRIIIFImageNotFoundException(String identifier) {
        super("Invalid image-identifier " + identifier + "!");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
