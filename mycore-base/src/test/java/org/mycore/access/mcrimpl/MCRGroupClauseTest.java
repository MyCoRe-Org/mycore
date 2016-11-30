package org.mycore.access.mcrimpl;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRUserInformation;

public class MCRGroupClauseTest extends MCRTestCase {

    private static final String INGROUP_NAME = "ingroup";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRUserInformation userInfo = new MCRUserInformation() {

            @Override
            public boolean isUserInRole(String role) {
                return INGROUP_NAME.equals(role);
            }

            @Override
            public String getUserID() {
                return "junit";
            }

            @Override
            public String getUserAttribute(String attribute) {
                // TODO Auto-generated method stub
                return null;
            }
        };
        MCRSessionMgr.getCurrentSession().setUserInformation(userInfo);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testEvaluate() {
        String outGroup = "outgroup";
        MCRGroupClause groupClause = new MCRGroupClause(INGROUP_NAME, false);
        assertTrue("Did not accept " + INGROUP_NAME, groupClause.evaluate(null));
        groupClause = new MCRGroupClause(outGroup, true);
        assertTrue("Did not accept " + outGroup, groupClause.evaluate(null));
    }

}
