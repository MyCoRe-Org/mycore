package org.mycore.handle;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * Class handles events related to ... handles. 
 * 
 * @author shermann
 *
 */
public class MCRHandleEventHandler extends MCREventHandlerBase {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleEventHandler.class);

    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        try {
            MCRHandleManager.delete(file);
        } catch (Throwable e) {
            LOGGER.error("Could not delete handle", e);
        }
    }
}
