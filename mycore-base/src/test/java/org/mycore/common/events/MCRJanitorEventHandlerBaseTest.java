/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.events;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.metadata.MCRObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class MCRJanitorEventHandlerBaseTest extends MCRTestCase {

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

        MCRSystemUserInformation oldUserInformation = MCRSystemUserInformation.getGuestInstance();
        MCRSessionMgr.getCurrentSession().setUserInformation(oldUserInformation);

        boolean exceptionCatched = false;
        try {
            MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.CREATE_EVENT);
            evt.put(MCREvent.OBJECT_KEY, new MCRObject());
            eventHandler.doHandleEvent(evt);
        } catch (MCRException e) {
            exceptionCatched = true;
        }

        MCRUserInformation userInformation = MCRSessionMgr.getCurrentSession().getUserInformation();

        Assert.assertTrue("The EventHandler should have been called", eventHandlerCalled.get());
        Assert.assertTrue("A Exception should have been thrown", exceptionCatched);
        Assert.assertEquals("The UserInformation should be the same as before. (" + oldUserInformation.getUserID() + "/"
            + userInformation.getUserID() + ")", oldUserInformation, userInformation);
    }
}
