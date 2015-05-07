/**
 * 
 */
package org.mycore.solr.proxy;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrProxyServletTest extends MCRTestCase {

    /**
     * Test method for
     * {@link org.mycore.solr.proxy.MCRSolrProxyServlet#toMultiMap(org.apache.solr.common.params.ModifiableSolrParams)}.
     */
    @Test
    public final void testToMultiMap() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        String[] paramValues = new String[] { "title:junit", "author:john" };
        String paramName = "fq";
        params.add(paramName, paramValues);
        Map<String, String[]> multiMap = MCRSolrProxyServlet.toMultiMap(params);
        System.out.println(multiMap.get(paramName));
        assertEquals("Expected " + paramValues.length + " values", paramValues.length, multiMap.get(paramName).length);
    }

}
