/*
 * $RCSfile$
 * $Revision$ $Date$
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

/**
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRUploadHandlerManager {
    protected static MCRCache handlers = new MCRCache(100,"UploadHandlerManager UploadHandlers");

    private static Logger logger = Logger.getLogger(MCRUploadHandlerManager.class);

    static void register(MCRUploadHandler handler) {
        logger.debug("Registered " + handler.getClass().getName() + " with upload ID " + handler.getID());
        handlers.put(handler.getID(), handler);
    }

    public static MCRUploadHandler getHandler(String uploadID) {
        long yesterday = System.currentTimeMillis() - 86400000;

        return (MCRUploadHandler) (handlers.getIfUpToDate(uploadID, yesterday));
    }

    public static void unregister(String uploadID) {
        handlers.remove(uploadID);
    }
}
