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

package org.mycore.datamodel.metadata;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * This eventhandler deals with changes to the maindoc file.
 * 
 * It sets the maindoc entry in derivate metadata empty,
 * if the maindoc was deleted
 * 
 * @author Robert Stephan
 *
 */
public class MCRMaindocEventHandler extends MCREventHandlerBase {
    private static Logger LOGGER = LogManager.getLogger(MCRMaindocEventHandler.class);

    @Override
    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        if (attrs != null && attrs.isDirectory()) {
            return;
        }
        MCRObjectID derivateID = MCRObjectID.getInstance(MCRPath.toMCRPath(path).getOwner());
        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.warn("Derivate {} from file '{}' does not exist.", derivateID, path);
            return;
        }
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        MCRObjectDerivate objectDerivate = derivate.getDerivate();
        String filePath = path.subpath(0, path.getNameCount()).toString();
        boolean wasMainDocDeleted = filePath.equals(objectDerivate.getInternals().getMainDoc());
        if (wasMainDocDeleted) {
            objectDerivate.getInternals().setMainDoc("");
            try {
                MCRMetadataManager.update(derivate);
            } catch (MCRPersistenceException | MCRAccessException e) {
                throw new MCRPersistenceException("Could not update derivate: " + derivateID, e);
            }
            LOGGER.warn("The maindoc '{}' was deleted.", path);
        }
    }

}
