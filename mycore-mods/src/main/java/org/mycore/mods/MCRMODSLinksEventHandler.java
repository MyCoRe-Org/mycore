/*
 * $Id$
 * $Revision: 5697 $ $Date: 07.09.2011 $
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

package org.mycore.mods;

import java.util.HashSet;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Eventhandler for linking MODS_OBJECTTYPE document to MyCoRe classifications.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSLinksEventHandler extends MCREventHandlerBase {

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(final MCREvent evt, final MCRObject obj) {
        if (!MCRMODSWrapper.isSupported(obj)) {
            return;
        }
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(obj);
        final HashSet<MCRCategoryID> categories = new HashSet<>(modsWrapper.getMcrCategoryIDs());
        if (!categories.isEmpty()) {
            final MCRCategLinkReference objectReference = new MCRCategLinkReference(obj.getId());
            MCRCategLinkServiceFactory.getInstance().setLinks(objectReference, categories);
        }
        List<Element> linkingNodes = modsWrapper.getLinkedRelatedItems();
        if (!linkingNodes.isEmpty()) {
            MCRLinkTableManager linkTableManager = MCRLinkTableManager.instance();
            for (Element linkingNode : linkingNodes) {
                String targetID = linkingNode.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                if (targetID == null) {
                    continue;
                }
                String relationshipTypeRaw = linkingNode.getAttributeValue("type");
                MCRMODSRelationshipType relType = MCRMODSRelationshipType.valueOf(relationshipTypeRaw);
                //MCR-1328 (no reference links for 'host')
                if (relType != MCRMODSRelationshipType.host) {
                    linkTableManager.addReferenceLink(obj.getId(), MCRObjectID.getInstance(targetID),
                        MCRLinkTableManager.ENTRY_TYPE_REFERENCE, relType.toString());
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectUpdated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectUpdated(final MCREvent evt, final MCRObject obj) {
        if (!MCRMODSWrapper.isSupported(obj)) {
            return;
        }
        handleObjectCreated(evt, obj);
        //may have to reindex children, if they inherit any information
        for (MCRMetaLinkID childLinkID : obj.getStructure().getChildren()) {
            MCRObjectID childID = childLinkID.getXLinkHrefID();
            if (MCRMetadataManager.exists(childID)) {
                MCREvent childEvent = new MCREvent(childID.getTypeId(), MCREvent.INDEX_EVENT);
                childEvent.put("object", MCRMetadataManager.retrieve(childID));
                MCREventManager.instance().handleEvent(childEvent);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectRepaired(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectRepaired(final MCREvent evt, final MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

}
