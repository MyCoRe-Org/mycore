package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.junit.Before;
import org.mycore.common.MCRTestCase;

public class MCREditorSessionCacheTest extends MCRTestCase{

    private Element xml;

    private MCRParameters parameters;

    private MCREditorSessionCache cache;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        xml = new Element("editor");
        parameters = new MCRParameters();
        parameters.addParameterValue("id", "4711");
        cache = MCREditorSessionCache.instance();
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
