package org.mycore.frontend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRURLTest extends MCRTestCase {

    @Test
    public void getParameter() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore");
        assertEquals("hallo", url.getParameter("a"));
        assertEquals("mycore", url.getParameter("b"));
    }

    @Test
    public void getParameterMap() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore&a=");
        Map<String, List<String>> p = url.getParameterMap();
        assertEquals(2, p.size());
        List<String> a = p.get("a");
        List<String> b = p.get("b");
        assertEquals(2, a.size());
        assertEquals(1, b.size());
        assertEquals("mycore", b.get(0));
        assertTrue(a.contains("hallo"));
        assertTrue(a.contains(""));
    }

}
