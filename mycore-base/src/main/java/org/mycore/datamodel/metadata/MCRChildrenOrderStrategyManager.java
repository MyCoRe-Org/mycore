package org.mycore.datamodel.metadata;

import org.mycore.common.config.MCRConfiguration2;

public final class MCRChildrenOrderStrategyManager {

    private static volatile MCRChildrenOrderStrategy childOrderStrategy;


  private MCRChildrenOrderStrategyManager() {
  }

  public static MCRChildrenOrderStrategy getChildOrderStrategy() {
    if (childOrderStrategy == null) {
            synchronized (MCRChildrenOrderStrategyManager.class) {
                if (childOrderStrategy == null) {
                    childOrderStrategy = MCRConfiguration2.getSingleInstanceOfOrThrow(
                        MCRChildrenOrderStrategy.class,
                        "MCR.ChildrenOrderStrategy");
                }
            }
        }
        return childOrderStrategy;
    }

}
