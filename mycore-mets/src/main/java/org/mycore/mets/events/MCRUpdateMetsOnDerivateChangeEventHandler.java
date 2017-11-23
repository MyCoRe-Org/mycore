/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mets.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

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
        MCRDerivate mcrDerivate = MCRMetadataManager.retrieveMCRDerivate(mcrDerivateId);
        return !MCRMarkManager.instance().isMarkedForDeletion(mcrDerivate);
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
        MCRMetsSave.updateMetsOnUrnGenerate(der);
    }

}
