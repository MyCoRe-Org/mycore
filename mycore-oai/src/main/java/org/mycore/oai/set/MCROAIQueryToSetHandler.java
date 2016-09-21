package org.mycore.oai.set;

import org.apache.solr.client.solrj.SolrQuery;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.MCROAIUtils;
import org.mycore.oai.pmh.Set;

public class MCROAIQueryToSetHandler extends MCROAISolrSetHandler {

    @Override
    public void apply(Set set, SolrQuery solrQuery) {
        MCRConfiguration config = MCRConfiguration.instance();
        String setId = MCROAIUtils.getSetId(set);
        String value = MCROAIUtils.getSetSpecValue(set);
        String configQuery = config.getString(getConfigPrefix() + "Sets." + setId + ".query");
        String resolvedQuery = configQuery.replace("{id}", value);
        solrQuery.add("fq", resolvedQuery);
    }

}
