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

import static org.mycore.access.facts.condition.fact.MCRFactsTestUtil.hackObjectIntoCache;

import java.util.ArrayList;
import java.util.Map;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRCreatedByConditionTest extends MCRTestCase {

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test
    public void testConditionMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());

        MCRObject object = new MCRObject();
        object.getService().addFlag("createdby", "administrator");
        MCRObjectID testId = MCRObjectID.getInstance("test_test_00000001");

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        MCRCreatedByCondition createdByCondition = new MCRCreatedByCondition();
        createdByCondition.parse(new Element("createdby"));

        Assert.assertTrue("current user should be creator", createdByCondition.matches(holder));
    }

    @Test
    public void testNotMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());

        MCRObject object = new MCRObject();
        object.getService().addFlag("createdby", "administrator");
        MCRObjectID testId = MCRObjectID.getInstance("test_test_00000001");
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        MCRCreatedByCondition createdByCondition = new MCRCreatedByCondition();
        createdByCondition.parse(new Element("createdby"));

        Assert.assertFalse("current user should not be the creator", createdByCondition.matches(holder));
    }

}
