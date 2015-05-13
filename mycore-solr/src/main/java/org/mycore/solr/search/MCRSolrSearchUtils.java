package org.mycore.solr.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

public abstract class MCRSolrSearchUtils {

    /**
     * Returns the first document.
     * 
     * @param solrClient solr server connection
     * @param query solr query
     * @return first solr document or null
     * @throws SolrServerException communication with the solr server failed in any way
     */
    public static SolrDocument first(SolrClient solrClient, String query) throws SolrServerException, IOException {
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set("q", query);
        p.set("rows", 1);
        QueryResponse response = solrClient.query(p);
        return response.getResults().isEmpty() ? null : response.getResults().get(0);
    }

    /**
     * Returns a list of ids found by the given query. Returns an empty list
     * when nothing is found.
     * 
     * @param solrClient solr server connection
     * @param query solr query
     * @return list of id's
     * @throws SolrServerException communication with the solr server failed in any way
     */
    public static List<String> listIDs(SolrClient solrClient, String query) throws SolrServerException, IOException {
        return list(solrClient, query, new IdDocumentHandler());
    }

    public static <T> List<T> list(SolrClient solrClient, String query, DocumentHandler<T> handler)
        throws SolrServerException, IOException {
        int numPerRequest = 10000;
        List<T> resultList = new ArrayList<>();
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set("q", query);
        p.set("rows", String.valueOf(numPerRequest));
        p.set("fl", handler.fl());
        int start = 0;
        long numFound = Integer.MAX_VALUE;
        while (start < numFound) {
            p.set("start", start);
            QueryResponse response = solrClient.query(p);
            numFound = response.getResults().getNumFound();
            SolrDocumentList results = response.getResults();
            for (SolrDocument doc : results) {
                resultList.add(handler.getResult(doc));
            }
            start += response.getResults().size();
        }
        return resultList;
    }

    public static interface DocumentHandler<R> {
        public R getResult(SolrDocument document);

        public String fl();
    }

    public static class IdDocumentHandler implements DocumentHandler<String> {

        @Override
        public String getResult(SolrDocument document) {
            return document.get("id").toString();
        }

        public String fl() {
            return "id";
        }
    }

}
