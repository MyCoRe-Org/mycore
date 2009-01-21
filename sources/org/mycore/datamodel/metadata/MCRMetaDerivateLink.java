package org.mycore.datamodel.metadata;

import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;

public class MCRMetaDerivateLink extends MCRMetaLink {

    public void setLinkToFile(MCRFile file) {
        String owner = file.getOwnerID();
        String path = file.getAbsolutePath();
        super.href = owner + path;
    }

    public MCRFile getLinkedFile() {
        int index = super.href.indexOf('/');
        if (index < 0)
            return null;
        String owner = super.href.substring(0, index);
        String path = super.href.substring(index);
        return (MCRFile) ((MCRDirectory) MCRFile.getRootNode(owner)).getChildByPath(path);
    }

    @Override
    public boolean isValid() {
        if (getLinkedFile() == null) {
            LOGGER.error("File not found: " + super.href);
            return false;
        }
        return super.isValid();
    }

}
