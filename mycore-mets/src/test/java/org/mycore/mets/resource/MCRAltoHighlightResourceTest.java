package org.mycore.mets.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MCRAltoHighlightResourceTest {

    @Test
    public void buildQuery() {
        MCRAltoHighlightResource r = new MCRAltoHighlightResource();
        assertEquals("alto_content:Jena", r.buildQuery("Jena"));
        assertEquals("alto_content:Jena alto_content:Stadt", r.buildQuery("Jena Stadt"));
        assertEquals("alto_content:\"Jena Stadt\"", r.buildQuery("\"Jena Stadt\""));
        assertEquals("alto_content:Berlin alto_content:\"Jena Stadt\" alto_content:Hamburg",
                r.buildQuery("Berlin \"Jena Stadt\" Hamburg"));
        assertEquals("alto_content:\"Berlin Hamburg\" alto_content:\"Jena Stadt\"",
                r.buildQuery("\"Berlin Hamburg\" \"Jena Stadt\""));
        assertEquals("alto_content:Berlin alto_content:\"Jena Stadt\" alto_content:Hamburg",
                r.buildQuery("Berlin   \"Jena Stadt\"     Hamburg"));
    }

}
