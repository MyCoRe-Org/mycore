package org.mycore.mets.model.simple;

public class MCRMetsFile {

    private String id;

    private String href;

    private String mimeType;

    private MCRMetsFileUse use;

    public MCRMetsFile() {
    }

    public MCRMetsFile(String id, String href, String mimeType, MCRMetsFileUse use) {
        this.id = id;
        this.href = href;
        this.mimeType = mimeType;
        this.use = use;
    }

    public void setId(String id) {
        this.id = id;
    }

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
