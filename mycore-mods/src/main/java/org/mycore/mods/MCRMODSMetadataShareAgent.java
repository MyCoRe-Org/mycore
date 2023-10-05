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

package org.mycore.mods;

import static org.mycore.mods.MCRMODSWrapper.LINKED_RELATED_ITEMS;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
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
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent
     * #inheritableMetadataChanged(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public boolean shareableMetadataChanged(MCRObject oldVersion, MCRObject newVersion) {
        final MCRObjectMetadata md = newVersion.getMetadata();
        final MCRObjectMetadata mdold = oldVersion.getMetadata();
        //if any metadata changed we need to update children
        boolean metadataChanged = !MCRXMLHelper.deepEqual(md.createXML(), mdold.createXML());
        if (!metadataChanged) {
            LOGGER.info("Metadata did not change on update of {}", newVersion.getId());
        }
        return metadataChanged;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent
     * #inheritMetadata(org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void distributeMetadata(MCRObject holder) throws MCRPersistenceException {
        MCRMODSWrapper holderWrapper = new MCRMODSWrapper(holder);
        distributeInheritedMetadata(holderWrapper);
        distributeLinkedMetadata(holderWrapper);
    }

    /**
     * Distribute metadata from holder to all children. The metadata is inherited to all children that are supported
     * by this agent. If {@link #runWithLockedObject(List, Consumer)} is implemented, the children are locked before
     * the metadata is inherited.
     * @param holderWrapper the mods wrapper which holds the metadata that should be inherited
     */
    protected void distributeInheritedMetadata(MCRMODSWrapper holderWrapper) {
        List<MCRMetaLinkID> children = holderWrapper.getMCRObject().getStructure().getChildren();
        if (children.isEmpty()) {
            return;
        }
        LOGGER.info("Update inherited metadata");
        List<MCRObjectID> childIds = children.stream()
            .map(MCRMetaLinkID::getXLinkHrefID)
            .filter(MCRMODSWrapper::isSupported)
            .collect(Collectors.toList());
        runWithLockedObject(childIds, (childId) -> {
            LOGGER.info("Update: {}", childId);
            MCRObject child = MCRMetadataManager.retrieveMCRObject(childId);
            MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
            inheritToChild(holderWrapper, childWrapper);
            LOGGER.info("Saving: {}", childId);
            try {
                checkHierarchy(childWrapper);
                MCRMetadataManager.update(child);
            } catch (MCRPersistenceException | MCRAccessException e) {
                throw new MCRPersistenceException("Error while updating inherited metadata", e);
            }
        });
    }

    /**
     * Distribute metadata from holder to linked objects. The metadata is inherited to all linked objects that are
     * supported by this agent. If {@link #runWithLockedObject(List, Consumer)} is implemented, the linked objects are
     * locked before the metadata is inherited.
     * @param holderWrapper the mods wrapper which holds the metadata that should be inherited
     */
    protected void distributeLinkedMetadata(MCRMODSWrapper holderWrapper) {
        Collection<String> recipientIdsStr = MCRLinkTableManager.instance()
            .getSourceOf(holderWrapper.getMCRObject().getId(), MCRLinkTableManager.ENTRY_TYPE_REFERENCE);
        List<MCRObjectID> recipientIds = recipientIdsStr.stream()
            .map(MCRObjectID::getInstance)
            .filter(MCRMODSWrapper::isSupported).toList();
        if (recipientIds.isEmpty()) {
            return;
        }
        runWithLockedObject(recipientIds, (recipientId) -> {
            LOGGER.info("distribute metadata to {}", recipientId);
            MCRObject recipient = MCRMetadataManager.retrieveMCRObject(recipientId);
            MCRMODSWrapper recipientWrapper = new MCRMODSWrapper(recipient);
            for (Element relatedItem : recipientWrapper.getLinkedRelatedItems()) {
                String holderId = relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                if (holderWrapper.getMCRObject().getId().toString().equals(holderId)) {
                    @SuppressWarnings("unchecked")
                    Filter<Content> sharedMetadata = (Filter<Content>) Filters.element("part",
                        MCRConstants.MODS_NAMESPACE).negate();
                    relatedItem.removeContent(sharedMetadata);
                    List<Content> newRelatedItemContent = getClearContent(holderWrapper);
                    relatedItem.addContent(newRelatedItemContent);
                    LOGGER.info("Saving: {}", recipientId);
                    try {
                        checkHierarchy(recipientWrapper);
                        MCRMetadataManager.update(recipient);
                    } catch (MCRPersistenceException | MCRAccessException e) {
                        throw new MCRPersistenceException("Error while updating shared metadata", e);
                    }
                }
            }
        });
    }

    /**
     * Determines if the content of the relatedItem should be removed, when it is inserted as related item
     * @param relatedItem the relatedItem element
     * @return true if the content should be removed
     */
    protected boolean isClearableRelatedItem(Element relatedItem) {
        return relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE) != null;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent
     * #inheritMetadata(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
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
            LOGGER.info("receive metadata from {} document {}", type, holderId);
            if ((holderId == null || parentID != null && parentID.toString().equals(holderId))
                && MCRMODSRelationshipType.host.name().equals(type)) {
                //already received metadata from parent;
                continue;
            }
            MCRObjectID holderObjectID = MCRObjectID.getInstance(holderId);
            if (MCRMODSWrapper.isSupported(holderObjectID)) {
                MCRObject targetObject = MCRMetadataManager.retrieveMCRObject(holderObjectID);
                MCRMODSWrapper targetWrapper = new MCRMODSWrapper(targetObject);
                List<Content> inheritedData = getClearContent(targetWrapper);
                relatedItem.addContent(inheritedData);
            }
        }
        checkHierarchy(childWrapper);
    }

    /**
     * @param targetWrapper the wrapper of the target object
     * @return a list of content that can be added to a relatedItem element as shared metadata
     */
    private List<Content> getClearContent(MCRMODSWrapper targetWrapper) {
        List<Content> inheritedData = targetWrapper.getMODS().cloneContent();
        inheritedData.stream()
            .filter(c -> c instanceof Element)
            .map(Element.class::cast)
            .filter(element -> element.getName().equals("relatedItem"))
            .filter(this::isClearableRelatedItem)
            .forEach(Element::removeContent);
        return inheritedData;
    }

    void inheritToChild(MCRMODSWrapper parentWrapper, MCRMODSWrapper childWrapper) {
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

    void checkHierarchy(MCRMODSWrapper mods) throws MCRPersistenceException {
        final MCRObjectID modsId = Objects.requireNonNull(mods.getMCRObject().getId());
        LOGGER.info("Checking relatedItem hierarchy of {}.", modsId);
        final List<Element> relatedItemLeaves = mods
            .getElements(".//" + LINKED_RELATED_ITEMS + "[not(mods:relatedItem)]");
        try {
            relatedItemLeaves.forEach(e -> checkHierarchy(e, new HashSet<>(Set.of(modsId))));
        } catch (MCRPersistenceException e) {
            throw new MCRPersistenceException("Hierarchy of mods:relatedItem in " + modsId + " contains circuits.", e);
        }
    }

    /**
     * Recursivly checks <code>relatedItem</code> and parent &lt;mods:relatedItem&gt; elements for multiple {@link MCRObjectID}s.
     * @param relatedItem &lt;mods:relatedItem&gt;
     * @param idCollected of IDs collected so far
     * @throws MCRPersistenceException if {@link MCRObjectID} of <code>relatedItem</code> is in <code>idCollected</code>
     */
    private void checkHierarchy(Element relatedItem, Set<MCRObjectID> idCollected) throws MCRPersistenceException {
        final Attribute href = relatedItem.getAttribute("href", MCRConstants.XLINK_NAMESPACE);
        if (href != null) {
            final String testId = href.getValue();
            LOGGER.debug("Checking relatedItem {}.", testId);
            if (MCRObjectID.isValid(testId)) {
                final MCRObjectID relatedItemId = MCRObjectID.getInstance(testId);
                LOGGER.debug("Checking if {} is in {}.", relatedItemId, idCollected);
                if (!idCollected.add(relatedItemId)) {
                    throw new MCRPersistenceException(
                        "Hierarchy of mods:relatedItem contains ciruit by object " + relatedItemId);
                }
            }
        }
        final Element parentElement = relatedItem.getParentElement();
        if (parentElement.getName().equals("relatedItem")) {
            checkHierarchy(parentElement, idCollected);
        }
    }

    /**
     * Can be used in inherited classes to lock objects before processing them. The default implementation does nothing,
     * but calling the {@link Consumer} for each object.
     * @param objects the objects to lock
     * @param lockedObjectConsumer the consumer that should be called for each locked object
     */
    protected void runWithLockedObject(List<MCRObjectID> objects, Consumer<MCRObjectID> lockedObjectConsumer) {
        objects.forEach(lockedObjectConsumer);
    }

}
