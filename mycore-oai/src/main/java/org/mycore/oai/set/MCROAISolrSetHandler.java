package org.mycore.oai.set;

import org.apache.solr.client.solrj.SolrQuery;

public abstract class MCROAISolrSetHandler implements MCROAISetHandler<SolrQuery> {

    private String configPrefix;

    private String handlerPrefix;

    @Override
    public void init(String configPrefix, String handlerPrefix) {
        this.configPrefix = configPrefix;
        this.handlerPrefix = handlerPrefix;
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public String getHandlerPrefix() {
        return handlerPrefix;
    }

}
