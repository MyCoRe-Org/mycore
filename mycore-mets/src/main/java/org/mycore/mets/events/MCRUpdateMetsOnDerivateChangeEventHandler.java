package org.mycore.mets.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.tools.MCRMetsSave;

/**
 * EventHandler updates the mets.xml after a file is added to an existing
 * derivate.
 * 
 * @author shermann
 */
public class MCRUpdateMetsOnDerivateChangeEventHandler extends MCREventHandlerBase {
    private static final Logger LOGGER = Logger.getLogger(MCRUpdateMetsOnDerivateChangeEventHandler.class);

    private String mets = MCRMetsSave.getMetsFileName();

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleFileDeleted(org.mycore.common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!(file instanceof MCRPath)) {
            return;
        }
        // do nothing if mets.xml itself is deleted
        Path fileName = file.getFileName();
        if (fileName != null && fileName.toString().equals(mets)) {
            return;
        }
        MCRPath mcrPath = MCRPath.toMCRPath(file);
        if (Files.notExists(MCRPath.getPath(mcrPath.getOwner(), '/' + mets))) {
            return;
        }
        try {
            MCRMetsSave.updateMetsOnFileDelete(mcrPath);
        } catch (Exception e) {
            LOGGER.error("Error while updating mets file", e);
        }
    }

    @Override
    protected void handlePathCreated(MCREvent evt, Path file, BasicFileAttributes attrs) {
        if (!(file instanceof MCRPath)) {
            return;
        }
        // do nothing if mets.xml itself is deleted
        if (file.getFileName().toString().equals(mets)) {
            return;
        }
        MCRPath mcrPath = MCRPath.toMCRPath(file);
        if (Files.notExists(MCRPath.getPath(mcrPath.getOwner(), '/' + mets))) {
            return;
        }

        try {
            MCRMetsSave.updateMetsOnFileAdd(mcrPath);
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
