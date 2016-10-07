package org.mycore.iiif.image.impl;

public class MCRIIIFUnsupportedFormatException extends Exception {

    public MCRIIIFUnsupportedFormatException(String unsupportedFormat) {
        super("The format " + unsupportedFormat + " is not supported!");
    }
}
