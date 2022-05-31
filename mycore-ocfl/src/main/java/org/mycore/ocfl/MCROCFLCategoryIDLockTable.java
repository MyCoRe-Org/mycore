/*
* This file is part of ***  M y C o R e  ***
* See http://www.mycore.de/ for details.
*
* MyCoRe is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* MyCoRe is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.mycore.ocfl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Handles locking of OCFL Classes, on top of the OCFL Builtin Object Lock
 * @author Tobias Lenhardt [Hammer1279]
 * @author Thomas Scheffler (yagee)
 */
public class MCROCFLCategoryIDLockTable implements MCRSessionListener {
    private static final MCROCFLCategoryIDLockTable SINGLETON = new MCROCFLCategoryIDLockTable();

    private static Logger LOGGER = LogManager.getLogger(MCROCFLCategoryIDLockTable.class);

    private ConcurrentMap<MCRCategoryID, MCRSession> lockMap;

    private static MCROCFLCategoryIDLockTable getInstance() {
        return SINGLETON;
    }

    private MCROCFLCategoryIDLockTable() {
        this.lockMap = new ConcurrentHashMap<>();
        MCRSessionMgr.addSessionListener(this);
    }

    public void clearTable(MCRSession session) {
        for (MCRCategoryID categoryID : lockMap.keySet()) {
            lockMap.remove(categoryID, session);
        }
    }

    public static void unlock(MCRCategoryID categoryID) {
        getInstance().lockMap.remove(categoryID);
    }

    public static void lock(MCRCategoryID categoryID) {
        getInstance().lockMap.putIfAbsent(categoryID, MCRSessionMgr.getCurrentSession());
    }

    public static boolean isLockedByCurrentSession(MCRCategoryID categoryID) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        return session.equals(getInstance().lockMap.get(categoryID));
    }

    public static MCRSession getLocker(MCRCategoryID categoryID) {
        return getInstance().lockMap.get(categoryID);
    }

    @Override
    public void sessionEvent(MCRSessionEvent event) {
        switch (event.getType()) {
            case destroyed:
                clearTable(event.getSession());
                break;
            default:
                LOGGER.debug("Skipping event: {}", event.getType());
                break;
        }
    }

    public static boolean isLockedByCurrentSession(String categoryID) {
        MCRCategoryID catId = MCRCategoryID.fromString(categoryID);
        return MCROCFLCategoryIDLockTable.isLockedByCurrentSession(catId);
    }

    public static String getLockingUserName(String categoryID) {
        MCRCategoryID catId = MCRCategoryID.fromString(categoryID);
        MCRSession locker = MCROCFLCategoryIDLockTable.getLocker(catId);
        if (locker == null) {
            return null;
        }
        return locker.getUserInformation().getUserID();
    }

    public static boolean isLocked(String categoryID) {
        MCRCategoryID catId = MCRCategoryID.fromString(categoryID);
        return isLocked(catId);
    }

    public static boolean isLocked(MCRCategoryID categoryID) {
        MCROCFLCategoryIDLockTable instance = getInstance();
        return instance.lockMap.containsKey(categoryID);
    }
}
