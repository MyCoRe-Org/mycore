/*
 * $Id$ 
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
package org.mycore.user2.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRPersistTransientUserEventHandler extends MCREventHandlerBase {

    private static Logger LOGGER = LogManager.getLogger(MCRPersistTransientUserEventHandler.class);

    /**
     * Persists {@link MCRTransientUser} if an {@link MCRObject} was created.
     * 
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!MCRUserManager.isInvalidUser(currentUser) && MCRUserManager.getUser(currentUser.getUserID()) == null) {
            LOGGER.info("create new user \"" + currentUser.getUserID() + "\"");
            MCRUserManager.createUser((MCRTransientUser) currentUser);
        }
    }

}
