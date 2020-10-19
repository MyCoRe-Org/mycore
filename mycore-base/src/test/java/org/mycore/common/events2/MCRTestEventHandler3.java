package org.mycore.common.events2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@MCREventHandler(before = {MCRTestEventHandler2.class}, after = { MCRTestEventHandler1.class })
public class MCRTestEventHandler3 {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRHandlerMethod
    public void handleCreate(MCRTestEvent1 event){
        LOGGER.info("handleCreate: " + event.getId());
    }

    @MCRHandlerMethod
    public void handleUpdate(MCRTestEvent2 event){
        LOGGER.info("handleUpdate: " + event.getId());
    }

}
