package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.junit.Before;

public class MCREditorSessionTest {

    private static String defaultURL = "http://www.test.de";

    private Element xml;

    private MCRParameters parameters;

    @Before
    public void setup() {
        xml = new Element("editor");
        xml.addContent(new Element("source").setAttribute("uri", "mcrobject:{id}"));
        xml.addContent(new Element("cancel").setAttribute("url", defaultURL));

        parameters = new MCRParameters();
        parameters.addParameterValue("id", "4711");
    }

    @Test
    public void testAttributeTokenSubstitution() {
        MCREditorSession session = new MCREditorSession(xml, parameters);
        assertThat(session.getCancelURL(), equalTo(defaultURL));
        assertThat(session.getSourceURI(), equalTo("mcrobject:4711"));
    }

    @Test
    public void testUniqueID() {
        MCREditorSession session1 = new MCREditorSession(xml, parameters);
        assertThat(session1.getID(), notNullValue());

        MCREditorSession session2 = new MCREditorSession(xml, parameters);
        assertThat(session2.getID(), notNullValue());

        assertThat(session1.getID(), not(equalTo(session2.getID())));
    }

    @Test
    public void testGetXML() {
        MCREditorSession session = new MCREditorSession(xml, parameters);
        assertThat(session.getXML(), sameInstance(xml));
    }
}
