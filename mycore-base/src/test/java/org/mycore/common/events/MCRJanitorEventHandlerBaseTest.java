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

package org.mycore.common.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.metadata.MCRObject;

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
            MCREvent evt = new MCREvent(MCREvent.ObjectType.OBJECT, MCREvent.EventType.CREATE);
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

    @Test
    public void testOtherThreadSeesOriginalUser() throws Exception {

        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRSystemUserInformation originalUser = MCRSystemUserInformation.getGuestInstance();
        session.setUserInformation(originalUser);

        CountDownLatch insideHandler = new CountDownLatch(1);
        CountDownLatch checkedUser = new CountDownLatch(1);
        AtomicReference<String> observedUserId = new AtomicReference<>();

        MCRJanitorEventHandlerBase eventHandler = new MCRJanitorEventHandlerBase() {
            @Override
            protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
                // signal that we are inside the handler with janitor active
                insideHandler.countDown();
                try {
                    // wait until the other thread has checked the user
                    checkedUser.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        // other thread binds same session and checks user info
        Thread otherThread = new Thread(() -> {
            MCRSessionMgr.setCurrentSession(session);
            try {
                insideHandler.await(30, TimeUnit.SECONDS);
                observedUserId.set(session.getUserInformation().getUserID());
                checkedUser.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                MCRSessionMgr.releaseCurrentSession();
            }
        });
        otherThread.start();

        MCREvent evt = new MCREvent(MCREvent.ObjectType.OBJECT, MCREvent.EventType.CREATE);
        evt.put(MCREvent.OBJECT_KEY, new MCRObject());
        eventHandler.doHandleEvent(evt);

        otherThread.join(5000);

        Assert.assertEquals("Other thread should see original user, not janitor",
            originalUser.getUserID(), observedUserId.get());
    }

    @Test
    public void testConcurrentAttachDetach() throws Exception {

        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRSystemUserInformation originalUser = MCRSystemUserInformation.getGuestInstance();
        session.setUserInformation(originalUser);

        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicBoolean anyFailure = new AtomicBoolean(false);

        MCRJanitorEventHandlerBase eventHandler = new MCRJanitorEventHandlerBase() {
            @Override
            protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
                String userId = session.getUserInformation().getUserID();
                if (!MCRSystemUserInformation.getJanitorInstance().getUserID().equals(userId)) {
                    anyFailure.set(true);
                }
            }
        };

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                MCRSessionMgr.setCurrentSession(session);
                try {
                    barrier.await(30, TimeUnit.SECONDS);
                    MCREvent evt = new MCREvent(MCREvent.ObjectType.OBJECT, MCREvent.EventType.CREATE);
                    evt.put(MCREvent.OBJECT_KEY, new MCRObject());
                    eventHandler.doHandleEvent(evt);
                } catch (Exception e) {
                    anyFailure.set(true);
                } finally {
                    MCRSessionMgr.releaseCurrentSession();
                    done.countDown();
                }
            }).start();
        }

        Assert.assertTrue("All threads should finish within timeout", done.await(30, TimeUnit.SECONDS));

        Assert.assertFalse("No thread should have seen wrong user during janitor execution", anyFailure.get());
        Assert.assertEquals("Original user should be restored after all threads finished",
            originalUser.getUserID(), session.getUserInformation().getUserID());
    }
}
