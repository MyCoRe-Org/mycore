package org.mycore.oai.set;

import org.apache.logging.log4j.LogManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

public class MCROAIQueryToSetHandler extends MCROAISolrSetHandler {

    @Override
    public void apply(MCRSet set, SolrQuery solrQuery) {
        String setId = set.getSetId();
        String value = set.getSpec();
        String configQuery = getSetFilterQuery(setId);
        String resolvedQuery = configQuery.replace("{id}", value);
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

}
