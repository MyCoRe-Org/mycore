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

import org.apache.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;

/**
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$
 */
public class MCRUploadHandlerManager {

    /** Cache of currently active upload handler sessions */
    protected static MCRCache<String, MCRUploadHandlerCacheEntry> handlers = new MCRCache<String, MCRUploadHandlerCacheEntry>(100, "UploadHandlerManager UploadHandlers");

    private static Logger logger = Logger.getLogger(MCRUploadHandlerManager.class);

    static void register(MCRUploadHandler handler) {
        logger.debug("Registering " + handler.getClass().getName() + " with upload ID " + handler.getID());
        String sessionID = MCRSessionMgr.getCurrentSession().getID();
        handlers.put(handler.getID(), new MCRUploadHandlerCacheEntry(sessionID, handler));
    }

    public static MCRUploadHandler getHandler(String uploadID) {

        long yesterday = System.currentTimeMillis() - 86400000;
        MCRUploadHandlerCacheEntry entry = handlers.getIfUpToDate(uploadID, yesterday);

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
        handlers.remove(uploadID);
    }
}

/** Represents a cache entry of currently active upload handler session */
class MCRUploadHandlerCacheEntry {

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
