package org.mycore.pi.exceptions;


import org.mycore.common.MCRCatchException;

public class MCRPersistentIdentifierException extends MCRCatchException {

    public MCRPersistentIdentifierException(String message) {
        super(message);
    }

    public MCRPersistentIdentifierException(String message, Throwable cause) {
        super(message, cause);
    }

}
