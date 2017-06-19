package org.mycore.oai.set;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.pmh.Set;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;

/**
 * Fires <code>{OAIPrefix}.Sets.{SetID}.Query</code> and limits results to the <code>id</code>s of the
 * current result list. Every returned <code>id</code> belong to this OAI set configuration.
 * @author Thomas Scheffler (yagee)
 */
class MCROAIQuerySetResolver extends MCROAISetResolver<String, SolrDocument> {

    private String query;

    private java.util.Set<String> idsInSet;

    public MCROAIQuerySetResolver(String query) {
        super();
        this.query = query;
    }

    @Override
    public void init(String configPrefix, String setId, Map<String, MCRSet> setMap, Collection<SolrDocument> result,
        Function<SolrDocument, String> identifier) {
        super.init(configPrefix, setId, setMap, result, identifier);
        if (result.isEmpty()) {
            idsInSet = Collections.emptySet();
            return;
        }
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        QueryResponse response;
        try {
            response = solrClient.query(getQuery());
        } catch (SolrServerException | IOException e) {
            throw new MCRException("Error while getting set membership.", e);
        }
        idsInSet = response.getResults().stream().map(getIdentifier()).collect(Collectors.toSet());
    }

    @Override
    public Collection<Set> getSets(String key) {
        if (idsInSet.contains(key)) {
            return Collections.singleton(getSetMap().get(getSetId()));
        }
        return Collections.emptySet();
    }

    private SolrQuery getQuery() {
        SolrQuery solrQuery = new SolrQuery();
        MCRConfiguration config = MCRConfiguration.instance();
        // query
        String idQuery = getResult().stream()
            .map(getIdentifier())
            .map(MCRSolrUtils::escapeSearchValue)
            .collect(Collectors.joining(" OR ", "id:(", ")"));
        solrQuery.setQuery(idQuery);
        solrQuery.setFilterQueries(query);
        solrQuery.setFields("id");
        solrQuery.setRows(getResult().size());
        // request handler
        solrQuery.setRequestHandler(config.getString(getConfigPrefix() + "Search.RequestHandler", "/select"));
        return solrQuery;
    }

}
