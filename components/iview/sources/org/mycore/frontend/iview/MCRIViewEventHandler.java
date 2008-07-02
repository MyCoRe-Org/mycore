package org.mycore.frontend.iview;

import org.apache.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;

public class MCRIViewEventHandler extends MCREventHandlerBase {
	private static Logger LOGGER = Logger.getLogger(MCRIViewEventHandler.class.getName());

	/**
	 * Handles file created events.
	 * 
	 * @param evt
	 *            the event that occured
	 * @param file
	 *            the MCRFile that caused the event
	 */
	protected void handleFileCreated(MCREvent evt, MCRFile file) {
		removeCachedFileNodeList(file);
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
		removeCachedFileNodeList(file);
	}

	private void removeCachedFileNodeList(MCRFile file) {
		MCRSession session = MCRSessionMgr.getCurrentSession();
		if (session!=null) {
			String cacheObjKey = "IView.FileNodesList";
			Object cacheObj = session.get(cacheObjKey);
			if (cacheObj!=null) {
				MCRCache cachedFileNodeList = (MCRCache)cacheObj;
				String dirID = file.getParentID();
				if (dirID!=null && !dirID.equals("")
						&& cachedFileNodeList.get(dirID)!=null) {
					LOGGER.debug("remove cached file node list");
					cachedFileNodeList.remove(dirID);	
				}				
			}
		}
	}
	
}














