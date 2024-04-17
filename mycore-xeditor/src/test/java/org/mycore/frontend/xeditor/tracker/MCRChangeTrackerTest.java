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
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.frontend.xeditor.MCRBinding;

public class MCRChangeTrackerTest extends MCRTestCase {

    @Test
    public void testAddElement() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[title][title[2]]", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = new Element("title");
        doc.getRootElement().getChildren().add(1, title);
        assertEquals(3, doc.getRootElement().getChildren().size());
        assertTrue(doc.getRootElement().getChildren().contains(title));

        tracker.track(new MCRAddedElement(title));
        tracker.undoChanges(doc);

        assertEquals(2, doc.getRootElement().getChildren().size());
        assertFalse(doc.getRootElement().getChildren().contains(title));
    }

    @Test
    public void testRemoveElement() throws JaxenException {
        String template = "document[title][title[2][@type='main'][subTitle]][title[3]]";
        Document doc = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = doc.getRootElement().getChildren().get(1);
        tracker.track(new MCRRemoveElement(title));
        assertEquals(2, doc.getRootElement().getChildren().size());
        assertFalse(doc.getRootElement().getChildren().contains(title));

        tracker.undoChanges(doc);

        assertEquals(3, doc.getRootElement().getChildren().size());
        assertEquals("main", doc.getRootElement().getChildren().get(1).getAttributeValue("type"));
        assertNotNull(doc.getRootElement().getChildren().get(1).getChild("subTitle"));
    }

    @Test
    public void testAddAttribute() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[title]", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = new Attribute("id", "foo");
        doc.getRootElement().setAttribute(id);
        tracker.track(new MCRAddedAttribute(id));

        tracker.undoChanges(doc);

        assertNull(doc.getRootElement().getAttribute("id"));
    }

    @Test
    public void testRemoveAttribute() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo']", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = doc.getRootElement().getAttribute("id");
        tracker.track(new MCRRemoveAttribute(id));
        assertNull(doc.getRootElement().getAttribute("id"));

        tracker.undoChanges(doc);

        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
    }

    @Test
    public void testSetAttributeValue() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo']", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = doc.getRootElement().getAttribute("id");
        tracker.track(new MCRSetAttributeValue(id, "bar"));
        assertEquals("bar", id.getValue());

        tracker.undoChanges(doc);

        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
    }

    @Test
    public void testSetElementText() throws JaxenException {
        Document doc = new Document(
            new MCRNodeBuilder().buildElement("document[@id='foo'][titles/title][author]", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        tracker.track(new MCRSetElementText(doc.getRootElement(), "text"));
        assertEquals("text", doc.getRootElement().getText());
        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));

        tracker.undoChanges(doc);
        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
        assertEquals(2, doc.getRootElement().getChildren().size());
        assertEquals("titles", doc.getRootElement().getChildren().get(0).getName());
        assertEquals("author", doc.getRootElement().getChildren().get(1).getName());
        assertEquals("", doc.getRootElement().getText());
    }

    @Test
    public void testCompleteUndo() throws JaxenException {
        String template = "document[titles[title][title[2]]][authors/author[first='John'][last='Doe']]";
        Document doc = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        Document before = doc.clone();

        MCRChangeTracker tracker = new MCRChangeTracker();

        Element titles = (Element) (new MCRBinding("document/titles", true, new MCRBinding(doc)).getBoundNode());
        Element title = new Element("title").setAttribute("type", "alternative");
        titles.addContent(2, title);
        tracker.track(new MCRAddedElement(title));

        Attribute lang = new Attribute("lang", "de");
        doc.getRootElement().setAttribute(lang);
        tracker.track(new MCRAddedAttribute(lang));

        Element author = (Element) (new MCRBinding("document/authors/author", true, new MCRBinding(doc))
            .getBoundNode());
        tracker.track(new MCRRemoveElement(author));

        tracker.undoChanges(doc);

        assertTrue(MCRXMLHelper.deepEqual(before, doc));
    }

    @Test
    public void testEscaping() {
        String pattern = "<?xed-foo ?>";
        Document doc = new Document(new Element("document").addContent(new Element("child").setText(pattern)));
        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.track(new MCRRemoveElement(doc.getRootElement().getChildren().get(0)));
        tracker.undoChanges(doc);
        assertEquals(pattern, doc.getRootElement().getChildren().get(0).getText());
    }

    @Test
    public void testNestedChanges() {
        Element root = new Element("root");
        Document doc = new Document(root);
        MCRChangeTracker tracker = new MCRChangeTracker();

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

        tracker.undoChanges(doc);
    }

    @Test
    public void testNamespaces() {
        Element root = new Element("root");
        Document doc = new Document(root);
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute href = new Attribute("href", "foo", MCRConstants.XLINK_NAMESPACE);
        root.setAttribute(href);
        tracker.track(new MCRAddedAttribute(href));

        tracker.track(new MCRSetAttributeValue(href, "bar"));

        tracker.track(new MCRRemoveAttribute(href));
        tracker.undoLastChange(doc);

        assertEquals("bar", root.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));
        tracker.undoChanges(doc);

        assertNull(root.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));

        Element title = new Element("title", MCRConstants.MODS_NAMESPACE).setText("foo");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        tracker.track(new MCRSetElementText(title, "bar"));
        tracker.undoLastChange(doc);
        assertEquals("foo", root.getChild("title", MCRConstants.MODS_NAMESPACE).getText());

        tracker.track(new MCRRemoveElement(title));
        tracker.undoLastChange(doc);

        assertNotNull(root.getChild("title", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    public void testBreakpoint() {
        Element root = new Element("root");
        Document doc = new Document(root);

        MCRChangeTracker tracker = new MCRChangeTracker();

        Element title = new Element("title");
        root.addContent(title);
        tracker.track(new MCRAddedElement(title));

        tracker.track(new MCRBreakpoint("Test"));

        Attribute href = new Attribute("href", "foo", MCRConstants.XLINK_NAMESPACE);
        root.setAttribute(href);
        tracker.track(new MCRAddedAttribute(href));

        tracker.track(new MCRSetAttributeValue(href, "bar"));

        String label = tracker.undoLastBreakpoint(doc);
        assertEquals("Test", label);
        assertNull(root.getAttribute("href"));
        assertEquals(1, tracker.getChangeCount());
    }

    @Test
    public void testSwapElements() throws JaxenException {
        Element root = new MCRNodeBuilder().buildElement("parent[name='a'][note][foo][name[2]='b'][note[2]]", null,
            null);
        Document doc = new Document(root);

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());

        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.track(new MCRSwapElements(root, 0, 3));

        assertEquals("b", root.getChildren().get(0).getText());
        assertEquals("a", root.getChildren().get(3).getText());

        tracker.undoChanges(doc);

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());
    }
}
