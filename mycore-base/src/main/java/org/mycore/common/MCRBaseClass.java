package org.mycore.common;

import org.apache.log4j.Logger;

/**
 * @deprecated without replacement
 */
@Deprecated
public class MCRBaseClass {
    private Logger LOGGER = Logger.getLogger(this.getClass());

    public void setLOGGER(Logger lOGGER) {
        LOGGER = lOGGER;
    }

    public Logger getLOGGER() {
        return LOGGER;
    }
}
