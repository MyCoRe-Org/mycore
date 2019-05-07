package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration;

/**
 * Handles andle which information is present in the {@link MCRMetaEnrichedLinkID} for Derivates.
 * Set class with: MCR.Metadata.EnrichedDerivateLinkIDFactory.Class
 */
public abstract class MCRMetaEnrichedDerivateLinkIDFactory {

    protected MCRMetaEnrichedDerivateLinkIDFactory() {
    }

    public static MCRMetaEnrichedDerivateLinkIDFactory getInstance() {
        return MCRConfiguration.instance()
            .getInstanceOf("MCR.Metadata.EnrichedDerivateLinkIDFactory.Class",
                MCRDefaultEnrichedDerivateLinkIDFactory.class.getName());
    }

    public abstract MCRMetaEnrichedLinkID getDerivateLink(MCRDerivate der);

    public MCRMetaEnrichedLinkID getEmptyLinkID(){
        return new MCRMetaEnrichedLinkID();
    }
}
