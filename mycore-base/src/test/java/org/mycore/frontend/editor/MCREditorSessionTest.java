package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.jdom.Element;
import org.junit.Test;

public class MCREditorSessionTest {
    
    @Test
    public void test() {
        String defaultURL = "http://www.test.de";
        
        Element xml = new Element("editor");
        xml.addContent(new Element("source").setAttribute("uri", "mcrobject:{id}"));
        xml.addContent(new Element("cancel").setAttribute("url", defaultURL ));

        MCRParameters parameters = new MCRParameters();
        parameters.addParameterValue("id", "4711");

        MCREditorSession session = new MCREditorSession(xml, parameters);
        assertThat( session.getID(), notNullValue() );
        assertThat( session.getCancelURL(), equalTo( defaultURL ) );
        assertThat( session.getSourceURI(), equalTo( "mcrobject:4711" ) );
    }
}
