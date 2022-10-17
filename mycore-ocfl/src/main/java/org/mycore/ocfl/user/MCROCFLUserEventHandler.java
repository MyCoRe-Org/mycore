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

package org.mycore.ocfl.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.user2.MCRUser;

/**
 * Event Handler to Handle MCRUser Events for OCFL
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLUserEventHandler implements MCREventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCROCFLXMLUserManager MANAGER = new MCROCFLXMLUserManager();

    @Override
    public void doHandleEvent(MCREvent evt) throws MCRException {
        if (MCREvent.ObjectType.USER == evt.getObjectType()) {
            MCRUser user = (MCRUser) evt.get(MCREvent.USER_KEY);
            LOGGER.debug("{} handling {} {}", getClass().getName(), user.getUserID(),
                evt.getEventType());
            switch (evt.getEventType()) {
            case UPDATE:
                MANAGER.updateUser(user);
                break;
            case CREATE:
                MANAGER.createUser(user);
                break;
            case DELETE:
                MANAGER.deleteUser(user);
                break;

            default:
                LOGGER.info("Event Type '{}' is not valid for {}", evt.getEventType(), getClass().getName());
                break;
            }
        }
    }

    @Override
    public void undoHandleEvent(MCREvent evt) throws MCRException {
        // undo not supported
        LOGGER.warn("A Error has occurred while saving User, please run 'sync ocfl users' in cli.");
    }

}
