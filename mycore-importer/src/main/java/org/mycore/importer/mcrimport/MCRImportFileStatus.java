package org.mycore.importer.mcrimport;

public class MCRImportFileStatus {

    private String importId;
    private String mycoreId;
    
    private MCRImportFileType type;

    private String filePath;

    public MCRImportFileStatus(String importId, String filePath, MCRImportFileType type) {
        this.importId = importId;
        this.filePath = filePath;
        this.type = type;
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
    public MCRImportFileType getType() {
        return type;
    }

    public boolean isImported() {
        if(mycoreId != null && !mycoreId.equals(""))
            return true;
        return false;
    }
}