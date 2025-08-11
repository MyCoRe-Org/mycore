package org.mycore.datamodel.metadata.normalization;

import org.mycore.datamodel.classifications2.MCRDefaultClassificationMapper;
import org.mycore.datamodel.metadata.MCRObject;

import java.util.Collections;

public class MCRDefaultClassificationMappingNormalizer extends MCRObjectNormalizer {

    private final MCRDefaultClassificationMapper mapper;

    public MCRDefaultClassificationMappingNormalizer() {
        this.mapper = new MCRDefaultClassificationMapper(Collections.emptyMap());
    }

    @Override
    public void normalize(MCRObject mcrObject) {
        this.mapper.clearMappings(mcrObject);
    }
}
