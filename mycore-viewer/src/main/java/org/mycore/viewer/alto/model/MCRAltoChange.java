package org.mycore.viewer.alto.model;

public class MCRAltoChange {
    public MCRAltoChange(String file, String type) {
        this.file = file;
        this.type = type;
    }

    public MCRAltoChange() {
    }

    private String file;
    private String type;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
