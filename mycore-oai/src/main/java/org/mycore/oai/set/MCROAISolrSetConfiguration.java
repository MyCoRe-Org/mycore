package org.mycore.oai.set;

import org.apache.solr.client.solrj.SolrQuery;
import org.mycore.common.config.MCRConfiguration;

/**
 * Default implementation for a set configuration. Loads the information from the
 * {@link MCRConfiguration} (MCR.OAIDataProvider.myprovider.Sets.<b>SET_ID</b>).
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrSetConfiguration implements MCROAISetConfiguration<SolrQuery> {

    private String id;

    private String uri;

    private MCROAISetHandler<SolrQuery> handler;

    public MCROAISolrSetConfiguration(String configPrefix, String setId) {
        MCRConfiguration config = MCRConfiguration.instance();
        MCROAISetHandler<SolrQuery> handler = config.getInstanceOf(configPrefix + "Sets." + setId + ".handler",
            "org.mycore.oai.set.MCROAIClassificationToSetHandler");
        handler.init(configPrefix, setId);

        this.id = setId;
        this.uri = config.getString(configPrefix + "Sets." + setId).trim();
        this.handler = handler;
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
    public MCROAISetHandler<SolrQuery> getHandler() {
        return this.handler;
    }

}
