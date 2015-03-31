package org.mycore.oai;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.solr.MCRSolrClientFactory;

/**
 * Same as {@link MCROAISetManager}. Uses solr for checking if a set is empty.
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrSetManager extends MCROAISetManager {

    @Override
    protected boolean isEmptySet(String setSpec) {
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set("q", MCROAIUtils.getDefaultSetQuery(setSpec, configPrefix));
        String restriction = MCROAIUtils.getDefaultRestriction(this.configPrefix);
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

}
