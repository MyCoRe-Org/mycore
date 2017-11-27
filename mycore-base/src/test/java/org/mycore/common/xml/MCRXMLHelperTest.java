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

package org.mycore.common.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import com.google.gson.JsonObject;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRXMLHelperTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.common.xml.MCRXMLHelper#deepEqual(org.jdom2.Element, org.jdom2.Element)}.
     */
    @Test
    public void testDeepEqualElementElement() {
        assertTrue("Elements should be equal", MCRXMLHelper.deepEqual(getSmallElement(), getSmallElement()));
        assertTrue("Elements should be equal", MCRXMLHelper.deepEqual(getBigElement(), getBigElement()));
        assertFalse("Elements should be different", MCRXMLHelper.deepEqual(getSmallElement(), getBigElement()));
        assertFalse("Elements should be different",
            MCRXMLHelper.deepEqual(getSmallElement().setAttribute("j", "junit"), getSmallElement()));
        assertFalse("Elements should be different", MCRXMLHelper.deepEqual(getBigElement(), getSmallElement()));
    }

    private static Element getSmallElement() {
        Element elm = new Element("test");
        elm.setAttribute("j", "unit");
        elm.setAttribute("junit", "test");
        elm.addContent(new Element("junit"));
        return elm;
    }

    private static Element getBigElement() {
        Element elm = getSmallElement();
        elm.addContent(new Element("junit"));
        return elm;
    }

    @Test
    public void testList() throws Exception {
        Element child1 = new Element("child").setText("Hallo Welt");
        Element child2 = new Element("child").setText("hello world");
        Element child3 = new Element("child").setText("Bonjour le monde");
        List<Content> l1 = new ArrayList<>();
        l1.add(child1);
        l1.add(child2);
        l1.add(child3);
        Element root = new Element("root");
        root.addContent(l1);

        String formattedXML = "<root>\n<child>Hallo Welt</child>\n" + "<child>hello world</child>"
            + "<child>Bonjour le monde</child>\n</root>";
        SAXBuilder b = new SAXBuilder();
        Document doc = b.build(new ByteArrayInputStream(formattedXML.getBytes(Charset.forName("UTF-8"))));

        assertEquals("Elements should be equal", true, MCRXMLHelper.deepEqual(root, doc.getRootElement()));
    }

    @Test
    public void jsonSerialize() throws Exception {
        // simple text
        Element e = new Element("hallo").setText("Hallo Welt");
        JsonObject json = MCRXMLHelper.jsonSerialize(e);
        assertEquals("Hallo Welt", json.getAsJsonPrimitive("$text").getAsString());
        // attribute
        e = new Element("hallo").setAttribute("hallo", "welt");
        json = MCRXMLHelper.jsonSerialize(e);
        assertEquals("welt", json.getAsJsonPrimitive("_hallo").getAsString());

        // complex world class test
        URL world = MCRXMLHelperTest.class.getResource("/worldclass.xml");
        SAXBuilder builder = new SAXBuilder();
        Document worldDocument = builder.build(world.openStream());
        json = MCRXMLHelper.jsonSerialize(worldDocument.getRootElement());
        assertNotNull(json);
        assertEquals("World", json.getAsJsonPrimitive("_ID").getAsString());
        assertEquals("http://www.w3.org/2001/XMLSchema-instance", json.getAsJsonPrimitive("_xmlns:xsi").getAsString());
        JsonObject deLabel = json.getAsJsonArray("label").get(0).getAsJsonObject();
        assertEquals("de", deLabel.getAsJsonPrimitive("_xml:lang").getAsString());
        assertEquals("Staaten", deLabel.getAsJsonPrimitive("_text").getAsString());
        assertEquals(2, json.getAsJsonObject("categories").getAsJsonArray("category").size());
    }

}
