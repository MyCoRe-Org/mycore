package org.mycore.oai.set;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;

public abstract class MCROAISolrSetHandler implements MCROAISetHandler<SolrQuery, SolrDocument, String> {

    private String configPrefix;

    private String handlerPrefix;

    private HashMap<String, MCRSet> setMap;

    @Override
    public void init(String configPrefix, String handlerPrefix) {
        this.configPrefix = configPrefix;
        this.handlerPrefix = handlerPrefix;
        this.setMap = new HashMap<>();
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public String getHandlerPrefix() {
        return handlerPrefix;
    }

    public Collection<String> getFieldNames() {
        return Collections.emptySet();
    }

    public MCROAISetResolver<String, SolrDocument> getSetResolver(Collection<SolrDocument> result) {
        MCROAISetResolver<String, SolrDocument> resolver = new MCROAISetResolver<>();
        resolver.init(configPrefix, handlerPrefix, getSetMap(), result, MCROAISolrSetHandler::getIdentifier);
        return resolver;
    }

    protected static String getIdentifier(SolrDocument doc) {
        return doc.getFieldValue("id").toString();
    }

    @Override
    public Map<String, MCRSet> getSetMap() {
        return setMap;
    }

}
