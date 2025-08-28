/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRJanitorEventHandlerBaseTest {

    @Test
    public void testUserSwitchBack() {

        AtomicBoolean eventHandlerCalled = new AtomicBoolean(false);

        MCRJanitorEventHandlerBase eventHandler = new MCRJanitorEventHandlerBase() {
            @Override
            protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
                eventHandlerCalled.set(true);
                throw new MCRException("Error that happened");
            }
        };

        MCRSystemUserInformation oldUserInformation = MCRSystemUserInformation.GUEST;
        MCRSessionMgr.getCurrentSession().setUserInformation(oldUserInformation);

        boolean exceptionCatched = false;
        try {
            MCREvent evt = new MCREvent(MCREvent.ObjectType.OBJECT, MCREvent.EventType.CREATE);
            evt.put(MCREvent.OBJECT_KEY, new MCRObject());
            eventHandler.doHandleEvent(evt);
        } catch (MCRException e) {
            exceptionCatched = true;
        }

        MCRUserInformation userInformation = MCRSessionMgr.getCurrentSession().getUserInformation();

        assertTrue(eventHandlerCalled.get(), "The EventHandler should have been called");
        assertTrue(exceptionCatched, "A Exception should have been thrown");
        assertEquals(oldUserInformation, userInformation,
            "The UserInformation should be the same as before. (" + oldUserInformation.getUserID() + "/"
                + userInformation.getUserID() + ")");
    }
}
