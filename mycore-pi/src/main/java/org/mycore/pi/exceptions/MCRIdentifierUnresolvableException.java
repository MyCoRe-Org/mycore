package org.mycore.pi.exceptions;

public class MCRIdentifierUnresolvableException extends MCRDatacenterException {

    private final String identifier;

    public MCRIdentifierUnresolvableException(String identifier, String message) {
        super(message);
        this.identifier = identifier;
    }

    public MCRIdentifierUnresolvableException(String identifier, String message, Throwable cause) {
        super(message, cause);
        this.identifier = identifier;
    }
}
