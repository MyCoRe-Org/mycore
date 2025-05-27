package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

public final class MCRChildOrderStrategyManager {

    private static volatile MCRChildOrderStrategy instance;

    private MCRChildOrderStrategyManager() {
    }

    public static MCRChildOrderStrategy getChildOrderStrategy() {
        if (instance == null) {
            synchronized (MCRChildOrderStrategyManager.class) {
                if (instance == null) {
                    instance = MCRConfiguration2.getSingleInstanceOfOrThrow(MCRChildOrderStrategy.class,
                        "MCR.ChildOrderStrategy");
                }
            }
        }
        return instance;
    }

}
