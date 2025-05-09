/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Handles category links to files
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileMetaEventHandler extends MCREventHandlerBase {

    private static final MCRCategLinkService CATEGLINK_SERVICE = MCRCategLinkServiceFactory.obtainInstance();

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        MCRObjectID derivateID = der.getId();
        MCRObjectDerivate objectDerivate = der.getDerivate();
        List<MCRFileMetadata> fileMetadata = objectDerivate.getFileMetadata();
        for (MCRFileMetadata metadata : fileMetadata) {
            Collection<MCRCategoryID> categories = metadata.getCategories();
            if (!categories.isEmpty()) {
                MCRPath path = MCRPath.getPath(derivateID.toString(), metadata.getName());
                MCRCategLinkReference linkReference = new MCRCategLinkReference(path);
                CATEGLINK_SERVICE.setLinks(linkReference, categories);
            }
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        Set<MCRCategLinkReference> before = new HashSet<>(CATEGLINK_SERVICE.getReferences(der.getId().toString()));

        handleDerivateDeleted(evt, der);
        handleDerivateCreated(evt, der);

        Set<MCRCategLinkReference> after = new HashSet<>(CATEGLINK_SERVICE.getReferences(der.getId().toString()));
        Set<MCRCategLinkReference> combined = new HashSet<>(before);
        combined.addAll(after);
        for (MCRCategLinkReference ref : combined) {
            MCRObjectID derId = der.getId();
            String path = ref.getObjectID();
            MCRPath file = MCRPath.getPath(derId.toString(), path);
            BasicFileAttributes attrs;
            try {
                attrs = Files.readAttributes(file, BasicFileAttributes.class);
            } catch (IOException e) {
                LOGGER.warn(() -> "File is linked to category but cannot be read:" + der.getId() + ref.getObjectID(),
                    e);
                continue;
            }
            MCREvent fileEvent = new MCREvent(MCREvent.ObjectType.PATH, MCREvent.EventType.INDEX);
            fileEvent.put(MCREvent.PATH_KEY, file);
            fileEvent.put(MCREvent.FILEATTR_KEY, attrs);
            MCREventManager.getInstance().handleEvent(fileEvent);
        }
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        Collection<MCRCategLinkReference> references = CATEGLINK_SERVICE.getReferences(der.getId().toString());
        CATEGLINK_SERVICE.deleteLinks(references);
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        if (attrs != null && attrs.isDirectory()) {
            return;
        }
        MCRPath mcrPath = MCRPath.ofPath(path);
        MCRObjectID derivateID = MCRObjectID.getInstance(mcrPath.getOwner());
        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.warn("Derivate {} from file '{}' does not exist.", derivateID, path);
            return;
        }
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        MCRObjectDerivate objectDerivate = derivate.getDerivate();
        String filePath = '/' + path.subpath(0, path.getNameCount()).toString();
        Optional<MCRFileMetadata> fileMetaWithoutURN = objectDerivate.getFileMetadata()
            .stream()
            .filter(m -> filePath.equals(m.getName()))
            .filter(m -> m.getUrn() == null && m.getHandle() == null)
            .findAny();

        if (fileMetaWithoutURN.isPresent() && objectDerivate.deleteFileMetaData(filePath)) {
            try {
                MCRMetadataManager.update(derivate);
            } catch (MCRPersistenceException | MCRAccessException e) {
                throw new MCRPersistenceException("Could not update derivate: " + derivateID, e);
            }
        }
    }

}
