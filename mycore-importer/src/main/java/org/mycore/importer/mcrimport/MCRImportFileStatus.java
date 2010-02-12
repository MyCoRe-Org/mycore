package org.mycore.importer.mcrimport;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This is a data holder class for the linkage between import- and
 * mycore objects. It holds information about the import id, the
 * mycore id, the type (MCRObject or MCRDerivate) and a path to the
 * import xml file.
 *
 * @author Matthias Eichner
 */
public class MCRImportFileStatus {

    private String importId;
    private MCRObjectID mycoreId;
    private boolean savedInTempDirectory;
    
    private MCRImportFileType type;

    private String importObjectPath;

    public MCRImportFileStatus(String importId, String importObjectPath, MCRImportFileType type) {
        this.importId = importId;
        this.importObjectPath = importObjectPath;
        this.type = type;
        this.mycoreId = null;
    }

    public void setMycoreId(MCRObjectID mycoreId) {
        this.mycoreId = mycoreId;
    }
    public String getImportObjectPath() {
        return importObjectPath;
    }
    public String getImportId() {
        return importId;
    }
    public MCRObjectID getMycoreId() {
        return mycoreId;
    }
    public MCRImportFileType getType() {
        return type;
    }
    public void setSavedInTempDirectory(boolean savedInTempDirectory) {
        this.savedInTempDirectory = savedInTempDirectory;
    }
    public boolean isSavedInTempDirectory() {
        return savedInTempDirectory;
    }
}