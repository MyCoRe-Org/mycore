package org.mycore.webtools.upload.exception;

public class MCRBadFileException extends MCRUploadException {

    private final String fileName;

    private final String reason;

    public MCRBadFileException(String fileName, String reason) {
        super("component.webtools.upload.invalid.file", fileName, reason);
        this.fileName = fileName;
        this.reason = reason;
    }

    public String getFileName() {
        return fileName;
    }

    public String getReason() {
        return reason;
    }
}
