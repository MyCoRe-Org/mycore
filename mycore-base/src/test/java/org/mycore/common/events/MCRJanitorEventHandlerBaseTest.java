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
