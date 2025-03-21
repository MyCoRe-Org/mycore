package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

public class MCRChildOrderStrategyManager {

    private static volatile MCRChildOrderStrategy instance;

    public static MCRChildOrderStrategy getInstance() {
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
