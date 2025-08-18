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

package org.mycore.frontend.xeditor.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRXlink;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRChangeTrackerTest {

    private Element root;

    private MCRChangeTracker tracker;

    @Test
    public void testAddElement() throws JaxenException {
        buildTestElement("document[title][title[2]]");

        Element title = new Element("title");
        root.getChildren().add(1, title);
        assertEquals(3, root.getChildren().size());
        assertTrue(root.getChildren().contains(title));

        tracker.track(new MCRAddedElement(title));
        tracker.undoChanges();

        assertEquals(2, root.getChildren().size());
        assertFalse(root.getChildren().contains(title));
    }

    @Test
    public void testRemoveElement() throws JaxenException {
        buildTestElement("document[title][title[2][@type='main'][subTitle]][title[3]]");

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
        buildTestElement("document[title]");

        Attribute id = new Attribute("id", "foo");
        root.setAttribute(id);
        tracker.track(new MCRAddedAttribute(id));

        tracker.undoChanges();

        assertNull(root.getAttribute("id"));
    }

    @Test
    public void testRemoveAttribute() throws JaxenException {
        buildTestElement("document[@id='foo']");

        Attribute id = root.getAttribute("id");
        tracker.track(new MCRRemoveAttribute(id));
        assertNull(root.getAttribute("id"));

        tracker.undoChanges();

        assertEquals("foo", root.getAttributeValue("id"));
    }

    @Test
    public void testSetAttributeValue() throws JaxenException {
        buildTestElement("document[@id='foo']");

        Attribute id = root.getAttribute("id");
        tracker.track(new MCRSetAttributeValue(id, "bar"));
        assertEquals("bar", id.getValue());

        tracker.undoChanges();

        assertEquals("foo", root.getAttributeValue("id"));
    }

    @Test
    public void testSetElementText() throws JaxenException {
        buildTestElement("document[@id='foo'][titles/title][author]");

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
        buildTestElement("document[titles[title][title[2]]][authors/author[first='John'][last='Doe']]");

        Element before = root.clone();

        Element titles = root.getChild("titles");
        Element title = new Element("title").setAttribute("type", "alternative");
        titles.addContent(2, title);
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
    public void testNestedChanges() throws JaxenException {
        buildTestElement("root");

        Element title = new Element("title");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        Attribute id = new Attribute("type", "main");
        title.setAttribute(id);
        tracker.track(new MCRAddedAttribute(id));

        Element part = new Element("part");
        title.addContent(part);
        tracker.track(new MCRAddedElement(part));

        tracker.track(new MCRRemoveElement(part));
        tracker.track(new MCRRemoveAttribute(id));
        tracker.track(new MCRRemoveElement(title));

        tracker.undoChanges();
    }

    @Test
    public void testBreakpoint() throws JaxenException {
        buildTestElement("root");

        Element title = new Element("title");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        tracker.track(new MCRBreakpoint("Test"));

        Attribute href = new Attribute(MCRXlink.HREF, "foo", MCRConstants.XLINK_NAMESPACE);
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
        buildTestElement("parent[name='a'][note][foo][name[2]='b'][note[2]]");

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());

        tracker.track(new MCRSwapElements(root, 0, 3));

        assertEquals("b", root.getChildren().get(0).getText());
        assertEquals("a", root.getChildren().get(3).getText());

        tracker.undoChanges();

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());
    }

    private void buildTestElement(String template) throws JaxenException {
        this.root = new MCRNodeBuilder().buildElement(template, null, null);
        this.tracker = new MCRChangeTracker();
        this.tracker.setEditedXML(new Document(root));
    }
}
