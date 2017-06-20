package org.mycore.mets.model.simple;

import java.util.UUID;

public class MCRMetsFile {

    public MCRMetsFile() {
    }

    public MCRMetsFile(String href, String mimeType, MCRMetsFileUse use) {
        this.href = href;
        this.mimeType = mimeType;
        this.use = use;
    }

    private String id = UUID.randomUUID().toString();

    private String href;

    private String mimeType;

    private MCRMetsFileUse use;

    public String getId() {
        return id;
    }

    public MCRMetsFileUse getUse() {
        return use;
    }

    public void setUse(MCRMetsFileUse use) {
        this.use = use;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
