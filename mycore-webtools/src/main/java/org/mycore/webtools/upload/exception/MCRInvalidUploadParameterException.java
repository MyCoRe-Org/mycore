package org.mycore.webtools.upload.exception;

/**
 * Should be thrown if a parameter required by an upload handler is not valid. E.g. a classification does not exist.
 */
public class MCRInvalidUploadParameterException extends MCRUploadException {

    private final String parameterName;

    private final String wrongReason;

    private final String badValue;

    public MCRInvalidUploadParameterException(String parameterName, String badValue, String wrongReason) {
        super("component.webtools.upload.invalid.parameter", parameterName, badValue , wrongReason);
        this.parameterName = parameterName;
        this.wrongReason = wrongReason;
        this.badValue = badValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getWrongReason() {
        return wrongReason;
    }

    public String getBadValue() {
        return badValue;
    }
}
