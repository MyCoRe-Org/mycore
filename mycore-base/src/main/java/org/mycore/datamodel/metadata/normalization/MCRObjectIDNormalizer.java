package org.mycore.datamodel.metadata.normalization;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This normalizer is used to assign a new object id to the MCRObject if the current id is 0.
 */
public class MCRObjectIDNormalizer extends MCRObjectNormalizer {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void normalize(MCRObject mcrObject) {
        MCRObjectID objectId = Objects.requireNonNull(mcrObject.getId(), "ObjectID must not be null");

        // assign new id if necessary
        if (objectId.getNumberAsInteger() == 0) {
            MCRObjectID oldId = objectId;
            objectId = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(objectId.getBase());
            mcrObject.setId(objectId);
            LOGGER.info("Assigned new object id {}", objectId);

            // if label was id with 00000000, set label to new id
            if (Objects.equals(mcrObject.getLabel(), oldId.toString())) {
                mcrObject.setLabel(objectId.toString());
            }
        }
    }
}
