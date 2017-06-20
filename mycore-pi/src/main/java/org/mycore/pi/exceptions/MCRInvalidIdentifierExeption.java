package org.mycore.pi.exceptions;

import java.util.Locale;

public class MCRInvalidIdentifierExeption extends MCRPersistentIdentifierException {
    public MCRInvalidIdentifierExeption(String idString, String type) {
        super(String.format(Locale.ENGLISH, "%s is not a valid %s!", idString, type));
    }
}
