package org.mycore.oai.set;

import org.apache.logging.log4j.LogManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.mycore.common.config.MCRConfiguration;

/**
 * Default implementation for a set configuration. Loads the information from the
 * {@link MCRConfiguration} (MCR.OAIDataProvider.myprovider.Sets.<b>SET_ID</b>).
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrSetConfiguration implements MCROAISetConfiguration<SolrQuery, SolrDocument, String> {

    private String id;

    private String uri;

    private MCROAISetHandler<SolrQuery, SolrDocument, String> handler;

    public MCROAISolrSetConfiguration(String configPrefix, String setId) {
        MCRConfiguration config = MCRConfiguration.instance();
        MCROAISetHandler<SolrQuery, SolrDocument, String> handler = config.getInstanceOf(
            configPrefix + "Sets." + setId + ".Handler",
            getFallbackHandler(configPrefix, setId));
        handler.init(configPrefix, setId);
        this.id = setId;
        this.uri = config.getString(configPrefix + "Sets." + setId).trim();
        this.handler = handler;
    }

    private String getFallbackHandler(String configPrefix, String setId) {
        MCRConfiguration config = MCRConfiguration.instance();
        String queryProperty = configPrefix + "Sets." + setId + ".Query";
        if (config.getString(queryProperty,
            config.getString(configPrefix + "MapSetToQuery." + setId, null)) != null) {
            return MCROAIQueryToSetHandler.class.getName();
        }
        String classProperty = configPrefix + "Sets." + setId + ".Classification";
        if (config.getString(classProperty,
            config.getString(configPrefix + "MapSetToClassification." + setId, null)) != null) {
            return MCROAIClassificationToSetHandler.class.getName();
        }
        if (config.getString(configPrefix + "Sets." + setId + ".Handler", null) == null) {
            LogManager.getLogger().error(
                "Neither '{}' nor '{}' is defined. Please map set '{}' to classification or query.", classProperty,
                queryProperty, setId);
            return MCROAIClassificationToSetHandler.class.getName();
        }
        return null;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getURI() {
        return this.uri;
    }

    @Override
    public MCROAISetHandler<SolrQuery, SolrDocument, String> getHandler() {
        return this.handler;
    }

}
