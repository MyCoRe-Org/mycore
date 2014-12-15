package org.mycore.handle;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Class handles events related to ... handles. 
 * 
 * @author shermann
 *
 */
public class MCRHandleEventHandler extends MCREventHandlerBase {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleEventHandler.class);

    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        try {
            if (file instanceof MCRPath) {
                MCRHandleManager.delete(MCRPath.toMCRPath(file));
            }
        } catch (Throwable e) {
            LOGGER.error("Could not delete handle", e);
        }
    }
}
