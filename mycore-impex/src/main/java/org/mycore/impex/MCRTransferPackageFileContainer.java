package org.mycore.impex;

import java.util.ArrayList;
import java.util.List;

import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Container for derivate files.
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public class MCRTransferPackageFileContainer {

    private MCRObjectID derivateId;

    private List<MCRFile> fileList;

    public MCRTransferPackageFileContainer(MCRObjectID derivateId) {
        if (derivateId == null) {
            throw new IllegalArgumentException("The derivate parameter must not be null.");
        }
        this.derivateId = derivateId;
    }

    /**
     * @return the name of this file container
     */
    public String getName() {
        return this.derivateId.toString();
    }

    public MCRObjectID getDerivateId() {
        return derivateId;
    }

    /**
     * @return the list of files hold by this container
     */
    public List<MCRFile> getFiles() {
        if (fileList == null) {
            this.fileList = new ArrayList<MCRFile>();
            this.createFileList();
        }
        return this.fileList;
    }

    private void createFileList() {
        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(this.derivateId.toString());
        if (node instanceof MCRDirectory) {
            processNode(node);
        }
    }

    /**
     * @param node the node to process
     */
    private void processNode(MCRFilesystemNode node) {
        MCRDirectory dir = (MCRDirectory) node;
        MCRFilesystemNode[] children = dir.getChildren();
        for (MCRFilesystemNode child : children) {
            if (child instanceof MCRDirectory) {
                processNode(child);
            }
            if (child instanceof MCRFile) {
                this.fileList.add((MCRFile) child);
            }
        }
    }

    @Override
    public String toString() {
        return this.getName() + " (" + this.fileList.size() + ")";
    }
}
