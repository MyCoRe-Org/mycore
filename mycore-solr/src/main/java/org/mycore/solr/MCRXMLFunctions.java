/**
 * 
 */
package org.mycore.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

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
     */
    public static long getNumFound(String q) throws SolrServerException, IOException {
        if (q == null || q.length() == 0) {
            throw new IllegalArgumentException("The query string must not be null");
        }
        SolrQuery solrQuery = new SolrQuery(q);
        solrQuery.set("rows", 0);
        QueryResponse queryResponse;
        queryResponse = MCRSolrClientFactory.getSolrClient().query(solrQuery);
        return queryResponse.getResults().getNumFound();
    }

    /**
     * @param q the query to execute (in solr syntax)
     * @return the identifier of the first document matching the query
     * @throws SolrServerException
     * @throws IOException
     */
    public static String getIdentifierOfFirst(String q) throws SolrServerException, IOException {
        if (q == null || q.length() == 0) {
            throw new IllegalArgumentException("The query string must not be null");
        }
        SolrQuery solrQuery = new SolrQuery(q);
        solrQuery.set("rows", 1);
        QueryResponse queryResponse;
        queryResponse = MCRSolrClientFactory.getSolrClient().query(solrQuery);

        if (queryResponse.getResults().getNumFound() == 0) {
            return null;
        }

        return queryResponse.getResults().get(0).get("id").toString();
    }
}
