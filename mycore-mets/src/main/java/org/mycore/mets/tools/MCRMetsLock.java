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

package org.mycore.mets.tools;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * 
 * Used to lock the mets editor for a specific Derivate
 * 
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRMetsLock {

    private static Hashtable<MCRObjectID, String> metsAccessSessionTable = new Hashtable<>();

    private static final Logger LOGGER = LogManager.getLogger(MCRMetsLock.class);

    /**
     * Checks if a Derivate is locked
     * @param derivateIdString the derivate id
     * @return true if the derivate is locked
     */
    public static synchronized boolean isLocked(String derivateIdString) {
        MCRObjectID derivateId = MCRObjectID.getInstance(derivateIdString);
        if (MCRMetsLock.metsAccessSessionTable.containsKey(derivateId)) {
            String lastAccessID = MCRMetsLock.metsAccessSessionTable.get(derivateId);
            MCRSession lastSession = MCRSessionMgr.getSession(lastAccessID);
            LOGGER
                .debug(new MessageFormat("{0} is locked : {1}", Locale.ROOT)
                    .format(new Object[] { derivateIdString, lastSession != null }));
            return lastSession != null;
        } else {
            LOGGER.debug(new MessageFormat("{0} is not locked", Locale.ROOT).format(derivateIdString));
            return false;
        }
    }

    /**
     * Locks a Derivate with the current SessionId
     * @param derivateIdString the Derivate to lock
     * @return True if the derivate is locked. False if the derivate could not be locked.
     */
    public static synchronized boolean doLock(String derivateIdString) {
        MCRObjectID derivateId = MCRObjectID.getInstance(derivateIdString);
        if (isLocked(derivateIdString)
            && MCRMetsLock.metsAccessSessionTable.get(derivateId) != MCRSessionMgr.getCurrentSessionID()) {
            LOGGER.info(
                new MessageFormat("Could not lock {0}, because its already locked.", Locale.ROOT)
                    .format(derivateIdString));
            return false;
        } else {
            LOGGER.info(new MessageFormat("{0} is now locked", Locale.ROOT).format(derivateIdString));
            MCRMetsLock.metsAccessSessionTable.put(derivateId, MCRSessionMgr.getCurrentSessionID());
            return true;
        }
    }

    /**
     * Unlocks a Derivate wich was locked with the current SessionId
     * @param derivateIdString the id of the derivate
     * @throws MCRException if the session-id of locker is different from current session-id
     */
    public static synchronized void doUnlock(String derivateIdString) throws MCRException {
        MCRObjectID derivateId = MCRObjectID.getInstance(derivateIdString);
        if (isLocked(derivateIdString)) {
            String sessionId = MCRMetsLock.metsAccessSessionTable.get(MCRObjectID.getInstance(derivateIdString));
            if (MCRSessionMgr.getCurrentSessionID().equals(sessionId)) {
                LOGGER.info("{} is not locked anymore", derivateIdString);
                MCRMetsLock.metsAccessSessionTable.remove(derivateId);
            } else {
                LOGGER.error("could not unlock {} because session id is different", derivateIdString);
                String message = String.format(Locale.ENGLISH,
                    "Could not unlock %s, because the session wich locked it was : ''%s'' "
                        + "and current sesssion is ''%s''",
                    derivateIdString, sessionId, MCRSessionMgr.getCurrentSessionID());
                throw new MCRException(message);
            }
        }
    }

}
