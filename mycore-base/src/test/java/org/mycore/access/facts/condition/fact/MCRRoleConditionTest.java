/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRRoleConditionTest {

    @Test
    public void testConditionMatch() {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        MCRRoleCondition userCondition = new MCRRoleCondition();
        userCondition.parse(new Element("role").setText("editor"));
        assertTrue(userCondition.matches(holder), "User should have editor role");
    }

    @Test
    public void testConditionNotMatch() {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.GUEST);
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        MCRRoleCondition userCondition = new MCRRoleCondition();
        userCondition.parse(new Element("role").setText("editor"));
        assertFalse(userCondition.matches(holder), "User should not have editor role");
    }
}
