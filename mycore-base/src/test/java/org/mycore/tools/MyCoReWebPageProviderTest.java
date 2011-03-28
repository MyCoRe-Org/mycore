package org.mycore.tools;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.jdom.Element;
import org.jdom.Namespace;
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
        // check section count
        assertEquals(3, wp.getXML().getRootElement().getContentSize());
    }

}
