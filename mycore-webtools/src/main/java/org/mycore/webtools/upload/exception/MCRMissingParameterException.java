package org.mycore.webtools.upload.exception;

/**
 * Should be thrown if a parameter required by an upload handler is missing.
 */
public class MCRMissingParameterException extends MCRUploadException {

    private static final long serialVersionUID = 1L;

    private final String parameterName;

    public MCRMissingParameterException(String parameterName) {
        super("component.webtools.upload.invalid.parameter.missing", parameterName);
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }
}
