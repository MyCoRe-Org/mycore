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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRVFSContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.xml.sax.SAXParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class MCRObjectTest extends MCRTestCase {

    private static final String TEST_OBJECT_RESOURCE_NAME = "/mcr_test_01.xml";

    private MCRObject testObject;

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create doc
        Document testObjectDocument = loadResourceDocument(TEST_OBJECT_RESOURCE_NAME);
        testObject = new MCRObject();
        testObject.setFromJDOM(testObjectDocument);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test
    public void createJSON() throws Exception {
        JsonObject json = testObject.createJSON();
        assertEquals("Invalid id", "mcr_test_00000001", json.getAsJsonPrimitive("id").getAsString());
        JsonObject textfield = json.getAsJsonObject("metadata").getAsJsonObject("def.textfield");
        String text = textfield.getAsJsonArray("data").get(0).getAsJsonObject().getAsJsonPrimitive("text")
            .getAsString();
        assertEquals("Invalid text metadata", "JUnit Test object 1", text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(json));
    }

    private static Document loadResourceDocument(String resource) throws MCRException, SAXParseException, IOException {
        URL mcrTestUrl = MCRObjectMetadataTest.class.getResource(resource);
        return MCRXMLParserFactory.getValidatingParser().parseXML(new MCRVFSContent(mcrTestUrl));
    }

}
