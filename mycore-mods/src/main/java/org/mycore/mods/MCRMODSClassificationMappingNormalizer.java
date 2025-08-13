package org.mycore.mods;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.normalization.MCRObjectNormalizer;
import org.mycore.mods.classification.mapping.MCRMODSGeneratorClassificationMapper;

import java.util.Collections;

/**
 * Removes all classification elements with the generator attribute set to *-mycore.
 */
public class MCRMODSClassificationMappingNormalizer extends MCRObjectNormalizer {

    private final MCRMODSGeneratorClassificationMapper mapper;

    public MCRMODSClassificationMappingNormalizer() {
        mapper = new MCRMODSGeneratorClassificationMapper(Collections.emptyMap());
    }

    @Override
    public void normalize(MCRObject mcrObject) {
        mapper.clearMappings(mcrObject);
    }

}
