package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Handles andle which information is present in the {@link MCRMetaEnrichedLinkID} for Derivates.
 * Set class with: MCR.Metadata.EnrichedDerivateLinkIDFactory.Class
 */
public abstract class MCRMetaEnrichedLinkIDFactory {

    protected MCRMetaEnrichedLinkIDFactory() {
    }

    public static MCRMetaEnrichedLinkIDFactory getInstance() {
        return MCRConfiguration2
            .<MCRMetaEnrichedLinkIDFactory> getInstanceOf("MCR.Metadata.EnrichedDerivateLinkIDFactory.Class")
            .orElseGet(MCRDefaultEnrichedDerivateLinkIDFactory::new);
    }

    public abstract MCREditableMetaEnrichedLinkID getDerivateLink(MCRDerivate der);

    public MCREditableMetaEnrichedLinkID getEmptyLinkID() {
        return new MCREditableMetaEnrichedLinkID();
    }
}
