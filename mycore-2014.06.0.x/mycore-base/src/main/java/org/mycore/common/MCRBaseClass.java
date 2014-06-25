package org.mycore.common;

import org.apache.log4j.Logger;

public class MCRBaseClass {
    private Logger LOGGER = Logger.getLogger(this.getClass());

    public void setLOGGER(Logger lOGGER) {
        LOGGER = lOGGER;
    }

    public Logger getLOGGER() {
        return LOGGER;
    }
}
