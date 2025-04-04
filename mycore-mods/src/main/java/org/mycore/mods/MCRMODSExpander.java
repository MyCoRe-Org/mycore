package org.mycore.mods;

import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRBasicObjectExpander;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRExpandedObjectManager;
import org.mycore.common.MCRXlink;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.classifications2.MCRClassificationMapper;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.classification.MCRMODSClassificationMapper;

/**
 * This class is used to expand the content of mods:relatedItems by inheriting metadata from the parent object and
 * linked objects.
 */
@MCRConfigurationProxy(proxyClass = MCRMODSExpander.Factory.class)
public class MCRMODSExpander extends MCRBasicObjectExpander {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String HOST_SECTION_XPATH = "mods:relatedItem[@type='host']";

    public MCRMODSExpander(MCRClassificationMapper mapper) {
        super(mapper);
    }

    @Override
    public MCRExpandedObject expand(MCRObject mcrObject) {
        MCRExpandedObject baseExpanded = super.expand(mcrObject);

        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(baseExpanded);

        MCRObjectID parentID = baseExpanded.getStructure().getParentID();

        if (parentID != null && MCRMODSWrapper.isSupported(parentID)) {
            MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentID);
            MCRExpandedObject expandedParentObject = MCRExpandedObjectManager.getInstance().getExpandedObject(parent);
            MCRMODSWrapper parentWrapper = new MCRMODSWrapper(expandedParentObject);
            inheritParentMetadata(parentWrapper, modsWrapper);
        }

        inheritLinkedMetadata(modsWrapper, parentID);

        return baseExpanded;
    }

    /**
     * Inherits the metadata linked from the current to other objects
     * @param modsWrapper the current object
     * @param parentID the id of the parent object. Used to determine if the metadata is already inherited from
     *                the parent.
     */
    protected void inheritLinkedMetadata(MCRMODSWrapper modsWrapper, MCRObjectID parentID) {
        for (Element relatedItem : modsWrapper.getLinkedRelatedItems()) {
            String type = relatedItem.getAttributeValue("type");
            String holderId = relatedItem.getAttributeValue(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE);
            LOGGER.info("receive metadata from {} document {}", type, holderId);
            if ((holderId == null || parentID != null && parentID.toString().equals(holderId))
                && MCRMODSRelationshipType.HOST.getValue().equals(type)) {
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
    }

    protected List<Content> getClearContent(MCRMODSWrapper targetWrapper) {
        List<Content> inheritedData = targetWrapper.getMODS().cloneContent();
        inheritedData.stream()
            .filter(c -> c instanceof Element)
            .map(Element.class::cast)
            .filter(element -> element.getName().equals("relatedItem"))
            .filter(this::isClearableRelatedItem)
            .forEach(Element::removeContent);
        return inheritedData;
    }

    protected void inheritParentMetadata(MCRMODSWrapper parentWrapper, MCRMODSWrapper childWrapper) {
        LOGGER.info("Inserting inherited Metadata.");
        Element hostContainer = childWrapper.getElement(HOST_SECTION_XPATH);
        if (hostContainer == null) {
            LOGGER.info("Adding new relatedItem[@type='host'])");
            String objectId = parentWrapper.getMCRObject().getId().toString();
            hostContainer = new Element("relatedItem", MCRConstants.MODS_NAMESPACE)
                .setAttribute(MCRXlink.HREF, objectId, MCRConstants.XLINK_NAMESPACE)
                .setAttribute("type", "host");
            childWrapper.addElement(hostContainer);
        }
        hostContainer.addContent(parentWrapper.getMODS().cloneContent());
    }

    /** Determines if the content of the relatedItem should be removed, when it is inserted as related item
     * @param relatedItem the relatedItem element
     * @return true if the content should be removed
     */
    protected boolean isClearableRelatedItem(Element relatedItem) {
        return relatedItem.getAttributeValue(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE) != null;
    }



    public static class Factory implements Supplier<MCRMODSExpander> {

        public static final String CLASSIFICATION_MAPPING_ENABLED_PROPERTY = "ClassificationMappingEnabled";

        @MCRProperty(name = CLASSIFICATION_MAPPING_ENABLED_PROPERTY)
        public String classificationMappingEnabled;

        @Override
        public MCRMODSExpander get() {
            if(Boolean.parseBoolean(classificationMappingEnabled)) {
                return new MCRMODSExpander(new MCRMODSClassificationMapper());
            } else {
                return new MCRMODSExpander(null);
            }
        }
    }
}
