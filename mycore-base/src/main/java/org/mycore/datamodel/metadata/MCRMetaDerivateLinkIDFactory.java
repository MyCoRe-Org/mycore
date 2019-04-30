package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration;

public abstract class MCRMetaDerivateLinkIDFactory {

    protected MCRMetaDerivateLinkIDFactory() {
    }

    public static MCRMetaDerivateLinkIDFactory getInstance() {
        return MCRConfiguration.instance()
            .getInstanceOf("MCR.Metadata.DerivateLinkIDFactory.class",
                MCRDefaultMetaDerivateLinkIDFactory.class.getName());
    }

    public abstract MCRMetaDerivateLinkID getDerivateLink(MCRDerivate der);

    public MCRMetaDerivateLinkID getEmptyDerivateLink(){
        return new MCRMetaDerivateLinkID();
    }
}
