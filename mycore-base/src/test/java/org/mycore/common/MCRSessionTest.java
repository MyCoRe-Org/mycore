/**
 * 
 */
package org.mycore.common;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSessionTest extends MCRTestCase {

    private static final MCRSystemUserInformation SUPER_USER_INSTANCE = MCRSystemUserInformation.getSuperUserInstance();

    private static final MCRSystemUserInformation GUEST_INSTANCE = MCRSystemUserInformation.getGuestInstance();

    private MCRSession session;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.session = new MCRSession();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        this.session.close();
        super.tearDown();
    }

    @Test
    public void testIsGuestDefault() {
        assertEquals(GUEST_INSTANCE, session.getUserInformation());
    }

    @Test
    public void loginSuperUser() {
        session.setUserInformation(SUPER_USER_INSTANCE);
        assertEquals(SUPER_USER_INSTANCE, session.getUserInformation());
    }

    @Test
    public void downGradeUser() {
        MCRUserInformation otherUser = getSimpleUserInformation("JUnit");
        session.setUserInformation(SUPER_USER_INSTANCE);
        session.setUserInformation(otherUser);
        assertEquals(otherUser, session.getUserInformation());
        session.setUserInformation(GUEST_INSTANCE);
        assertEquals(GUEST_INSTANCE, session.getUserInformation());
    }

    @Test(expected = IllegalArgumentException.class)
    public void upgradeFail() {
        MCRUserInformation otherUser = getSimpleUserInformation("JUnit");
        session.setUserInformation(otherUser);
        session.setUserInformation(SUPER_USER_INSTANCE);
    }

    private static MCRUserInformation getSimpleUserInformation(String userID) {
        return new MCRUserInformation() {
            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public String getUserID() {
                return userID;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        };
    }

}
