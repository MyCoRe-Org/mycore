package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

public final class MCRChildrenOrderStrategyManager {

    private MCRChildrenOrderStrategyManager() {
    }

    public static MCRChildrenOrderStrategy getChildrenOrderStrategy() {
        return ChildrenOrderStrategyHolder.INSTANCE;
    }

    private static final class ChildrenOrderStrategyHolder {
        private static final MCRChildrenOrderStrategy INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRChildrenOrderStrategy.class, "MCR.ChildrenOrderStrategy.Class");
    }

}
