package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

public class MCRChildOrderStrategyManager {

    private static volatile MCRChildOrderStrategy childOrderStrategy;

    public static MCRChildOrderStrategy getChildOrderStrategy() {
        if (childOrderStrategy == null) {
            synchronized (MCRChildOrderStrategyManager.class) {
                if (childOrderStrategy == null) {
                    childOrderStrategy = MCRConfiguration2.getSingleInstanceOfOrThrow(MCRChildOrderStrategy.class,
                        "MCR.ChildOrderStrategy");
                }
            }
        }
        return childOrderStrategy;
    }

}
