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

package org.mycore.access.facts.condition.fact;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestCase;

public class MCRRoleConditionTest extends MCRTestCase {

    @Test
    public void testConditionMatch() {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        MCRRoleCondition userCondition = new MCRRoleCondition();
        userCondition.parse(new Element("role").setText("editor"));
        Assert.assertTrue("User should have editor role", userCondition.matches(holder));
    }

    @Test
    public void testConditionNotMatch() {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        MCRRoleCondition userCondition = new MCRRoleCondition();
        userCondition.parse(new Element("role").setText("editor"));
        Assert.assertFalse("User should not have editor role", userCondition.matches(holder));
    }
}
