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

package org.mycore.datamodel.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.test.MyCoReTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRObjectTest {

    private static final String TEST_OBJECT_RESOURCE_NAME = "/mcr_test_01.xml";

    private MCRObject testObject;

    @BeforeEach
    public void setUp() throws Exception {
        String testId = "junit_test_00000001";
        if (MCRObjectID.getInstance(testId).toString().length() != testId.length()) {
            MCRObjectIDTest.resetObjectIDFormat();
        }
        // create doc
        Document testObjectDocument = loadResourceDocument(TEST_OBJECT_RESOURCE_NAME);
        testObject = new MCRObject();
        testObject.setFromJDOM(testObjectDocument);
    }

    @Test
    public void createJSON() {
        JsonObject json = testObject.createJSON();
        assertEquals("mcr_test_00000001", json.getAsJsonPrimitive("id").getAsString(), "Invalid id");
        JsonObject textfield = json.getAsJsonObject("metadata").getAsJsonObject("def.textfield");
        String text = textfield.getAsJsonArray("data").get(0).getAsJsonObject().getAsJsonPrimitive("text")
            .getAsString();
        assertEquals("JUnit Test object 1", text, "Invalid text metadata");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(json));
    }

    private static Document loadResourceDocument(String resource) throws MCRException, IOException, JDOMException {
        URL mcrTestUrl = MCRObjectMetadataTest.class.getResource(resource);
        return MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(mcrTestUrl));
    }

}
