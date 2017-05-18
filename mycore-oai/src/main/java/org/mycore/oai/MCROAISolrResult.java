package org.mycore.oai;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

/**
 * Solr implementation of a MCROAIResult.
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrResult implements MCROAIResult {

    protected QueryResponse response;

    public MCROAISolrResult(QueryResponse response) {
        this.response = response;
    }

    @Override
    public int getNumHits() {
        return (int) getResponse().getResults().getNumFound();
    }

    @Override
    public List<String> list() {
        SolrDocumentList list = getResponse().getResults();
        return list.stream().map(doc -> {
            return (String) doc.getFieldValue("id");
        }).collect(Collectors.toList());
    }

    @Override
    public String nextCursor() {
        return this.response.getNextCursorMark();
    }

    public QueryResponse getResponse() {
        return response;
    }

}
