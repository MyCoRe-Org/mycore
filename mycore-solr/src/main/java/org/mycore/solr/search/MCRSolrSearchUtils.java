package org.mycore.solr.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

public abstract class MCRSolrSearchUtils {

    /**
     * Returns a list of ids found by the given query. Returns an empty list
     * when nothing is found.
     * 
     * @param server solr server
     * @param query solr query
     * @return list of id's
     * @throws SolrServerException communication with the solr server failed in any way
     */
    public static List<String> listIDs(SolrServer server, String query) throws SolrServerException {
        return list(server, query, new IdDocumentHandler());
    }

    public static <T> List<T> list(SolrServer server, String query, DocumentHandler<T> handler)
        throws SolrServerException {
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
            QueryResponse response = server.query(p);
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
