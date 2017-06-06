package org.mycore.oai.set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.mycore.common.config.MCRConfiguration;

public class MCROAIQueryToSetHandler extends MCROAISolrSetHandler {

    @Override
    public void apply(MCRSet set, SolrQuery solrQuery) {
        MCRConfiguration config = MCRConfiguration.instance();
        String setId = set.getSetId();
        String value = set.getSpec();
        String configQuery = config.getString(getConfigPrefix() + "Sets." + setId + ".Query");
        String resolvedQuery = configQuery.replace("{id}", value);
        solrQuery.add(CommonParams.FQ, resolvedQuery);
    }
    
}
