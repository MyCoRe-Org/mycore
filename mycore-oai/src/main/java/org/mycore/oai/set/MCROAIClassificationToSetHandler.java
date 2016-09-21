package org.mycore.oai.set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.MCROAIUtils;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.oai.pmh.Set;
import org.mycore.solr.MCRSolrClientFactory;

/**
 * Classification set handler.
 * 
 * @author Matthias Eichner
 */
public class MCROAIClassificationToSetHandler extends MCROAISolrSetHandler {

    protected final static Logger LOGGER = Logger.getLogger(MCROAIClassificationToSetHandler.class);

    public void apply(Set set, SolrQuery query) {
        String origSet = MCROAIUtils.getSetSpecValue(set);
        String setFilter = MCRConfiguration.instance().getString(getConfigPrefix() + "MapSetToQuery." + origSet, null);
        if (setFilter == null) {
            String classid = MCRClassificationAndSetMapper.mapSetToClassification(getConfigPrefix(),
                set.getSpec().split("\\:")[1]);
            String field = MCRConfiguration.instance().getString(getConfigPrefix() + "SetSolrField", "category.top");
            if (origSet.contains(":")) {
                setFilter = field + ":" + classid + "\\:" + origSet.substring(origSet.indexOf(":") + 1);
            } else {
                setFilter = field + ":" + classid + "*";
            }
        }
        query.add("fq", setFilter);
    }

    @Override
    public boolean filter(Set set) {
        if (!filterEmptySets()) {
            return false;
        }
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        ModifiableSolrParams p = new ModifiableSolrParams();
        String value = MCROAIUtils.getSetSpecValue(set);
        p.set("q", MCROAIUtils.getDefaultSetQuery(value, getConfigPrefix()));
        String restriction = MCROAIUtils.getDefaultRestriction(getConfigPrefix());
        if (restriction != null) {
            p.set("fq", restriction);
        }
        p.set("rows", 1);
        p.set("fl", "id");
        try {
            QueryResponse response = solrClient.query(p);
            return response.getResults().isEmpty();
        } catch (Exception exc) {
            LOGGER.error("Unable to get results of solr server", exc);
            return true;
        }
    }

    private boolean filterEmptySets() {
        return MCRConfiguration.instance().getBoolean(getConfigPrefix() + "FilterEmptySets", true);
    }

}
