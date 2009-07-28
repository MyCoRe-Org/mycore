package org.mycore.importer.mcrimport;

public class MCRImportFileStatus {

    private String importId;
    private String mycoreId;

    private String filePath;

    public MCRImportFileStatus(String importId, String filePath) {
        this.importId = importId;
        this.filePath = filePath;
    }

    public void setMycoreId(String mycoreId) {
        this.mycoreId = mycoreId;
    }
    public String getFilePath() {
        return filePath;
    }
    public String getImportId() {
        return importId;
    }
    public String getMycoreId() {
        return mycoreId;
    }

    public boolean isImported() {
        if(mycoreId != null && !mycoreId.equals(""))
            return true;
        return false;
    }
}