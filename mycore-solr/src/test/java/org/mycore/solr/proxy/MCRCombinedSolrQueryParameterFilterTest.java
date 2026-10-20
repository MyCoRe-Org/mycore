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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.jupiter.api.Test;

public class MCRCombinedSolrQueryParameterFilterTest {

    @Test
    void testFiltersAreAppliedInOrder() {
        MCRSolrQueryParameterFilter filter = new MCRCombinedSolrQueryParameterFilter(new OwnerFilter(),
            new AppendFilter());
        ModifiableSolrParams params = createUserParams();

        filter.filter("/find", params);

        assertArrayEquals(new String[] { "createdby:anna" }, params.getParams("owner"));
        assertArrayEquals(new String[] { "createdby:bob", "/find:createdby:anna" }, params.getParams("fq"));
    }

    @Test
    void testWithoutFilters() {
        MCRSolrQueryParameterFilter filter = new MCRCombinedSolrQueryParameterFilter();
        ModifiableSolrParams params = createUserParams();

        filter.filter("/find", params);

        assertArrayEquals(new String[] { "*:*", "createdby:bob" }, params.getParams("owner"));
        assertArrayEquals(new String[] { "createdby:bob" }, params.getParams("fq"));
    }

    private static ModifiableSolrParams createUserParams() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("owner", "*:*", "createdby:bob");
        params.add("fq", "createdby:bob");
        return params;
    }

    private static class OwnerFilter implements MCRSolrQueryParameterFilter {
        @Override
        public void filter(String queryHandlerPath, ModifiableSolrParams params) {
            params.set("owner", "createdby:anna");
        }
    }

    private static class AppendFilter implements MCRSolrQueryParameterFilter {
        @Override
        public void filter(String queryHandlerPath, ModifiableSolrParams params) {
            params.add("fq", queryHandlerPath + ":" + params.get("owner"));
        }
    }

}
