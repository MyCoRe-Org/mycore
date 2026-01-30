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

package org.mycore.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * @author shermann
 *
 */
public class MCRXMLFunctions {

    /**
     * Convenience method for retrieving the result count for a given solr query.
     * 
     * @param q the query to execute (in solr syntax)
     * 
     * @return the amount of documents matching the given query
     * @deprecated the method lacks support for core selection and is used as xalan extension,
     * which is deprecated as well. It will be removed in a future release, so please use the
     * SolrJ API directly instead or implement a XSLT function which uses uri resolver to do the
     * request to the correct core.
     */
    @Deprecated(forRemoval = true, since = "2026.06.0")
    public static long getNumFound(String q) throws SolrServerException, IOException {
        if (q == null || q.isEmpty()) {
            throw new IllegalArgumentException("The query string must not be null");
        }
        SolrQuery solrQuery = new SolrQuery(q);
        solrQuery.set("rows", 0);
        QueryRequest queryRequest = new QueryRequest(solrQuery);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
            MCRSolrAuthenticationLevel.SEARCH);
        QueryResponse queryResponse = queryRequest.process(MCRSolrIndexManager.obtainInstance()
            .requireMainIndex().getClient());
        return queryResponse.getResults().getNumFound();
    }

    /**
     * @param q the query to execute (in solr syntax)
     * @return the identifier of the first document matching the query
     * @deprecated tje method lacks support for core selection and is used as xalan extension,
     * which is deprecated as well. It will be removed in a future release, so please use the
     * SolrJ API directly instead or implement a XSLT function which uses uri resolver to do the
     * request to the correct core.
     */
    @Deprecated(forRemoval = true, since = "2026.06.0")
    public static String getIdentifierOfFirst(String q) throws SolrServerException, IOException {
        if (q == null || q.isEmpty()) {
            throw new IllegalArgumentException("The query string must not be null");
        }
        SolrQuery solrQuery = new SolrQuery(q);
        solrQuery.set("rows", 1);
        QueryResponse queryResponse;
        QueryRequest queryRequest = new QueryRequest(solrQuery);
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
            MCRSolrAuthenticationLevel.SEARCH);
        queryResponse = queryRequest.process(MCRSolrIndexManager.obtainInstance()
            .requireMainIndex().getClient());

        if (queryResponse.getResults().getNumFound() == 0) {
            return null;
        }

        return queryResponse.getResults().getFirst().get("id").toString();
    }
}
