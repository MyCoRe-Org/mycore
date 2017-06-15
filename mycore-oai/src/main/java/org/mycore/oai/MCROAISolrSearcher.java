package org.mycore.oai;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRISO8601FormatChooser;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Set;
import org.mycore.oai.set.MCROAISetConfiguration;
import org.mycore.oai.set.MCROAISetHandler;
import org.mycore.oai.set.MCROAISetResolver;
import org.mycore.oai.set.MCROAISolrSetHandler;
import org.mycore.oai.set.MCRSet;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;

/**
 * Solr searcher implementation. Uses cursors.
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrSearcher extends MCROAISearcher {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAISolrSearcher.class);

    private MCRSet set;

    private Instant from;

    private Instant until;

    /**
     * Solr always returns a nextCursorMark even when the end of the list is reached.
     * Due to that behavior we query the next result in advance and check if the
     * marks are equal (which tells us the end is reached). To avoid double querying
     * we store the result in {@link #nextResult}. For simple forwarding harvesting
     * this should be efficient.
     */
    private Optional<String> lastCursor;

    private MCROAISolrResult nextResult;

    @Override
    public Optional<Header> getHeader(String mcrId) {
        SolrQuery query = getBaseQuery(CommonParams.FQ);
        query.set(CommonParams.Q, "id:" + MCRSolrUtils.escapeSearchValue(mcrId));
        query.setRows(1);
        // do the query
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        try {
            QueryResponse response = solrClient.query(query);
            SolrDocumentList results = response.getResults();
            if (!results.isEmpty()) {
                return Optional.of(toHeader(results.get(0), getSetResolver(results)));
            }
        } catch (Exception exc) {
            LOGGER.error("Unable to handle solr request", exc);
        }
        return Optional.empty();
    }

    @Override
    public MCROAIResult query(String cursor) {
        this.updateRunningExpirationTimer();
        Optional<String> currentCursor = Optional.of(cursor);
        try {
            return handleResult(currentCursor.equals(this.lastCursor) ? this.nextResult : solrQuery(currentCursor));
        } catch (SolrServerException | IOException e) {
            throw new MCRException("Error while handling query.", e);
        }
    }

    @Override
    public MCROAIResult query(MCRSet set, Instant from, Instant until) {
        this.set = set;
        this.from = from;
        this.until = until;
        try {
            return handleResult(solrQuery(Optional.empty()));
        } catch (SolrServerException | IOException e) {
            throw new MCRException("Error while handling query.", e);
        }
    }

    private MCROAIResult handleResult(MCROAISolrResult result) throws SolrServerException, IOException {
        this.nextResult = solrQuery(result.nextCursor());
        this.lastCursor = result.nextCursor();
        if (result.nextCursor().equals(this.nextResult.nextCursor())) {
            return MCROAISimpleResult.from(result).setNextCursor(null);
        }
        return result;
    }

    protected MCROAISolrResult solrQuery(Optional<String> cursor) throws SolrServerException, IOException {
        SolrQuery query = getBaseQuery(CommonParams.Q);

        // set support
        if (this.set != null) {
            String setId = this.set.getSetId();
            MCROAISetConfiguration<SolrQuery, SolrDocument, String> setConfig = getSetManager().getConfig(setId);
            setConfig.getHandler().apply(this.set, query);
        }
        // from & until
        if (this.from != null || this.until != null) {
            String fromUntilCondition = buildFromUntilCondition(this.from, this.until);
            query.add(CommonParams.FQ, fromUntilCondition);
        }

        // cursor
        query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor.orElse(CursorMarkParams.CURSOR_MARK_START));
        query.set(CommonParams.ROWS, String.valueOf(getPartitionSize()));
        query.set(CommonParams.SORT, "id asc");

        // do the query
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        QueryResponse response = solrClient.query(query);
        Collection<MCROAISetResolver<String, SolrDocument>> setResolver = getSetResolver(response.getResults());
        return new MCROAISolrResult(response, d -> toHeader(d, setResolver));
    }

    private SolrQuery getBaseQuery(String restrictionField) {
        String configPrefix = this.identify.getConfigPrefix();
        SolrQuery query = new SolrQuery();
        // query
        String restriction = getConfig().getString(configPrefix + "Search.Restriction", null);
        if (restriction != null) {
            query.set(restrictionField, restriction);
        }
        String[] requiredFields = Stream.concat(Stream.of("id", getModifiedField()), getRequiredFieldNames().stream())
            .toArray(i -> new String[i]);
        query.setFields(requiredFields);
        // request handler
        query.setRequestHandler(getConfig().getString(configPrefix + "Search.RequestHandler", "/select"));
        return query;
    }

    private Collection<MCROAISetResolver<String, SolrDocument>> getSetResolver(Collection<SolrDocument> result) {
        return getSetManager().getDefinedSetIds().stream()
            .map(getSetManager()::getConfig)
            .map(MCROAISetConfiguration::getHandler)
            .map(this::cast)
            .map(h -> h.getSetResolver(result))
            .collect(Collectors.toList());
    }

    private Collection<String> getRequiredFieldNames() {
        return getSetManager().getDefinedSetIds().stream()
            .map(getSetManager()::getConfig)
            .map(MCROAISetConfiguration::getHandler)
            .filter(MCROAISolrSetHandler.class::isInstance)
            .map(MCROAISolrSetHandler.class::cast)
            .flatMap(h -> h.getFieldNames().stream())
            .collect(Collectors.toSet());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MCROAISetHandler<SolrQuery, SolrDocument, String> cast(MCROAISetHandler handler) {
        return handler;
    }

    Header toHeader(SolrDocument doc, Collection<MCROAISetResolver<String, SolrDocument>> setResolver) {
        Date modified = (Date) doc.getFieldValue(getModifiedField());
        String docId = doc.getFieldValue("id").toString();
        Header header = new Header(getObjectManager().getOAIId(docId), modified.toInstant());
        setResolver.parallelStream()
            .map(r -> r.getSets(docId))
            .flatMap(Collection::stream)
            .sorted(this::compare)
            .sequential()
            .forEachOrdered(header.getSetList()::add);
        return header;
    }

    private int compare(Set s1, Set s2) {
        return s1.getSpec().compareTo(s2.getSpec());
    }

    private String buildFromUntilCondition(Instant from, Instant until) {
        String fieldFromUntil = getModifiedField();
        StringBuilder query = new StringBuilder(" +").append(fieldFromUntil).append(":[");
        DateTimeFormatter format = MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT;
        if (from == null) {
            query.append("* TO ");
        } else {
            query.append(format.format(from.atZone(ZoneId.of("UTC")))).append(" TO ");
        }
        if (until == null) {
            query.append("*]");
        } else {
            query.append(format.format(until.atZone(ZoneId.of("UTC")))).append(']');
        }
        return query.toString();
    }

    private String getModifiedField() {
        return getConfig().getString(getConfigPrefix() + "Search.FromUntil", "modified");
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
