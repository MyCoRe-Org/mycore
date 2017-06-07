package org.mycore.oai.set;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.oai.pmh.Set;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;

public class MCROAIQueryToSetHandler extends MCROAISolrSetHandler {

    private static Pattern SIMPLE_PARAMETER_QUERY = Pattern.compile("^(?<field>[^:]+):\\{setSpec\\}$");

    private static final String SET_SPEC_PARAMETER = "{setSpec}";

    String searchField;

    String configQuery;

    @Override
    public void init(String configPrefix, String setId) {
        super.init(configPrefix, setId);
        configQuery = getSetFilterQuery(setId);
        if (configQuery.contains(SET_SPEC_PARAMETER)) {
            checkParameterQuery(configQuery);
            this.searchField = getSearchField(configQuery);
        } else {
            this.searchField = null;
        }
    }

    private String getSearchField(String simpleQuery) {
        return SIMPLE_PARAMETER_QUERY.matcher(simpleQuery).group("field");
    }

    @Override
    public Collection<String> getFieldNames() {
        if (searchField == null) {
            return super.getFieldNames();
        }
        return Collections.singleton(searchField);
    }

    private void checkParameterQuery(String simpleQuery) {
        if (!SIMPLE_PARAMETER_QUERY.matcher(simpleQuery).matches()) {
            throw new MCRConfigurationException(
                "Queries containing '" + SET_SPEC_PARAMETER + "' must be in simple form: fielName:{setSpec}");
        }
    }

    @Override
    public void apply(MCRSet set, SolrQuery solrQuery) {
        String resolvedQuery = configQuery.replace(SET_SPEC_PARAMETER, set.getSpec());
        solrQuery.add(CommonParams.FQ, resolvedQuery);
    }

    private String getSetFilterQuery(String setId) {
        MCRConfiguration config = MCRConfiguration.instance();
        String queryProperty = getConfigPrefix() + "Sets." + setId + ".Query";
        String configQuery;
        try {
            configQuery = config.getString(queryProperty);
        } catch (MCRConfigurationException e) {
            String deprecatedProperty = getConfigPrefix() + "MapSetToQuery." + setId;
            configQuery = config.getString(deprecatedProperty, null);
            if (configQuery == null) {
                throw e;
            }
            LogManager.getLogger().warn(
                "Property '{}' is deprecated and support will be removed. Please rename to '{}' soon!",
                deprecatedProperty, queryProperty);
        }
        return configQuery;
    }

    @Override
    public MCROAISetResolver<String, SolrDocument> getSetResolver(Collection<SolrDocument> result) {
        if (searchField == null) {
            MCROAIQuerySetResolver resolver = new MCROAIQuerySetResolver(configQuery);
            resolver.init(getConfigPrefix(), getHandlerPrefix(), getSetMap(), result,
                MCROAISolrSetHandler::getIdentifier);
            return resolver;
        }
        MCROAIParameterQuerySetResolver resolver = new MCROAIParameterQuerySetResolver(searchField);
        resolver.init(getConfigPrefix(), getHandlerPrefix(), getSetMap(), result, MCROAISolrSetHandler::getIdentifier);
        return resolver;
    }

    private static class MCROAIParameterQuerySetResolver extends MCROAISetResolver<String, SolrDocument> {

        private String queryField;

        private Map<String, SolrDocument> resultMap;

        public MCROAIParameterQuerySetResolver(String queryField) {
            super();
            this.queryField = queryField;
        }

        @Override
        public void init(String configPrefix, String setId, Map<String, MCRSet> setMap, Collection<SolrDocument> result,
            Function<SolrDocument, String> identifier) {
            super.init(configPrefix, setId, setMap, result, identifier);
            resultMap = getResult().stream().collect(Collectors.toMap(getIdentifier(), d -> d));
        }

        @Override
        public Collection<Set> getSets(String key) {
            return resultMap.get(key)
                .getFieldValues(queryField)
                .stream()
                .map(getSetMap()::get)
                .collect(Collectors.toSet());
        }

    }

    private static class MCROAIQuerySetResolver extends MCROAISetResolver<String, SolrDocument> {

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
            solrQuery.setQuery(query);
            solrQuery.setFields("id");
            String filterQuery = getResult().stream()
                .map(getIdentifier())
                .map(MCRSolrUtils::escapeSearchValue)
                .collect(Collectors.joining(" OR ", "id:(", ")"));
            solrQuery.set(CommonParams.FQ, filterQuery);
            // request handler
            solrQuery.setRequestHandler(config.getString(getConfigPrefix() + "Search.RequestHandler", "/select"));
            return solrQuery;

        }

    }

}
