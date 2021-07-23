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

import java.util.Collections;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;

public class MCRIPConditionTest extends MCRTestCase {

    @Test
    public void testConditionMatch() {
        MCRSessionMgr.getCurrentSession().setCurrentIP("111.111.111.111");
        MCRIPCondition condition = new MCRIPCondition();
        condition.parse(new Element("ip").setText("111.111.111.111"));
        MCRFactsHolder facts = new MCRFactsHolder(Collections.emptyList());
        Assert.assertTrue("The ip should match", condition.matches(facts));
    }

    @Test
    public void testConditionNotMatch() {
        MCRSessionMgr.getCurrentSession().setCurrentIP("222.222.222.222");
        MCRIPCondition condition = new MCRIPCondition();
        condition.parse(new Element("ip").setText("111.111.111.111"));
        MCRFactsHolder facts = new MCRFactsHolder(Collections.emptyList());
        Assert.assertFalse("The ip should not match", condition.matches(facts));
    }

}
