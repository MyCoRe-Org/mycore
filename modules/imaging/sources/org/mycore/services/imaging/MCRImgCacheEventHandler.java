package org.mycore.services.imaging;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;

public class MCRImgCacheEventHandler extends MCREventHandlerBase {
    private static Logger LOGGER = Logger.getLogger(MCRImgCacheEventHandler.class.getName());

    /**
     * Handles file created events.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        LOGGER.debug("MCRImgCacheEventHandler.handleFileCreated");
        LOGGER.debug("FileName: " + file.getName());
        LOGGER.debug("OwnerID: " + file.getOwnerID());
        MCRImgCacheManager imgCache = MCRImgCacheManager.instance();

        if (!file.getOwnerID().equals(MCRImgCacheManager.CACHE_FOLDER) && !imgCache.existInCache(file)) {
            LOGGER.debug("MCRImgCacheEventHandler.handleFileCreated - IF");
            LOGGER.debug("FileName: " + file.getName());
            LOGGER.debug("OwnerID: " + file.getOwnerID());
            LOGGER.debug("ID: " + file.getID());
            try {
                MCRImgCacheCommands.cacheFile(file.getID());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            LOGGER.debug("MCRImgCacheEventHandler.handleFileCreated");
            LOGGER.debug("We don't want to cache files from Image Cache");
        }
    }

    /**
     * Handles file deleted events.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        MCRImgCacheManager imgCache = MCRImgCacheManager.instance();
        if (imgCache.existInCache(file)) {
            try {
                MCRImgCacheCommands.deleteCachedFile(file.getID());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            LOGGER.debug("MCRImgCacheEventHandler.handleFileDeleted");
            LOGGER.debug("Remove File: " + file.getName());
        } else {
            LOGGER.debug("MCRImgCacheEventHandler.handleFileCreated");
            LOGGER.debug("We only delete files from Image Cache");
        }
    }

}
