package org.mycore.mods;


import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.normalization.MCRObjectNormalizer;

/**
 * This class normalizes the metadata of MCRMODSWrapper objects by sorting the elements in the MODS format.
 */
public class MCRMODSSortElementsMetadataNormalizer extends MCRObjectNormalizer {

    @Override
    public void normalize(MCRObject mcrObject) {
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(mcrObject);
        MCRMODSSorter.sort(modsWrapper.getMODS());
    }

}
