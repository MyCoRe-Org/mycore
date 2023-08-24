package org.mycore.webtools.upload.exception;

public class MCRUploadForbiddenException extends MCRUploadException {

    private final String reason;

    public MCRUploadForbiddenException(String reason) {
        super("component.webtools.upload.forbidden", reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
