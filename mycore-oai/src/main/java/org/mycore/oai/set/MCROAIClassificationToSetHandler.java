/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.oai.set;

import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.oai.MCROAIUtils;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 * Classification set handler.
 *
 * @author Matthias Eichner
 */
public class MCROAIClassificationToSetHandler extends MCROAISolrSetHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private String classField;

    @Override
    public void init(String configPrefix, String handlerPrefix) {
        super.init(configPrefix, handlerPrefix);
        classField = MCRConfiguration2.getString(getConfigPrefix() + "SetSolrField").orElse("category.top");
    }

    @Override
    public void apply(MCRSet set, SolrQuery query) {
        String setSpec = set.getSpec();
        String classid = MCRClassificationAndSetMapper.mapSetToClassification(getConfigPrefix(), set.getSetId());
        //Check: Is it possible for setSpec to NOT contain ":" here?
        String value = setSpec.contains(":") ? setSpec.substring(setSpec.indexOf(':')) : ":*";
        String setFilter = classField + ":" + MCRSolrUtils.escapeSearchValue(classid + value);
        query.add(CommonParams.FQ, setFilter);
    }

    @Override
    public boolean filter(MCRSet set) {
        if (!filterEmptySets()) {
            return false;
        }
        SolrClient solrClient = MCRSolrCoreManager.getMainSolrClient();
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
            QueryRequest queryRequest = new QueryRequest(p);
            MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(queryRequest,
                MCRSolrAuthenticationLevel.SEARCH);
            QueryResponse response = queryRequest.process(solrClient);
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
        return MCRConfiguration2.getBoolean(getConfigPrefix() + "FilterEmptySets").orElse(true);
    }

    @Override
    public MCROAISetResolver<String, SolrDocument> getSetResolver(Collection<SolrDocument> result) {
        MCROAIClassificationSetResolver resolver = new MCROAIClassificationSetResolver();
        resolver.init(getConfigPrefix(), getHandlerPrefix(), getSetMap(), result, MCROAISolrSetHandler::getIdentifier);
        return resolver;
    }

}
