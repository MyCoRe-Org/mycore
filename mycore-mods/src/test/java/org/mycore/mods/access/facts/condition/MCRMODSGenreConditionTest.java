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

package org.mycore.mods.access.facts.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.access.facts.condition.fact.MCRFactsTestUtil.hackObjectIntoCache;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.mods", string = "true")
})
public class MCRMODSGenreConditionTest {

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

        Element genre = mw.addElement("genre");
        genre.setAttribute("valueURI", "https://mycore.de/classifications/mir_genres#article");

        MCRMODSGenreCondition genreCondition = new MCRMODSGenreCondition();
        genreCondition.parse(new Element("genre").setText("article"));
        assertTrue(genreCondition.matches(holder), "object should be in genre article");
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

        Element genre = mw.addElement("genre");
        genre.setAttribute("valueURI", "https://mycore.de/classifications/mir_genres#book");

        MCRMODSGenreCondition genreCondition = new MCRMODSGenreCondition();
        genreCondition.parse(new Element("genre").setText("article"));
        assertFalse(genreCondition.matches(holder), "object should not be in genre article");
    }

}
