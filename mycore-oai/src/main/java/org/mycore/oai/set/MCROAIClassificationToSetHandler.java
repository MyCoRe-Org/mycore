package org.mycore.oai.set;

import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.MCROAIUtils;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;

/**
 * Classification set handler.
 * 
 * @author Matthias Eichner
 */
public class MCROAIClassificationToSetHandler extends MCROAISolrSetHandler {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAIClassificationToSetHandler.class);

    private String classField;

    @Override
    public void init(String configPrefix, String handlerPrefix) {
        super.init(configPrefix, handlerPrefix);
        classField = MCRConfiguration.instance().getString(getConfigPrefix() + "SetSolrField", "category.top");
    }

    public void apply(MCRSet set, SolrQuery query) {
        String setSpec = set.getSpec();
        String classid = MCRClassificationAndSetMapper.mapSetToClassification(getConfigPrefix(), set.getSetId());
        //Check: Is it possible for setSpec to NOT contain ":" here?
        String value = setSpec.contains(":") ? setSpec.substring(setSpec.indexOf(":")) : ":*";
        String setFilter = classField + ":" + MCRSolrUtils.escapeSearchValue(classid + value);
        query.add(CommonParams.FQ, setFilter);
    }

    @Override
    public boolean filter(MCRSet set) {
        if (!filterEmptySets()) {
            return false;
        }
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        ModifiableSolrParams p = new ModifiableSolrParams();
        String value = set.getSpec();
        p.set(CommonParams.Q, MCROAIUtils.getDefaultSetQuery(value, getConfigPrefix()));
        String restriction = MCROAIUtils.getDefaultRestriction(getConfigPrefix());
        if (restriction != null) {
            p.set(CommonParams.FQ, restriction);
        }
        p.set(CommonParams.ROWS, 1);
        p.set(CommonParams.FL, "id");
        try {
            QueryResponse response = solrClient.query(p);
            return response.getResults().isEmpty();
        } catch (Exception exc) {
            LOGGER.error("Unable to get results of solr server", exc);
            return true;
        }
    }

    @Override
    public Collection<String> getFieldNames() {
        if (classField == null) {
            return super.getFieldNames();
        }
        return Collections.singleton(classField);
    }

    private boolean filterEmptySets() {
        return MCRConfiguration.instance().getBoolean(getConfigPrefix() + "FilterEmptySets", true);
    }

    @Override
    public MCROAISetResolver<String, SolrDocument> getSetResolver(Collection<SolrDocument> result) {
        MCROAIClassificationSetResolver resolver = new MCROAIClassificationSetResolver();
        resolver.init(getConfigPrefix(), getHandlerPrefix(), getSetMap(), result, MCROAISolrSetHandler::getIdentifier);
        return resolver;
    }

}
