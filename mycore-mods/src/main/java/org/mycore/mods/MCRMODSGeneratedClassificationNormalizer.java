package org.mycore.mods;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.normalization.MCRObjectNormalizer;
import org.mycore.mods.classification.MCRMODSClassificationMapper;

/**
 * Removes all classification elements with the generator attribute set to *-mycore.
 */
public class MCRMODSGeneratedClassificationNormalizer extends MCRObjectNormalizer {

    private final MCRMODSClassificationMapper mapper;


    public MCRMODSGeneratedClassificationNormalizer() {
        mapper = new MCRMODSClassificationMapper();
    }

    @Override
    public void normalize(MCRObject mcrObject) {
        mapper.clearMappings(mcrObject);
    }

}
