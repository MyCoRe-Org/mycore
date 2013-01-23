package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.junit.Before;

public class MCREditorSessionCacheTest {

    private Element xml;

    private MCRParameters parameters;

    private MCREditorSessionCache cache = MCREditorSessionCache.instance();

    @Before
    public void setup() {
        xml = new Element("editor");
        parameters = new MCRParameters();
        parameters.addParameterValue("id", "4711");
    }

    @Test
    public void testEmptyCache() {
        assertThat(cache.getEditorSession("foo"), nullValue());
    }

    @Test
    public void testGetSession() {
        MCREditorSession session = new MCREditorSession( xml, parameters);
        String sessionID = session.getID();
        cache.storeEditorSession(session);
        
        assertThat(cache.getEditorSession(sessionID), notNullValue());
        assertThat(cache.getEditorSession(sessionID).getID(), equalTo(sessionID));
    }
}
