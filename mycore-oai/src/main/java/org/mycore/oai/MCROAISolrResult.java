package org.mycore.oai;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.oai.pmh.Header;

/**
 * Solr implementation of a MCROAIResult.
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrResult implements MCROAIResult {

    protected QueryResponse response;

    private Function<SolrDocument, Header> toHeader;

    public MCROAISolrResult(QueryResponse response, Function<SolrDocument, Header> toHeader) {
        this.response = response;
        this.toHeader = toHeader;
    }

    @Override
    public int getNumHits() {
        return (int) getResponse().getResults().getNumFound();
    }

    @Override
    public List<Header> list() {
        SolrDocumentList list = getResponse().getResults();
        return list.stream().map(toHeader).collect(Collectors.toList());
    }

    @Override
    public Optional<String> nextCursor() {
        return Optional.ofNullable(this.response.getNextCursorMark());
    }

    public QueryResponse getResponse() {
        return response;
    }

}
