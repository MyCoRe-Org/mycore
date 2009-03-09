package org.mycore.backend.hibernate;

import java.sql.Timestamp;

import org.mycore.common.MCRException;
import org.mycore.common.MCRHibTestCase;
import org.mycore.frontend.cli.MCRUserCommands;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;

public class MCRHIBUserStoreTest extends MCRHibTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Users.Superuser.UserName", "adminUser", true);
        setProperty("MCR.Users.Superuser.GroupName", "adminGroup", true);
        setProperty("MCR.Persistence.User.Store.Class", MCRHIBUserStore.class.getCanonicalName(), true);
        MCRUserCommands.initSuperuser();
    }

    public void testCreateUser() throws MCRException, Exception {
        MCRUser user = new MCRUser("test", "testPwd");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createUser(user);
        startNewTransaction();
        assertNotNull("User was not stored." + user, userStore.retrieveUser(user.getID()));
    }

    public void testUpdateUser() throws MCRException, Exception {
        MCRUser user = new MCRUser("test", "testPwd");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createUser(user);
        startNewTransaction();
        user.setDescription("JUnit-Test");
        userStore.updateUser(user);
        startNewTransaction();
        assertNotNull("User description was not updated" + user.getDescription(), userStore.retrieveUser(user.getID()).getDescription());
    }

    public void testCreateGroup() {
        MCRGroup group=new MCRGroup("testGroup");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createGroup(group);
        startNewTransaction();
        assertNotNull("User was not stored." + group, userStore.retrieveGroup(group.getID()));
    }

    public void testUpdateGroup() throws MCRException, Exception {
        MCRGroup group=new MCRGroup("testGroup");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createGroup(group);
        MCRUser user = new MCRUser("test", "testPwd");
        userStore.createUser(user);
        startNewTransaction();
        group.getAdminUserIDs().add(user.getID());
        userStore.updateGroup(group);
        assertTrue("Admin users were not updated", userStore.retrieveGroup(group.getID()).getAdminUserIDs().contains(user.getID()));
    }

}
