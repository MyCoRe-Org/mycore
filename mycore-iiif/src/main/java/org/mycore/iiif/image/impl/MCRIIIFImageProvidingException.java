package org.mycore.iiif.image.impl;

public class MCRIIIFImageProvidingException extends Exception {
    public MCRIIIFImageProvidingException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MCRIIIFImageProvidingException(Throwable cause) {
        super(cause);
    }

    public MCRIIIFImageProvidingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MCRIIIFImageProvidingException(String message) {
        super(message);
    }

    public MCRIIIFImageProvidingException() {
    }
}
