/*
 * $Id$
 * $Revision: 5697 $ $Date: Oct 28, 2013 $
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

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.share.MCRMetadataShareAgent;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSMetadataShareAgent implements MCRMetadataShareAgent {
    private static Logger LOGGER = LogManager.getLogger(MCRMODSMetadataShareAgent.class);

    private static final String HOST_SECTION_XPATH = "mods:relatedItem[@type='host']";

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent#inheritableMetadataChanged(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public boolean shareableMetadataChanged(MCRObject oldVersion, MCRObject newVersion) {
        final MCRObjectMetadata md = newVersion.getMetadata();
        final MCRObjectMetadata mdold = oldVersion.getMetadata();
        //if any metadata changed we need to update children
        boolean metadataChanged = !MCRXMLHelper.deepEqual(md.createXML(), mdold.createXML());
        if (!metadataChanged) {
            LOGGER.info("Metadata did not change on update of " + newVersion.getId());
        }
        return metadataChanged;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent#inheritMetadata(org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void distributeMetadata(MCRObject holder) throws MCRPersistenceException, MCRAccessException {
        MCRMODSWrapper holderWrapper = new MCRMODSWrapper(holder);
        List<MCRMetaLinkID> children = holder.getStructure().getChildren();
        if (!children.isEmpty()) {
            LOGGER.info("Update inherited metadata");
            for (MCRMetaLinkID childIdRef : children) {
                MCRObjectID childId = childIdRef.getXLinkHrefID();
                if (MCRMODSWrapper.isSupported(childId)) {
                    LOGGER.info("Update: " + childIdRef);
                    MCRObject child = MCRMetadataManager.retrieveMCRObject(childId);
                    MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
                    inheritToChild(holderWrapper, childWrapper);
                    LOGGER.info("Saving: " + childIdRef);
                    try {
                        MCRMetadataManager.update(child);
                    } catch (MCRActiveLinkException e) {
                        throw new MCRPersistenceException("Error while updating inherited metadata", e);
                    }
                }
            }
        }
        Collection<String> recipientIds = MCRLinkTableManager.instance().getSourceOf(holder.getId(),
            MCRLinkTableManager.ENTRY_TYPE_REFERENCE);
        for (String rId : recipientIds) {
            MCRObjectID recipientId = MCRObjectID.getInstance(rId);
            if (MCRMODSWrapper.isSupported(recipientId)) {
                LOGGER.info("distribute metadata to " + rId);
                MCRObject recipient = MCRMetadataManager.retrieveMCRObject(recipientId);
                MCRMODSWrapper recipientWrapper = new MCRMODSWrapper(recipient);
                for (Element relatedItem : recipientWrapper.getLinkedRelatedItems()) {
                    String holderId = relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                    if (holder.getId().toString().equals(holderId)) {
                        @SuppressWarnings("unchecked")
                        Filter<Content> sharedMetadata = (Filter<Content>) Filters.element("part",
                            MCRConstants.MODS_NAMESPACE).negate();
                        relatedItem.removeContent(sharedMetadata);
                        relatedItem.addContent(holderWrapper.getMODS().cloneContent());
                        LOGGER.info("Saving: " + recipientId);
                        try {
                            MCRMetadataManager.update(recipient);
                        } catch (MCRActiveLinkException e) {
                            throw new MCRPersistenceException("Error while updating shared metadata", e);
                        }
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent#inheritMetadata(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void receiveMetadata(MCRObject child) {
        MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
        MCRObjectID parentID = child.getStructure().getParentID();
        LOGGER.debug("Removing old inherited Metadata.");
        childWrapper.removeInheritedMetadata();
        if (parentID != null && MCRMODSWrapper.isSupported(parentID)) {
            MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentID);
            MCRMODSWrapper parentWrapper = new MCRMODSWrapper(parent);
            inheritToChild(parentWrapper, childWrapper);
        }
        for (Element relatedItem : childWrapper.getLinkedRelatedItems()) {
            String type = relatedItem.getAttributeValue("type");
            String holderId = relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            LOGGER.info("receive metadata from " + type + " document " + holderId);
            if ((holderId == null || parentID != null && parentID.toString().equals(holderId))
                && MCRMODSRelationshipType.host.name().equals(type)) {
                //already received metadata from parent;
                continue;
            }
            MCRObjectID holderObjectID = MCRObjectID.getInstance(holderId);
            if (MCRMODSWrapper.isSupported(holderObjectID)) {
                MCRObject targetObject = MCRMetadataManager.retrieveMCRObject(holderObjectID);
                MCRMODSWrapper targetWrapper = new MCRMODSWrapper(targetObject);
                relatedItem.addContent(targetWrapper.getMODS().cloneContent());
            }
        }
    }

    private void inheritToChild(MCRMODSWrapper parentWrapper, MCRMODSWrapper childWrapper) {
        LOGGER.info("Inserting inherited Metadata.");
        Element hostContainer = childWrapper.getElement(HOST_SECTION_XPATH);
        if (hostContainer == null) {
            LOGGER.info("Adding new relatedItem[@type='host'])");
            hostContainer = new Element("relatedItem", MCRConstants.MODS_NAMESPACE)
                .setAttribute("href", parentWrapper.getMCRObject().getId().toString(), MCRConstants.XLINK_NAMESPACE)
                .setAttribute("type", "host");
            childWrapper.addElement(hostContainer);
        }
        hostContainer.addContent(parentWrapper.getMODS().cloneContent());
    }

}
