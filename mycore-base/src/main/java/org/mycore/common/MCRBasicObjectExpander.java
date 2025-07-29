package org.mycore.common;

import static org.mycore.datamodel.metadata.MCRObjectService.DATE_TYPE_EFFECTIVE_MODIFIED_DATE;

import java.util.Date;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.classifications2.MCRClassificationMapper;
import org.mycore.datamodel.classifications2.MCRDefaultClassificationMapper;
import org.mycore.datamodel.metadata.MCRChildrenOrderStrategyManager;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRExpandedObjectStructure;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkIDFactory;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class expands common elements of MCRObjects, including: children and derivates.
 */
@MCRConfigurationProxy(proxyClass = MCRBasicObjectExpander.Factory.class)
public class MCRBasicObjectExpander implements MCRObjectExpander {

    private final MCRClassificationMapper mapper;

    /**
     * Constructs a new MCRBasicObjectExpander.
     *
     * @param mapper The classification mapper to use, or null if classification mapping is disabled.
     */
    public MCRBasicObjectExpander(MCRClassificationMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Expands the given MCRObject by resolving its structure (parent, children, derivates)
     * and potentially handling classification mappings. It also stores an effectivemodifydate in
     * the Service Element of the MCRExpandedObject.
     *
     * @param mcrObject The MCRObject to expand.
     * @return An MCRExpandedObject containing the expanded structure and metadata.
     */
    @Override
    public MCRExpandedObject expand(MCRObject mcrObject) {

        MCRExpandedObjectStructure newStructure = new MCRExpandedObjectStructure();
        if (mcrObject.hasParent()) {
            newStructure.setParent(mcrObject.getParent());
        }

        for (MCRObjectID mcrObjectID : MCRChildrenOrderStrategyManager.getChildOrderStrategy()
            .getChildOrder(mcrObject)) {
            newStructure.addChild(new MCRMetaLinkID("child", mcrObjectID, null, mcrObjectID.toString()));
        }

        mcrObject.getStructure().getChildrenOrder().forEach(newStructure.getChildrenOrder()::add);

        MCRMetadataManager.getDerivateIds(mcrObject.getId())
            .stream()
            .map(MCRMetadataManager::retrieveMCRDerivate)
            .map(MCRMetaEnrichedLinkIDFactory.obtainInstance()::getDerivateLink)
            .forEach(newStructure::addDerivate);

        MCRExpandedObject expandedObject =
            new MCRExpandedObject(newStructure, mcrObject.getMetadata(), mcrObject.getService(), "");

        expandedObject.setId(mcrObject.getId());
        expandedObject.setVersion(mcrObject.getVersion());
        expandedObject.setSchema(mcrObject.getSchema());

        expandClassifications(mcrObject);

        expandedObject.getService().setDate(DATE_TYPE_EFFECTIVE_MODIFIED_DATE, new Date());

        return expandedObject;
    }

    /**
     * Handles the expansion of classifications for the given MCRObject,
     * if a classification mapper is configured.
     *
     * @param mcrObject The MCRObject whose classifications should be expanded.
     */
    void expandClassifications(MCRObject mcrObject) {
        if (mapper != null) {
            mapper.clearMappings(mcrObject);
            mapper.createMapping(mcrObject);
        }
    }

    public static final class Factory implements Supplier<MCRBasicObjectExpander> {

        @MCRProperty(name = "ClassificationMappingEnabled")
        public String classificationMappingEnabled;

        @Override
        public MCRBasicObjectExpander get() {
            MCRDefaultClassificationMapper mapper =
                Boolean.parseBoolean(classificationMappingEnabled) ? new MCRDefaultClassificationMapper() : null;
            return new MCRBasicObjectExpander(mapper);
        }
    }

}
