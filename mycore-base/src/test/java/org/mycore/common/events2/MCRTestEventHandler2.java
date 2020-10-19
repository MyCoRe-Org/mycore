package org.mycore.common.events2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@MCREventHandler(after = { MCRTestEventHandler1.class })
public class MCRTestEventHandler2 {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRHandlerMethod()
    public void handleCreate(MCRTestEvent1 event) {
        LOGGER.info("handleCreate: " + event.getId());
    }

}
