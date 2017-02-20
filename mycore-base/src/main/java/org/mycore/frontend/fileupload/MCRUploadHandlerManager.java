/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.fileupload;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;

/**
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$
 */
public class MCRUploadHandlerManager {

    private static Logger LOGGER = LogManager.getLogger(MCRUploadHandlerManager.class);

    /** Cache of currently active upload handler sessions */
    protected static MCRCache<String, MCRUploadHandlerCacheEntry> HANDLERS;

    protected static MCRProcessableCollection COLLECTION;

    static {
        HANDLERS = new MCRCache<String, MCRUploadHandlerCacheEntry>(100, "UploadHandlerManager UploadHandlers");
        COLLECTION = new MCRProcessableDefaultCollection("Upload Manager");
        MCRProcessableRegistry registry = MCRInjectorConfig.injector().getInstance(MCRProcessableRegistry.class);
        registry.register(COLLECTION);

    }

    static void register(MCRUploadHandler handler) {
        LOGGER.debug("Registering " + handler.getClass().getName() + " with upload ID " + handler.getID());
        String sessionID = MCRSessionMgr.getCurrentSession().getID();
        HANDLERS.put(handler.getID(), new MCRUploadHandlerCacheEntry(sessionID, handler));
        COLLECTION.add(handler);
    }

    public static MCRUploadHandler getHandler(String uploadID) {

        long yesterday = System.currentTimeMillis() - 86400000;
        MCRUploadHandlerCacheEntry entry = HANDLERS.getIfUpToDate(uploadID, yesterday);

        if (entry == null)
            throw new MCRUsageException("Upload session " + uploadID + " timed out");

        String sessionID = entry.getSessionID();

        if (!sessionID.equals(MCRSessionMgr.getCurrentSessionID())) {
            MCRSession session = MCRSessionMgr.getSession(sessionID);
            if (session != null)
                MCRSessionMgr.setCurrentSession(session);
        }

        return entry.getUploadHandler();
    }

    public static void unregister(String uploadID) {
        MCRUploadHandlerCacheEntry cacheEntry = HANDLERS.get(uploadID);
        HANDLERS.remove(uploadID);
        COLLECTION.remove(cacheEntry.handler);
    }

    /** Represents a cache entry of currently active upload handler session */
    private static class MCRUploadHandlerCacheEntry {

        /** The ID of the MCRSession this upload is associated with */
        private String sessionID;

        /** The MCRUploadHander instance to be used */
        private MCRUploadHandler handler;

        public MCRUploadHandlerCacheEntry(String sessionID, MCRUploadHandler handler) {
            this.sessionID = sessionID;
            this.handler = handler;
        }

        public String getSessionID() {
            return sessionID;
        }

        public MCRUploadHandler getUploadHandler() {
            return handler;
        }
    }
}
