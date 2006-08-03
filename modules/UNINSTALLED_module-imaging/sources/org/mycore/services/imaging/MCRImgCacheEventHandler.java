package org.mycore.services.imaging;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRClassificationCommands;

public class MCRImgCacheEventHandler extends MCREventHandlerBase {
	private static Logger LOGGER = Logger.getLogger(MCRImgCacheEventHandler.class.getName());

	/**
	 * Handles file created events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void handleFileCreated(MCREvent evt, MCRFile file) {
		LOGGER.debug("*************************************************");
		LOGGER.debug("* MCRImgCacheEventHandler.handleFileCreated     *");
		LOGGER.debug("* FileName: "+ file.getName());
		LOGGER.debug("* OwnerID: "+ file.getOwnerID());
		LOGGER.debug("*************************************************");
		MCRConfiguration config = MCRConfiguration.instance();
		boolean useCache = Boolean.getBoolean(config.getString("MCR.Module-iview.useCache"));
		MCRImgCacheManager imgCache = new MCRImgCacheManager();
		
		if (useCache && !file.getOwnerID().equals(MCRImgCacheManager.CACHE_FOLDER) && !imgCache.existInCache(file)) {
			LOGGER.debug("**************************************************");
			LOGGER.debug("* MCRImgCacheEventHandler.handleFileCreated - IF *");
			LOGGER.debug("* FileName: "+ file.getName());
			LOGGER.debug("* OwnerID: "+ file.getOwnerID());
			LOGGER.debug("* ID: "+ file.getID());
			LOGGER.debug("**************************************************");
			try {
				MCRImgCacheCommands.cacheFile(file.getID());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			LOGGER.debug("*************************************************");
			LOGGER.debug("* MCRImgCacheEventHandler.handleFileCreated     *");
			LOGGER.debug("* We don't want to cache files from Image Cache *");
			LOGGER.debug("*************************************************");
		}
	}

	/**
	 * Handles file deleted events. This implementation does nothing and should
	 * be overwritted by subclasses.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void handleFileDeleted(MCREvent evt, MCRFile file) {
		MCRImgCacheManager imgCache = new MCRImgCacheManager();
		if (imgCache.existInCache(file)) {
			try {
				MCRImgCacheCommands.removeCachedFile(file.getID());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			LOGGER.debug("*************************************************");
			LOGGER.debug("* MCRImgCacheEventHandler.handleFileCreated     *");
			LOGGER.debug("* Remove File: " + file.getName());
			LOGGER.debug("*************************************************");
		} else {
			LOGGER.debug("*************************************************");
			LOGGER.debug("* MCRImgCacheEventHandler.handleFileCreated     *");
			LOGGER.debug("* We only delete files from Image Cache *");
			LOGGER.debug("*************************************************");
		}
	}
	
}
