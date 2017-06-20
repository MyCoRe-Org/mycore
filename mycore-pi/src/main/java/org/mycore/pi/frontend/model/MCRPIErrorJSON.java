package org.mycore.pi.frontend.model;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPIErrorJSON {

    public String message;

    public String stackTrace;

    public String translatedAdditionalInformation;

    public String code;

    public MCRPIErrorJSON(String message) {
        this(message, null);
    }

    public MCRPIErrorJSON(String message, Exception e) {
        this.message = message;

        if (e instanceof MCRPersistentIdentifierException) {
            MCRPersistentIdentifierException identifierException = (MCRPersistentIdentifierException) e;
            identifierException.getCode().ifPresent(code -> this.code = Integer.toHexString(code));
            identifierException.getTranslatedAdditionalInformation()
                .ifPresent(msg -> this.translatedAdditionalInformation = msg);
        }

        if (e != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            stackTrace = sw.toString();
        }
    }
}
