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

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mycore.access.facts.condition.fact.MCRFactsTestUtil.hackObjectIntoCache;

public class MCRCategoryConditionTest extends MCRJPATestCase {

    MCRCategoryImpl clazz2;
    MCRCategoryImpl clazz1;


    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRCategoryDAO instance = MCRCategoryDAOFactory.getInstance();
        MCRCategoryImpl clazz = new MCRCategoryImpl();
        clazz.setRootID("clazz");
        clazz.setRootID("clazz");

        clazz1 = new MCRCategoryImpl();
        clazz1.setRootID("clazz");
        clazz1.setCategID("clazz1");

        clazz2 = new MCRCategoryImpl();
        clazz2.setRootID("clazz");
        clazz2.setCategID("clazz2");

        instance.addCategory(null, clazz);
        instance.addCategory(clazz.getId(), clazz1);
        instance.addCategory(clazz.getId(), clazz2);

    }

    @Test
    public void testConditionMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());

        MCRObject object = new MCRObject();
        MCRObjectID testId = MCRObjectID.getInstance("test_test_00000001");

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        Collection<MCRCategoryID> collect = Stream.of(clazz1.getId()).collect(Collectors.toList());
        MCRCategLinkServiceFactory.getInstance().setLinks(new MCRCategLinkReference(testId), collect);

        MCRCategoryCondition categoryCondition = new MCRCategoryCondition();

        categoryCondition.parse(new Element("classification").setText("clazz:clazz1"));
        Assert.assertTrue("Object should be linked with clazz1",
                categoryCondition.matches(holder));

    }

    @Test
    public void testConditionNotMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());

        MCRObject object = new MCRObject();
        MCRObjectID testId = MCRObjectID.getInstance("test_test_00000001");

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        Collection<MCRCategoryID> collect = Stream.of(clazz1.getId()).collect(Collectors.toList());
        MCRCategLinkServiceFactory.getInstance().setLinks(new MCRCategLinkReference(testId), collect);

        MCRCategoryCondition categoryCondition = new MCRCategoryCondition();

        categoryCondition.parse(new Element("classification").setText("clazz:clazz2"));
        Assert.assertFalse("Object not should be linked with clazz2",
                categoryCondition.matches(holder));

    }

}