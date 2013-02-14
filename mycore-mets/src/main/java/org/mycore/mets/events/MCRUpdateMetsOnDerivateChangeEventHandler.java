package org.mycore.mets.events;

import java.util.Map;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mets.tools.MCRMetsSave;

/**
 * EventHandler updates the mets.xml after a file is added to an existing
 * derivate.
 * 
 * @author shermann
 */
public class MCRUpdateMetsOnDerivateChangeEventHandler extends MCREventHandlerBase {
    private static final Logger LOGGER = Logger.getLogger(MCRUpdateMetsOnDerivateChangeEventHandler.class);

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleFileDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        String mets = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");
        // do nothing if mets.xml itself is deleted
        if (file.getName().equals(mets)) {
            return;
        }

        MCRDirectory rootDir = file.getRootDirectory();
        if (rootDir == null || !rootDir.hasChild(mets)) {
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
        String mets = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");

        // do nothing if mets.xml itself is deleted
        if (file.getName().equals(mets)) {
            return;
        }

        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwnerID());
        MCRDerivate owner = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        if (!owner.receiveDirectoryFromIFS().hasChild(mets)) {
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
