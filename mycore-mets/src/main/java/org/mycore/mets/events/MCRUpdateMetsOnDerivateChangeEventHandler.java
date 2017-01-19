package org.mycore.mets.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.tools.MCRMetsSave;

/**
 * EventHandler updates the mets.xml after a file is added to an existing
 * derivate.
 * 
 * @author shermann
 */
public class MCRUpdateMetsOnDerivateChangeEventHandler extends MCREventHandlerBase {
    private static final Logger LOGGER = LogManager.getLogger(MCRUpdateMetsOnDerivateChangeEventHandler.class);

    private String mets = MCRMetsSave.getMetsFileName();

    /**
     * Checks if the mets.xml should be updated.
     * 
     * @param evt the mcr event
     * @param file the file which was changed
     * @param attrs the file attributes
     * @return true if the mets shoud be updated, otherwise false
     */
    protected boolean checkUpdateMets(MCREvent evt, Path file, BasicFileAttributes attrs) {
        // don't update if no MCRPath
        if (!(file instanceof MCRPath)) {
            return false;
        }
        // don't update if mets.xml is deleted
        Path fileName = file.getFileName();
        if (fileName != null && fileName.toString().equals(mets)) {
            return false;
        }
        MCRPath mcrPath = MCRPath.toMCRPath(file);
        String derivateId = mcrPath.getOwner();
        // don't update if mets.xml does not exist
        if (Files.notExists(MCRPath.getPath(derivateId, '/' + mets))) {
            return false;
        }
        // don't update if derivate or mycore object is marked for deletion
        MCRObjectID mcrDerivateId = MCRObjectID.getInstance(derivateId);
        if (isMarkedForDeletion(MCRMetadataManager.retrieveMCRDerivate(mcrDerivateId))) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the derivate or the corresponding mycore object is
     * marked for deletion.
     * 
     * @return true if one of them is marked for deletion
     */
    protected boolean isMarkedForDeletion(MCRDerivate derivate) {
        MCRMarkManager markManager = MCRMarkManager.instance();
        if (markManager.isMarkedForDeletion(derivate.getId()) ||
            markManager.isMarkedForDeletion(derivate.getOwnerID())) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleFileDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!checkUpdateMets(evt, file, attrs)) {
            return;
        }
        MCRPath mcrPath = MCRPath.toMCRPath(file);
        try {
            MCRMetsSave.updateMetsOnFileDelete(mcrPath);
        } catch (Exception e) {
            LOGGER.error("Error while updating mets file", e);
        }
    }

    @Override
    protected void handlePathCreated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!checkUpdateMets(evt, file, attrs)) {
            return;
        }
        MCRPath mcrPath = MCRPath.toMCRPath(file);
        try {
            MCRMetsSave.updateMetsOnFileAdd(mcrPath);
        } catch (Exception e) {
            LOGGER.error("Error while updating mets file", e);
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        if (isMarkedForDeletion(der)) {
            return;
        }
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
