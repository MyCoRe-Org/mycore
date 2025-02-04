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

package org.mycore.wcms2;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.wcms2.navigation.MCRWCMSDefaultSectionProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MCRWCMSDefaultSectionProviderTest {

    @Test
    public void toJSON() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File("src/test/resources/navigation/content.xml"));
        MCRWCMSDefaultSectionProvider provider = new MCRWCMSDefaultSectionProvider();
        JsonArray sectionArray = provider.toJSON(doc.getRootElement());

        assertEquals(2, sectionArray.size());
        // test section one
        JsonObject section1 = (JsonObject) sectionArray.get(0);
        assertEquals("Title one", section1.getAsJsonPrimitive("title").getAsString());
        assertEquals("de", section1.getAsJsonPrimitive("lang").getAsString());
        assertEquals("<p>Content one</p><br />", section1.getAsJsonPrimitive("data").getAsString());
        // test section two
        JsonObject section2 = (JsonObject) sectionArray.get(1);
        assertEquals("Title two", section2.getAsJsonPrimitive("title").getAsString());
        assertEquals("en", section2.getAsJsonPrimitive("lang").getAsString());
        assertEquals("Content two", section2.getAsJsonPrimitive("data").getAsString());
    }

}
