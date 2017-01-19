package org.mycore.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @deprecated without replacement
 */
@Deprecated
public class MCRBaseClass {
    private Logger LOGGER = LogManager.getLogger(this.getClass());

    public void setLOGGER(Logger lOGGER) {
        LOGGER = lOGGER;
    }

    public Logger getLOGGER() {
        return LOGGER;
    }
}
