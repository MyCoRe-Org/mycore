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
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
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

        tracker.track(MCRAddedElement.added(title));
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
        tracker.track(MCRRemoveElement.remove(title));
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
        tracker.track(MCRAddedAttribute.added(id));

        tracker.undoChanges(doc);

        assertNull(doc.getRootElement().getAttribute("id"));
    }

    @Test
    public void testRemoveAttribute() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo']", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = doc.getRootElement().getAttribute("id");
        tracker.track(MCRRemoveAttribute.remove(id));
        assertNull(doc.getRootElement().getAttribute("id"));

        tracker.undoChanges(doc);

        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
    }

    @Test
    public void testSetAttributeValue() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo']", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = doc.getRootElement().getAttribute("id");
        tracker.track(MCRSetAttributeValue.setValue(id, "bar"));
        assertEquals("bar", id.getValue());

        tracker.undoChanges(doc);

        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
    }

    @Test
    public void testSetElementText() throws JaxenException {
        Document doc = new Document(
            new MCRNodeBuilder().buildElement("document[@id='foo'][titles/title][author]", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        tracker.track(MCRSetElementText.setText(doc.getRootElement(), "text"));
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
    public void testCompleteUndo() throws JaxenException, JDOMException {
        String template = "document[titles[title][title[2]]][authors/author[first='John'][last='Doe']]";
        Document doc = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        Document before = doc.clone();

        MCRChangeTracker tracker = new MCRChangeTracker();

        Element titles = (Element) (new MCRBinding("document/titles", true, new MCRBinding(doc)).getBoundNode());
        Element title = new Element("title").setAttribute("type", "alternative");
        titles.addContent(2, title);
        tracker.track(MCRAddedElement.added(title));

        Attribute lang = new Attribute("lang", "de");
        doc.getRootElement().setAttribute(lang);
        tracker.track(MCRAddedAttribute.added(lang));

        Element author = (Element) (new MCRBinding("document/authors/author", true, new MCRBinding(doc))
            .getBoundNode());
        tracker.track(MCRRemoveElement.remove(author));

        tracker.undoChanges(doc);

        assertTrue(MCRXMLHelper.deepEqual(before, doc));
    }

    @Test
    public void testRemoveChangeTracking() throws JaxenException, JDOMException {
        String template = "document[titles[title][title[2]]][authors/author[first='John'][last='Doe']]";
        Document doc = new Document(new MCRNodeBuilder().buildElement(template, null, null));

        MCRChangeTracker tracker = new MCRChangeTracker();

        Element titles = (Element) (new MCRBinding("document/titles", true, new MCRBinding(doc)).getBoundNode());
        Element title = new Element("title").setAttribute("type", "alternative");
        titles.addContent(2, title);
        tracker.track(MCRAddedElement.added(title));

        Attribute lang = new Attribute("lang", "de");
        doc.getRootElement().setAttribute(lang);
        tracker.track(MCRAddedAttribute.added(lang));

        Element author = (Element) (new MCRBinding("document/authors/author", true, new MCRBinding(doc))
            .getBoundNode());
        tracker.track(MCRRemoveElement.remove(author));

        doc = MCRChangeTracker.removeChangeTracking(doc);
        assertFalse(doc.getDescendants(Filters.processinginstruction()).iterator().hasNext());
    }

    @Test
    public void testEscaping() {
        String pattern = "<?xed-foo ?>";
        Document doc = new Document(new Element("document").addContent(new Element("child").setText(pattern)));
        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.track(MCRRemoveElement.remove(doc.getRootElement().getChildren().get(0)));
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
        tracker.track(MCRAddedElement.added(title));

        Attribute id = new Attribute("type", "main");
        title.setAttribute(id);
        tracker.track(MCRAddedAttribute.added(id));

        Element part = new Element("part");
        title.addContent(part);
        tracker.track(MCRAddedElement.added(part));

        tracker.track(MCRRemoveElement.remove(part));
        tracker.track(MCRRemoveAttribute.remove(id));
        tracker.track(MCRRemoveElement.remove(title));

        tracker.undoChanges(doc);
    }

    @Test
    public void testNamespaces() {
        Element root = new Element("root");
        Document doc = new Document(root);
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute href = new Attribute("href", "foo", MCRConstants.XLINK_NAMESPACE);
        root.setAttribute(href);
        tracker.track(MCRAddedAttribute.added(href));

        tracker.track(MCRSetAttributeValue.setValue(href, "bar"));

        tracker.track(MCRRemoveAttribute.remove(href));
        tracker.undoLastChange(doc);

        assertEquals("bar", root.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));
        tracker.undoChanges(doc);

        assertNull(root.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE));

        Element title = new Element("title", MCRConstants.MODS_NAMESPACE).setText("foo");
        root.addContent(title);
        tracker.track(MCRAddedElement.added(title));

        tracker.track(MCRSetElementText.setText(title, "bar"));
        tracker.undoLastChange(doc);
        assertEquals("foo", root.getChild("title", MCRConstants.MODS_NAMESPACE).getText());

        tracker.track(MCRRemoveElement.remove(title));
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
        tracker.track(MCRAddedElement.added(title));

        tracker.track(MCRBreakpoint.setBreakpoint(root, "Test"));

        Attribute href = new Attribute("href", "foo", MCRConstants.XLINK_NAMESPACE);
        root.setAttribute(href);
        tracker.track(MCRAddedAttribute.added(href));

        tracker.track(MCRSetAttributeValue.setValue(href, "bar"));

        String label = tracker.undoLastBreakpoint(doc);
        assertEquals("Test", label);
        assertNull(root.getAttribute("href"));
        assertEquals(1, tracker.getChangeCounter());
    }

    @Test
    public void testSwapElements() throws JaxenException {
        Element root = new MCRNodeBuilder().buildElement("parent[name='a'][note][foo][name[2]='b'][note[2]]", null,
            null);
        Document doc = new Document(root);

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());

        MCRChangeTracker tracker = new MCRChangeTracker();
        tracker.track(MCRSwapElements.swap(root, 0, 3));

        assertEquals("b", root.getChildren().get(0).getText());
        assertEquals("a", root.getChildren().get(3).getText());

        tracker.undoChanges(doc);

        assertEquals("a", root.getChildren().get(0).getText());
        assertEquals("b", root.getChildren().get(3).getText());
    }
}
