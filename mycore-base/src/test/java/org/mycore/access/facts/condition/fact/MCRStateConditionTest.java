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
import static org.mycore.access.facts.condition.fact.MCRFactsTestUtil.hackObjectIntoCache;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRStateConditionTest {

    @BeforeEach
    public void setUp() throws Exception {

        MCRCategoryDAO instance = MCRCategoryDAOFactory.obtainInstance();
        MCRCategoryImpl state = new MCRCategoryImpl();
        state.setRootID("state");
        state.setRootID("state");

        MCRCategoryImpl newState = new MCRCategoryImpl();
        newState.setRootID("state");
        newState.setCategID("new");

        MCRCategoryImpl published = new MCRCategoryImpl();
        published.setRootID("state");
        published.setCategID("published");

        instance.addCategory(null, state);
        instance.addCategory(state.getId(), newState);
        instance.addCategory(state.getId(), published);
    }

    @Test
    public void testConditionMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);

        MCRObject object = new MCRObject();
        object.getService().setState("published");
        MCRObjectID testId = MCRObjectID.getInstance("test_test_00000001");

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        MCRStateCondition mcrStateCondition = new MCRStateCondition();
        mcrStateCondition.parse(new Element("state").setText("published"));
        assertTrue(mcrStateCondition.matches(holder), "State should be 'published'!");
    }

    @Test
    public void testConditionNotMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);

        MCRObject object = new MCRObject();
        object.getService().setState("published");
        MCRObjectID testId = MCRObjectID.getInstance("test_test_00000001");

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        MCRStateCondition mcrStateCondition = new MCRStateCondition();
        mcrStateCondition.parse(new Element("state").setText("new"));
        assertFalse(mcrStateCondition.matches(holder), "State should not be 'published'!");
    }
}
