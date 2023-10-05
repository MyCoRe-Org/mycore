package org.mycore.webtools.upload.exception;

/**
 * Should be thrown if the file is not valid. E.g. the size is too big or the file name contains invalid characters.
 */
public class MCRInvalidFileException extends MCRUploadException {

    private static final long serialVersionUID = 1L;

    private final String fileName;

    private final String reason;

    public MCRInvalidFileException(String fileName, String reason) {
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
