package org.mycore.user.hibernate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.MCRHibTestCase;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserCommands;

public class MCRHIBUserStoreTest extends MCRHibTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Users.Superuser.UserName", "adminUser", true);
        setProperty("MCR.Users.Superuser.GroupName", "adminGroup", true);
        setProperty("MCR.Persistence.User.Store.Class", MCRHIBUserStore.class.getCanonicalName(), true);
        MCRUserCommands.initSuperuser();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRHibTestCase#getHibernateConfiguration(org.mycore.backend.hibernate.MCRHIBConnection)
     */
    @Override
    protected Configuration getHibernateConfiguration() {
        MCRHIBConnection connection = MCRHIBConnection.instance();
        Configuration conf = connection.getConfiguration();
        if (connection.containsMapping("MCRUSERS"))
            return conf;
        return conf
            .addResource("org/mycore/user/hibernate/MCRGROUPADMINS.hbm.xml")
            .addResource("org/mycore/user/hibernate/MCRGROUPMEMBERS.hbm.xml")
            .addResource("org/mycore/user/hibernate/MCRGROUPS.hbm.xml")
            .addResource("org/mycore/user/hibernate/MCRUSERS.hbm.xml");
    }

    @Test
    public void createUser() throws MCRException, Exception {
        MCRUser user = new MCRUser("test", "testPwd");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createUser(user);
        startNewTransaction();
        assertNotNull("User was not stored." + user, userStore.retrieveUser(user.getID()));
    }

    @Test
    public void updateUser() throws MCRException, Exception {
        MCRUser user = new MCRUser("test", "testPwd");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createUser(user);
        startNewTransaction();
        user.setDescription("JUnit-Test");
        userStore.updateUser(user);
        startNewTransaction();
        assertNotNull("User description was not updated" + user.getDescription(), userStore.retrieveUser(user.getID()).getDescription());
    }

    @Test
    public void createGroup() {
        MCRGroup group = new MCRGroup("testGroup");
        MCRHIBUserStore userStore = new MCRHIBUserStore();
        userStore.createGroup(group);
        startNewTransaction();
        assertNotNull("User was not stored." + group, userStore.retrieveGroup(group.getID()));
    }

    @Test
    public void updateGroup() throws MCRException, Exception {
        MCRGroup group = new MCRGroup("testGroup");
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
