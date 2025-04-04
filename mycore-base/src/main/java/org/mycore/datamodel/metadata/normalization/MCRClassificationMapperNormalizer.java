package org.mycore.datamodel.metadata.normalization;

import org.mycore.datamodel.classifications2.MCRDefaultClassificationMapper;
import org.mycore.datamodel.metadata.MCRObject;

public class MCRClassificationMapperNormalizer extends MCRObjectNormalizer {

    private final MCRDefaultClassificationMapper mapper;

    public MCRClassificationMapperNormalizer() {
        this.mapper = new MCRDefaultClassificationMapper();
    }

    @Override
    public void normalize(MCRObject mcrObject) {
        this.mapper.clearMappings(mcrObject);
    }
}
