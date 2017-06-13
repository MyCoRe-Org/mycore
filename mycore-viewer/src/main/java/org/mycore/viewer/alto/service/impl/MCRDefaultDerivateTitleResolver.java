package org.mycore.viewer.alto.service.impl;

import java.util.concurrent.TimeUnit;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.viewer.alto.service.MCRDerivateTitleResolver;

public class MCRDefaultDerivateTitleResolver implements MCRDerivateTitleResolver {

    private static final int EXPIRE_METADATA_CACHE_TIME = 10; // in seconds

    @Override
    public String resolveTitle(String derivateIDString) {
        MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDString);
        final MCRObjectID objectID = MCRMetadataManager.getObjectId(derivateID, EXPIRE_METADATA_CACHE_TIME,
            TimeUnit.SECONDS);
        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
        return object.getStructure().getDerivateLink(derivateID).getXLinkTitle();
    }
}
