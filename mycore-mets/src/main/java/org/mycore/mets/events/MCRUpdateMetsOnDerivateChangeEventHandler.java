package org.mycore.mets.events;

import java.nio.file.Files;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileEventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.tools.MCRMetsSave;

/**
 * EventHandler updates the mets.xml after a file is added to an existing
 * derivate.
 * 
 * @author shermann
 */
public class MCRUpdateMetsOnDerivateChangeEventHandler extends MCRFileEventHandlerBase {
    private static final Logger LOGGER = Logger.getLogger(MCRUpdateMetsOnDerivateChangeEventHandler.class);
    private String mets = MCRMetsSave.getMetsFileName();

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleFileDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        // do nothing if mets.xml itself is deleted
        if (file.getName().equals(mets )) {
            return;
        }

        MCRDirectory rootDir = file.getRootDirectory();
        if (rootDir == null || !rootDir.hasChild(mets )) {
            return;
        }

        try {
            MCRMetsSave.updateMetsOnFileDelete(file);
        } catch (Exception e) {
            LOGGER.error("Error while updating mets file", e);
        }
    }

    @Override
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        // do nothing if mets.xml itself is created
        if (file.getName().equals(mets)) {
            return;
        }

        if (Files.notExists(MCRPath.getPath(file.getOwnerID(), mets))) {
            return;
        }

        try {
            MCRMetsSave.updateMetsOnFileAdd(file);
        } catch (Exception e) {
            LOGGER.error("Error while updating mets file", e);
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        try {
            Map<String, String> urnFileMap = der.getUrnMap();
            if (urnFileMap.size() > 0) {
                MCRMetsSave.updateMetsOnUrnGenerate(der.getId(), urnFileMap);
            } else {
                LOGGER.debug("There are no URN to insert");
            }
        } catch (Exception e) {
            LOGGER.error("Read derivate XML cause error", e);
        }
    }

}
