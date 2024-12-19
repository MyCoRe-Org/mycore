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

package org.mycore.frontend.xeditor.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXMLHelper;

public class MCRChangeTrackerTest extends MCRTestCase {

    private Element buildElement(String template) throws JaxenException {
        return new MCRNodeBuilder().buildElement(template, null, null);
    }

    @Test
    public void testAddElement() throws JaxenException {
        Element root = buildElement("document[title][title[2]]");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = new Element("title");
        root.getChildren().add(1, title);
        tracker.track(new MCRAddedElement(title));

        assertEquals(3, root.getChildren().size());
        assertTrue(root.getChildren().contains(title));

        tracker.undoChanges();

        assertEquals(2, root.getChildren().size());
        assertFalse(root.getChildren().contains(title));
    }

    @Test
    public void testRemoveElement() throws JaxenException {
        Element root = buildElement("document[title][title[2][@type='main'][subTitle]][title[3]]");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = root.getChildren().get(1);
        tracker.track(new MCRRemoveElement(title));

        assertEquals(2, root.getChildren().size());
        assertFalse(root.getChildren().contains(title));

        tracker.undoChanges();

        assertEquals(3, root.getChildren().size());
        assertEquals("main", root.getChildren().get(1).getAttributeValue("type"));
        assertNotNull(root.getChildren().get(1).getChild("subTitle"));
    }

    @Test
    public void testAddAttribute() throws JaxenException {
        Element root = buildElement("document[title]");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = new Attribute("id", "foo");
        root.setAttribute(id);
        tracker.track(new MCRAddedAttribute(id));

        tracker.undoChanges();

        assertNull(root.getAttribute("id"));
    }

    @Test
    public void testRemoveAttribute() throws JaxenException {
        Element root = buildElement("document[@id='foo']");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = root.getAttribute("id");
        tracker.track(new MCRRemoveAttribute(id));
        assertNull(root.getAttribute("id"));

        tracker.undoChanges();

        assertEquals("foo", root.getAttributeValue("id"));
    }

    @Test
    public void testSetAttributeValue() throws JaxenException {
        Element root = buildElement("document[@id='foo']");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = root.getAttribute("id");
        tracker.track(new MCRSetAttributeValue(id, "bar"));

        assertEquals("bar", id.getValue());

        tracker.undoChanges();

        assertEquals("foo", root.getAttributeValue("id"));
    }

    @Test
    public void testSetElementText() throws JaxenException {
        Element root = buildElement("document[@id='foo'][titles/title][author]");
        MCRChangeTracker tracker = new MCRChangeTracker();

        tracker.track(new MCRSetElementText(root, "text"));
        assertEquals("text", root.getText());
        assertEquals("foo", root.getAttributeValue("id"));

        tracker.undoChanges();

        assertEquals("foo", root.getAttributeValue("id"));
        assertEquals(2, root.getChildren().size());
        assertEquals("titles", root.getChildren().get(0).getName());
        assertEquals("author", root.getChildren().get(1).getName());
        assertEquals("", root.getText());
    }

    @Test
    public void testCompleteUndo() throws JaxenException {
        String template = "document[titles[title][title[2]]][authors/author[first='John'][last='Doe']]";
        Element root = buildElement(template);
        Element before = root.clone();

        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = new Element("title").setAttribute("type", "alternative");
        root.getChild("titles").addContent(2, title);
        tracker.track(new MCRAddedElement(title));

        Attribute lang = new Attribute("lang", "de");
        root.setAttribute(lang);
        tracker.track(new MCRAddedAttribute(lang));

        Element author = root.getChild("authors").getChild("author");
        tracker.track(new MCRRemoveElement(author));

        tracker.undoChanges();

        assertTrue(MCRXMLHelper.deepEqual(before, root));
    }

    @Test
    public void testNestedChanges() {
        Element root = new Element("root");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = new Element("title");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        Attribute type = new Attribute("type", "main");
        title.setAttribute(type);
        tracker.track(new MCRAddedAttribute(type));

        Element part = new Element("part");
        title.addContent(part);
        tracker.track(new MCRAddedElement(part));

        tracker.track(new MCRRemoveElement(part));
        tracker.track(new MCRRemoveAttribute(type));
        tracker.track(new MCRRemoveElement(title));

        tracker.undoChanges();

        Element expected = new Element("root");
        assertTrue(MCRXMLHelper.deepEqual(expected, root));
    }

    @Test
    public void testNamespaces() {
        Element root = new Element("root");
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute href = new Attribute("href", "foo", MCRConstants.XLINK_NAMESPACE);
        root.setAttribute(href);
        tracker.track(new MCRAddedAttribute(href));

        tracker.track(new MCRSetAttributeValue(href, "bar"));

        tracker.track(new MCRRemoveAttribute(href));
        tracker.undoLastChange();

        assertEquals("bar", root.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));
        tracker.undoChanges();

        assertNull(root.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));

        Element title = new Element("title", MCRConstants.MODS_NAMESPACE).setText("foo");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        tracker.track(new MCRSetElementText(title, "bar"));
        tracker.undoLastChange();
        assertEquals("foo", root.getChild("title", MCRConstants.MODS_NAMESPACE).getText());

        tracker.track(new MCRRemoveElement(title));
        tracker.undoLastChange();

        assertNotNull(root.getChild("title", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    public void testBreakpoint() {
        Element root = new Element("root");

        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = new Element("title");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        tracker.track(new MCRBreakpoint("Test"));

        Attribute href = new Attribute("href", "foo", MCRConstants.XLINK_NAMESPACE);
        root.setAttribute(href);
        tracker.track(new MCRAddedAttribute(href));

        tracker.track(new MCRSetAttributeValue(href, "bar"));

        String label = tracker.undoLastBreakpoint();
        assertEquals("Test", label);
        assertNull(root.getAttribute("href"));
        assertEquals(1, tracker.getChangeCount());
    }

    @Test
    public void testSwapElements() throws JaxenException {
        Element root
            = new MCRNodeBuilder().buildElement("parent[name='a'][note][foo][name[2]='b'][note[2]]", null, null);

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());

        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.track(new MCRSwapElements(root, 0, 3));

        assertEquals("b", root.getChildren().get(0).getText());
        assertEquals("a", root.getChildren().get(3).getText());

        tracker.undoChanges();

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());
    }
}
