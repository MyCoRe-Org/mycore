package org.mycore.webtools.upload.exception;

public class MCRUploadForbiddenException extends MCRUploadException {

    private static final long serialVersionUID = 1L;

    private final String reason;

    public MCRUploadForbiddenException(String reason) {
        super("component.webtools.upload.forbidden", reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
