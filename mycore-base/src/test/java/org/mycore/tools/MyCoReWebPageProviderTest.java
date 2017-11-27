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

package org.mycore.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MyCoReWebPageProviderTest extends MCRTestCase {

    @Test
    public void addSection() throws Exception {
        MyCoReWebPageProvider wp = new MyCoReWebPageProvider();
        // simple test
        Element content = new Element("content");
        content.setText("sample text");
        Element section = wp.addSection("title1", content, "de");
        assertNotNull(section);
        assertEquals("title1", section.getAttributeValue("title"));
        assertEquals("de", section.getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("sample text", section.getChild("content").getText());
        // xml text surrounded by p-tag
        Element section2 = wp.addSection("title2", "<p>text test</p>", "en");
        assertEquals("title2", section2.getAttributeValue("title"));
        assertEquals("en", section2.getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("text test", section2.getChild("p").getText());
        // only text
        Element section3 = wp.addSection("title3", "simple text", "uk");
        assertEquals("simple text", section3.getText());
        // multi tags
        Element section4 = wp.addSection("title4", "<b>bold</b> <i>italic</i>", "at");
        assertEquals(2, section4.getChildren().size());
        // check section count
        assertEquals(4, wp.getXML().getRootElement().getContentSize());
        // entities
        wp.addSection("title5", "&nbsp;&amp;&auml;Hallo", "de");
        // custom tags
        wp.addSection("title6", "<p><printlatestobjects /></p>", "de");
    }

    @Test
    public void updateMeta() throws Exception {
        MyCoReWebPageProvider wp = new MyCoReWebPageProvider();
        wp.updateMeta("myId", "myPath");
        Element rootElement = wp.getXML().getRootElement();
        Element meta = rootElement.getChild("meta");
        assertNotNull(meta);
        Element log = meta.getChild("log");
        assertNotNull(log);
        assertNotNull(log.getAttribute("date"));
        assertNotNull(log.getAttribute("time"));
        assertEquals("myId", log.getAttributeValue("lastEditor"));
        assertEquals("myPath", log.getAttributeValue("labelPath"));
    }
}
