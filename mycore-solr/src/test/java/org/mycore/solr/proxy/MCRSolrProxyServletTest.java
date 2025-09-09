/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.solr.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 */
@MyCoReTest
public class MCRSolrProxyServletTest {

    /**
     * Test method for
     * {@link org.mycore.solr.proxy.MCRSolrProxyServlet#toMultiMap(org.apache.solr.common.params.ModifiableSolrParams)}.
     */
    @Test
    final void testToMultiMap() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        String[] paramValues = { "title:junit", "author:john" };
        String paramName = "fq";
        params.add(paramName, paramValues);
        Map<String, String[]> multiMap = MCRSolrProxyServlet.toMultiMap(params);
        System.out.println(Arrays.toString(multiMap.get(paramName)));
        assertEquals(paramValues.length, multiMap.get(paramName).length, "Expected " + paramValues.length + " values");
    }

}
