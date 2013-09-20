package org.mycore.frontend.xeditor;

import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.mycore.common.MCRTestCase;
import static org.junit.Assert.*;

import org.junit.Test;
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

        tracker.track(MCRChangeTracker.ADD_ELEMENT.added(title));
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
        tracker.track(MCRChangeTracker.REMOVE_ELEMENT.remove(title));
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
        tracker.track(MCRChangeTracker.ADD_ATTRIBUTE.added(id));

        tracker.undoChanges(doc);

        assertNull(doc.getRootElement().getAttribute("id"));
    }

    @Test
    public void testRemoveAttribute() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo']", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = doc.getRootElement().getAttribute("id");
        tracker.track(MCRChangeTracker.REMOVE_ATTRIBUTE.remove(id));
        assertNull(doc.getRootElement().getAttribute("id"));

        tracker.undoChanges(doc);

        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
    }

    @Test
    public void testSetAttributeValue() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo']", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        Attribute id = doc.getRootElement().getAttribute("id");
        tracker.track(MCRChangeTracker.SET_ATTRIBUTE_VALUE.set(id, "bar"));
        assertEquals("bar", id.getValue());

        tracker.undoChanges(doc);

        assertEquals("foo", doc.getRootElement().getAttributeValue("id"));
    }

    @Test
    public void testSetElementText() throws JaxenException {
        Document doc = new Document(new MCRNodeBuilder().buildElement("document[@id='foo'][titles/title][author]", null, null));
        MCRChangeTracker tracker = new MCRChangeTracker();

        tracker.track(MCRChangeTracker.SET_TEXT.set(doc.getRootElement(), "text"));
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

        MCRChangeTracker changes = new MCRChangeTracker();

        Element titles = (Element) (new MCRBinding("document/titles", new MCRBinding(doc)).getBoundNode());
        Element title = new Element("title").setAttribute("type", "alternative");
        titles.addContent(2, title);
        changes.track(MCRChangeTracker.ADD_ELEMENT.added(title));

        Attribute lang = new Attribute("lang", "de");
        doc.getRootElement().setAttribute(lang);
        changes.track(MCRChangeTracker.ADD_ATTRIBUTE.added(lang));

        Element author = (Element) (new MCRBinding("document/authors/author", new MCRBinding(doc)).getBoundNode());
        changes.track(MCRChangeTracker.REMOVE_ELEMENT.remove(author));

        changes.undoChanges(doc);

        assertTrue(MCRXMLHelper.deepEqual(before, doc));
    }

    @Test
    public void testRemoveChangeTracking() throws JaxenException, JDOMException {
        String template = "document[titles[title][title[2]]][authors/author[first='John'][last='Doe']]";
        Document doc = new Document(new MCRNodeBuilder().buildElement(template, null, null));

        MCRChangeTracker changes = new MCRChangeTracker();

        Element titles = (Element) (new MCRBinding("document/titles", new MCRBinding(doc)).getBoundNode());
        Element title = new Element("title").setAttribute("type", "alternative");
        titles.addContent(2, title);
        changes.track(MCRChangeTracker.ADD_ELEMENT.added(title));

        Attribute lang = new Attribute("lang", "de");
        doc.getRootElement().setAttribute(lang);
        changes.track(MCRChangeTracker.ADD_ATTRIBUTE.added(lang));

        Element author = (Element) (new MCRBinding("document/authors/author", new MCRBinding(doc)).getBoundNode());
        changes.track(MCRChangeTracker.REMOVE_ELEMENT.remove(author));

        MCRChangeTracker.removeChangeTracking(doc);
        assertFalse(doc.getDescendants(Filters.processinginstruction()).iterator().hasNext());
    }

    @Test
    public void testEscaping() {
        String pattern = "<?xed-foo ?>";
        Document doc = new Document(new Element("document").addContent(new Element("child").setText(pattern)));
        MCRChangeTracker changes = new MCRChangeTracker();
        changes.track(MCRChangeTracker.REMOVE_ELEMENT.remove(doc.getRootElement().getChildren().get(0)));
        changes.undoChanges(doc);
        assertEquals(pattern, doc.getRootElement().getChildren().get(0).getText());
    }
}
