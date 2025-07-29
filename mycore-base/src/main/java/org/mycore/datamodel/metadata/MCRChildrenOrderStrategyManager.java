package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

public final class MCRChildrenOrderStrategyManager {

    private static volatile MCRChildrenOrderStrategy childOrderStrategy;

    private MCRChildrenOrderStrategyManager() {
    }

    public static MCRChildrenOrderStrategy getChildOrderStrategy() {
        return ChildrenOrderStrategyHolder.INSTANCE;
    }

    private static final class ChildrenOrderStrategyHolder {
        private static final MCRChildrenOrderStrategy INSTANCE = MCRConfiguration2.getSingleInstanceOfOrThrow(
            MCRChildrenOrderStrategy.class, "MCR.ChildrenOrderStrategy");
    }

}
