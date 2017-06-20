package org.mycore.pi.exceptions;

public class MCRDatacenterException extends MCRPersistentIdentifierException {

    public MCRDatacenterException(String message) {
        super(message);
    }

    public MCRDatacenterException(String message, Throwable cause) {
        super(message, cause);
    }
}
