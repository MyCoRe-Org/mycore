package org.mycore.common.events2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@MCREventHandler()
public class MCRTestEventHandler1 {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRHandlerMethod()
    public void handleCreate(MCRCreateObjectEvent event){
       LOGGER.info("handleCreate: " + event.getId());
    }

    @MCRHandlerMethod
    public void handleUpdate(MCRUpdateObjectEvent event){
        LOGGER.info("handleUpdate: " + event.getId());
    }
}
