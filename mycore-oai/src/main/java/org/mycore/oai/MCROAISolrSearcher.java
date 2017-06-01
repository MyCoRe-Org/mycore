package org.mycore.oai;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.datamodel.common.MCRISO8601FormatChooser;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Set;
import org.mycore.oai.set.MCROAISetConfiguration;
import org.mycore.solr.MCRSolrClientFactory;

/**
 * Solr searcher implementation. Uses cursors.
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrSearcher extends MCROAISearcher {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAISolrSearcher.class);
    
    private Set set;

    private ZonedDateTime from;

    private ZonedDateTime until;

    /**
     * Solr always returns a nextCursorMark even when the end of the list is reached.
     * Due to that behavior we query the next result in advance and check if the
     * marks are equal (which tells us the end is reached). To avoid double querying
     * we store the result in {@link #nextResult}. For simple forwarding harvesting
     * this should be efficient.
     */
    private String lastCursor;

    private MCROAISolrResult nextResult;
    
    @Override
    public Optional<Header> getHeader(String mcrId){
        SolrQuery query = getBaseQuery(CommonParams.FQ);
        query.set(CommonParams.Q, mcrId);
        query.setRows(1);
        // do the query
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        try {
            QueryResponse response = solrClient.query(query);
            SolrDocumentList results = response.getResults();
            if (!results.isEmpty()){
                return Optional.of(toHeader(results.get(0)));
            }
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return Optional.empty();
    }

    @Override
    public MCROAIResult query(String cursor) {
        this.updateRunningExpirationTimer();
        return handleResult(cursor.equals(this.lastCursor) ? this.nextResult : solrQuery(cursor));
    }

    @Override
    public MCROAIResult query(Set set, ZonedDateTime from, ZonedDateTime until) {
        this.set = set;
        this.from = from;
        this.until = until;
        return handleResult(solrQuery(CursorMarkParams.CURSOR_MARK_START));
    }

    private MCROAIResult handleResult(MCROAISolrResult result) {
        this.nextResult = solrQuery(result.nextCursor());
        this.lastCursor = result.nextCursor();
        if (result.nextCursor().equals(this.nextResult.nextCursor())) {
            return MCROAISimpleResult.from(result).setNextCursor(null);
        }
        return result;
    }

    protected MCROAISolrResult solrQuery(String cursor) {
        SolrQuery query = getBaseQuery(CommonParams.Q);

        // set support
        if (this.set != null) {
            String setId = MCROAIUtils.getSetId(this.set);
            MCROAISetConfiguration<SolrQuery> setConfig = getSetManager().getConfig(setId);
            setConfig.getHandler().apply(this.set, query);
        }
        // date range
        StringBuilder dateFilter = new StringBuilder();
        // from & until
        if (this.from != null || this.until != null) {
            dateFilter.append(buildFromUntilCondition(this.from, this.until));
        }
        if (dateFilter.length() > 0) {
            query.add(CommonParams.FQ, dateFilter.toString());
        }

        // cursor
        query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
        query.set(CommonParams.ROWS, String.valueOf(getPartitionSize()));
        query.set(CommonParams.SORT, "id asc");

        // do the query
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        try {
            QueryResponse response = solrClient.query(query);
            return new MCROAISolrResult(response, this::toHeader);
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return null;
    }

    private SolrQuery getBaseQuery(String restrictionField) {
        String configPrefix = this.identify.getConfigPrefix();
        SolrQuery query = new SolrQuery();
        // query
        String restriction = getConfig().getString(configPrefix + "Search.Restriction", null);
        if (restriction != null) {
            query.set(restrictionField, restriction);
        }

        query.setFields("id", getModifiedField(), getCategoryField());
        // request handler
        query.setRequestHandler(getConfig().getString(configPrefix + "Search.RequestHandler", "/select"));
        return query;
    }
    
    Header toHeader(SolrDocument doc) {
        Date modified = (Date) doc.getFieldValue(getModifiedField());
        Header header = new Header(getObjectManager().getOAIId(doc.getFieldValue("id").toString()), modified);
        return header;
    }

    private String buildFromUntilCondition(ZonedDateTime from, ZonedDateTime until) {
        String fieldFromUntil = getModifiedField();
        StringBuilder query = new StringBuilder(" +").append(fieldFromUntil).append(":[");
        DateTimeFormatter format = MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT;
        if (from == null) {
            query.append("* TO ");
        } else {
            query.append(format.format(from)).append(" TO ");
        }
        if (until == null) {
            query.append("*]");
        } else {
            query.append(format.format(until)).append(']');
        }
        return query.toString();
    }

    private String getModifiedField() {
        return getConfig().getString(getConfigPrefix() + "Search.FromUntil", "modified");
    }

    private String getCategoryField() {
        return getConfig().getString(getConfigPrefix() + "Search.Category", "category");
    }

    @Override
    public Instant getEarliestTimestamp() {
        String sortBy = getConfig().getString(getConfigPrefix() + "EarliestDatestamp.SortBy", "modified asc");
        String fieldName = getConfig().getString(getConfigPrefix() + "EarliestDatestamp.FieldName", "modified");
        String restriction = getConfig().getString(getConfigPrefix() + "Search.Restriction", null);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(CommonParams.SORT, sortBy);
        params.add(CommonParams.Q, restriction);
        params.add(CommonParams.FQ, fieldName + ":*");
        params.add(CommonParams.FL, fieldName);
        params.add(CommonParams.ROWS, "1");
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        try {
            QueryResponse response = solrClient.query(params);
            SolrDocumentList list = response.getResults();
            if (list.size() >= 1) {
                Date date = (Date) list.get(0).getFieldValue(fieldName);
                return date.toInstant();
            }
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return null;
    }

}
