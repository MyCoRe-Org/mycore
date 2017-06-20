package org.mycore.common.log4j2.lookups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRUserInformation;

public class MCRUserInformationLookupTest extends MCRTestCase {

    @Test
    public final void testLookupString() {
        MCRUserInformationLookup lookup = new MCRUserInformationLookup();
        assertNull("User information should not be available", lookup.lookup("id"));
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        assertEquals(MCRSystemUserInformation.getGuestInstance().getUserID(), lookup.lookup("id"));
        assertNull("Guest user should have no role", lookup.lookup("role:admin:editor:submitter"));
        mcrSession.setUserInformation(new MCRUserInformation() {

            @Override
            public boolean isUserInRole(String role) {
                return !role.startsWith("a");
            }

            @Override
            public String getUserID() {
                return "junit";
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        });
        String[] testRoles = new String[] { "admin", "editor", "submitter" };
        String expRole = testRoles[1];
        assertTrue("Current user should be in role " + expRole, mcrSession.getUserInformation().isUserInRole(expRole));
        assertEquals(expRole,
            lookup.lookup("role:" + Arrays.asList(testRoles).stream().collect(Collectors.joining(","))));
    }

}
