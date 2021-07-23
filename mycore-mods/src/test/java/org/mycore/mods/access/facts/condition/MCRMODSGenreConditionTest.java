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

package org.mycore.mods.access.facts.condition;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mycore.access.facts.condition.fact.MCRFactsTestUtil.hackObjectIntoCache;

public class MCRMODSGenreConditionTest extends MCRTestCase {

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.mods", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test
    public void testConditionMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRMODSWrapper mw = new MCRMODSWrapper();
        MCRObject object = mw.getMCRObject();
        mw.setMODS(new Element("mods", MCRConstants.MODS_NAMESPACE));
        MCRObjectID testId = MCRObjectID.getInstance("test_mods_00000001");
        object.setId(testId);

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        Element classification = mw.addElement("genre");
        classification.setAttribute("valueURI", "https://mycore.de/classifications/mir_genres#article");

        MCRMODSGenreCondition genreCondition = new MCRMODSGenreCondition();
        genreCondition.parse(new Element("genre").setText("article"));
        Assert.assertTrue("object should be in genre article",genreCondition.matches(holder));
    }

    @Test
    public void testConditionNotMatch() throws NoSuchFieldException, IllegalAccessException {
        MCRMODSWrapper mw = new MCRMODSWrapper();
        MCRObject object = mw.getMCRObject();
        mw.setMODS(new Element("mods", MCRConstants.MODS_NAMESPACE));
        MCRObjectID testId = MCRObjectID.getInstance("test_mods_00000001");
        object.setId(testId);

        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        hackObjectIntoCache(object, testId);
        holder.add(new MCRObjectIDFact("objid", testId.toString(), testId));

        Element classification = mw.addElement("genre");
        classification.setAttribute("valueURI", "https://mycore.de/classifications/mir_genres#book");

        MCRMODSGenreCondition genreCondition = new MCRMODSGenreCondition();
        genreCondition.parse(new Element("genre").setText("article"));
        Assert.assertFalse("object should not be in genre article",genreCondition.matches(holder));
    }

}