/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 7, 2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.metadata;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Handles category links to files
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileMetaEventHandler extends MCREventHandlerBase {
    private static MCRCategLinkService CATEGLINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    private static Logger LOGGER = Logger.getLogger(MCRFileMetaEventHandler.class);

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        MCRObjectID derivateID = der.getId();
        MCRObjectDerivate objectDerivate = der.getDerivate();
        List<MCRFileMetadata> fileMetadata = objectDerivate.getFileMetadata();
        for (MCRFileMetadata metadata : fileMetadata) {
            Collection<MCRCategoryID> categories = metadata.getCategories();
            if (!categories.isEmpty()) {
                MCRCategLinkReference linkReference = MCRFile.getCategLinkReference(derivateID, metadata.getName());
                CATEGLINK_SERVICE.setLinks(linkReference, categories);
            }
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        HashSet<MCRCategLinkReference> before = new HashSet<MCRCategLinkReference>();
        before.addAll(CATEGLINK_SERVICE.getReferences(der.getId().toString()));

        handleDerivateDeleted(evt, der);
        handleDerivateCreated(evt, der);

        HashSet<MCRCategLinkReference> after = new HashSet<MCRCategLinkReference>();
        after.addAll(CATEGLINK_SERVICE.getReferences(der.getId().toString()));
        HashSet<MCRCategLinkReference> combined = new HashSet<MCRCategLinkReference>(before);
        combined.addAll(after);
        for (MCRCategLinkReference ref : combined) {
            MCRFile file = MCRFile.getMCRFile(der.getId(), ref.getObjectID());
            if (file == null) {
                LOGGER.warn("File is linked to category but does not exist:" + der.getId() + ref.getObjectID());
                continue;
            }
            MCREvent fileEvent = new MCREvent(MCREvent.FILE_TYPE, MCREvent.INDEX_EVENT);
            fileEvent.put("file", file);
            MCREventManager.instance().handleEvent(fileEvent);
        }
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        Collection<MCRCategLinkReference> references = CATEGLINK_SERVICE.getReferences(der.getId().toString());
        CATEGLINK_SERVICE.deleteLinks(references);
    }

    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        MCRObjectID derivateID = MCRObjectID.getInstance(file.getOwnerID());
        if (!MCRMetadataManager.exists(derivateID)) {
            LOGGER.warn("Derivate " + derivateID + " from file '" + file + "' does not exist.");
            return;
        }
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        MCRObjectDerivate objectDerivate = derivate.getDerivate();
        if (objectDerivate.deleteFileMetaData(file.getAbsolutePath())) {
            MCRMetadataManager.update(derivate);
        }
    }

}
