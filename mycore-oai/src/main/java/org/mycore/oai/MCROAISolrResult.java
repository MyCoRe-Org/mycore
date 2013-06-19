package org.mycore.oai;

import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

public class MCROAISolrResult implements MCROAIResult {

    protected QueryResponse response;

    protected List<String> deletedRecords;

    public MCROAISolrResult(QueryResponse response, List<String> deletedRecords) {
        this.response = response;
        this.deletedRecords = deletedRecords;
    }

    @Override
    public int getNumHits() {
        return (int) getResponse().getResults().getNumFound() + getDeletedRecords().size();
    }

    @Override
    public String getID(int cursor) {
        SolrDocumentList list = getResponse().getResults();
        long start = list.getStart();
        int pos = (int) (cursor - start);
        if (cursor >= getNumHits() || pos < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= list.size()) {
            pos = cursor - (int) getResponse().getResults().getNumFound();
            if (pos >= getDeletedRecords().size() || pos < 0) {
                throw new IndexOutOfBoundsException();
            }
            return getDeletedRecords().get(pos);
        }
        return (String) list.get(pos).getFieldValue("id");
    }

    public QueryResponse getResponse() {
        return response;
    }

    public List<String> getDeletedRecords() {
        return deletedRecords;
    }

}
