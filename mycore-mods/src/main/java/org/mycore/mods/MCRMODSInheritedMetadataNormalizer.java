package org.mycore.mods;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.normalization.MCRObjectNormalizer;

/**
 * This class is responsible for normalizing MCRObjects by removing inherited metadata from mods:relatedItem elements.
 */
public class MCRMODSInheritedMetadataNormalizer extends MCRObjectNormalizer {

    @Override
    public void normalize(MCRObject mcrObject) {
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(mcrObject);
        modsWrapper.removeInheritedMetadata();
    }
}
