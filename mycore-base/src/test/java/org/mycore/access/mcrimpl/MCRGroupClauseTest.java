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
