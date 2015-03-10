package org.mycore.oai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.oai.pmh.Set;
import org.mycore.solr.MCRSolrClientFactory;

public class MCROAISolrSearcher extends MCROAISearcher {

    protected final static Logger LOGGER = Logger.getLogger(MCROAISolrSearcher.class);

    private Set set;

    private Date from;

    private Date until;

    private List<String> deletedRecords;

    public MCROAISolrSearcher() {
        super();
        this.deletedRecords = new ArrayList<>();
    }

    @Override
    public MCROAIResult query(int cursor) {
        return solrQuery(cursor);
    }

    @Override
    public MCROAIResult query(Set set, Date from, Date until) {
        this.set = set;
        this.from = from;
        this.until = until;
        this.deletedRecords = searchDeleted(from, until);
        return solrQuery(0);
    }

    protected MCROAIResult solrQuery(int start) {
        SolrQuery query = new SolrQuery();
        // query
        String restriction = getConfig().getString(getConfigPrefix() + "Search.Restriction", null);
        if (restriction != null) {
            query.set("q", restriction);
        }

        // sort
        String sortBy = getConfig().getString(getConfigPrefix() + "Search.SortBy", null);
        if (sortBy != null) {
            sortBy = sortBy.replace("ascending", "asc").replace("descending", "desc");
            query.set("sort", sortBy);
        }

        // set support
        if (this.set != null) {
            String origSet = set.getSpec();
            String setFilter = getConfig().getString(getConfigPrefix() + "MapSetToQuery." + origSet, null);
            if (setFilter == null) {
                String classid = MCRClassificationAndSetMapper.mapSetToClassification(getConfigPrefix(), set.getSpec()
                    .split("\\:")[0]);
                if (origSet.contains(":")) {
                    setFilter = "category.top:" + classid + "\\:" + origSet.substring(origSet.indexOf(":") + 1);
                } else {
                    setFilter = "category.top:" + classid + "*";
                }
            }
            query.add("fq", setFilter);
        }

        // date range
        StringBuilder dateFilter = new StringBuilder();
        // from & until
        if (this.from != null || this.until != null) {
            dateFilter.append(buildFromUntilCondition(this.from, this.until));
        }
        if (dateFilter.length() > 0) {
            query.add("fq", dateFilter.toString());
        }

        // start & rows
        query.add("start", String.valueOf(start));
        query.add("rows", String.valueOf(getPartitionSize()));
        // request handler
        query.setRequestHandler(getConfig().getString(getConfigPrefix() + "Search.RequestHandler", "/select"));

        // do the query
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        try {
            QueryResponse response = solrClient.query(query);
            return new MCROAISolrResult(response, this.deletedRecords);
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return null;
    }

    private String buildFromUntilCondition(Date from, Date until) {
        String fieldFromUntil = getConfig().getString(getConfigPrefix() + "Search.FromUntil", "modified");
        StringBuilder query = new StringBuilder(" +").append(fieldFromUntil).append(":[");
        if (from == null) {
            query.append("* TO ");
        } else {
            MCRISO8601Date mcrDate = new MCRISO8601Date();
            mcrDate.setDate(from);
            query.append(mcrDate.getISOString()).append(" TO ");
        }
        if (until == null) {
            query.append("*]");
        } else {
            MCRISO8601Date mcrDate = new MCRISO8601Date();
            mcrDate.setDate(until);
            query.append(mcrDate.getISOString()).append("]");
        }
        return query.toString();
    }

    @Override
    public Date getEarliestTimestamp() {
        String sortBy = getConfig().getString(getConfigPrefix() + "EarliestDatestamp.SortBy", "modified asc");
        String fieldName = getConfig().getString(getConfigPrefix() + "EarliestDatestamp.fieldName", "modified");
        String restriction = getConfig().getString(getConfigPrefix() + "Search.Restriction", null);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("sort", sortBy);
        params.add("q", restriction);
        params.add("fl", fieldName);
        params.add("rows", "1");
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        try {
            QueryResponse response = solrClient.query(params);
            SolrDocumentList list = response.getResults();
            if (list.size() >= 1) {
                return (Date) list.get(0).getFieldValue(fieldName);
            }
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return null;
    }

}
