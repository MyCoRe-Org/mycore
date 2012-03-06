package org.mycore.access;

import org.mycore.common.MCRCatchException;

public class MCRAccessException extends MCRCatchException {

    public MCRAccessException(String message) {
        super(message);
    }

    public MCRAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}
