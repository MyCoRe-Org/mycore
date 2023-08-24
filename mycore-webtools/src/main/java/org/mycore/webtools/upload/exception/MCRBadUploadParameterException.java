package org.mycore.webtools.upload.exception;

public class MCRBadUploadParameterException extends MCRUploadException {

    private final String parameterName;

    private final String wrongReason;

    private final String badValue;

    public MCRBadUploadParameterException(String parameterName, String badValue, String wrongReason) {
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
