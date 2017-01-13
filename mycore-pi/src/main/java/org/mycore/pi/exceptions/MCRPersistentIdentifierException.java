package org.mycore.pi.exceptions;

import java.util.Optional;

import org.mycore.common.MCRCatchException;

public class MCRPersistentIdentifierException extends MCRCatchException {

    private final String translatedAdditionalInformation;

    private final Integer code;

    public MCRPersistentIdentifierException(String message) {
        super(message);
        translatedAdditionalInformation = null;
        code = null;
    }

    public MCRPersistentIdentifierException(String message, Throwable cause) {
        super(message, cause);
        translatedAdditionalInformation = null;
        code = null;
    }

    public MCRPersistentIdentifierException(String message, String translatedAdditionalInformation, int code) {
        this(message, translatedAdditionalInformation, code, null);
    }

    public MCRPersistentIdentifierException(String message, String translatedAdditionalInformation, int code,
        Exception cause) {
        super(message, cause);

        this.translatedAdditionalInformation = translatedAdditionalInformation;
        this.code = code;
    }

    public Optional<String> getTranslatedAdditionalInformation() {
        return Optional.ofNullable(translatedAdditionalInformation);
    }

    public Optional<Integer> getCode() {
        return Optional.ofNullable(code);
    }
}
