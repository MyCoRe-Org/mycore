/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

public class MCROAIQueryToSetHandler extends MCROAISolrSetHandler {

    private static Pattern SIMPLE_PARAMETER_QUERY = Pattern.compile("^(?<field>[^:]+):\\{setSpec\\}$");

    private static final String SET_SPEC_PARAMETER = "{setSpec}";

    String searchField;

    String configQuery;

    @Override
    public void init(String configPrefix, String setId) {
        super.init(configPrefix, setId);
        configQuery = getSetFilterQuery(setId);
        if (configQuery.contains(SET_SPEC_PARAMETER)) {
            this.searchField = getSearchField(configQuery);
        } else {
            this.searchField = null;
        }
    }

    private String getSearchField(String simpleQuery) {
        Matcher fieldMatcher = SIMPLE_PARAMETER_QUERY.matcher(simpleQuery);
        if (fieldMatcher.matches()) {
            return fieldMatcher.group("field");
        }
        throw new MCRConfigurationException(
            "Queries containing '" + SET_SPEC_PARAMETER + "' must be in simple form: fielName:{setSpec}");
    }

    @Override
    public Collection<String> getFieldNames() {
        if (searchField == null) {
            return super.getFieldNames();
        }
        return Collections.singleton(searchField);
    }

    @Override
    public void apply(MCRSet set, SolrQuery solrQuery) {
        String resolvedQuery = configQuery.replace(SET_SPEC_PARAMETER, set.getSpec());
        solrQuery.add(CommonParams.FQ, resolvedQuery);
    }

    private String getSetFilterQuery(String setId) {
        MCRConfiguration config = MCRConfiguration.instance();
        String queryProperty = getConfigPrefix() + "Sets." + setId + ".Query";
        String configQuery;
        try {
            configQuery = config.getString(queryProperty);
        } catch (MCRConfigurationException e) {
            String deprecatedProperty = getConfigPrefix() + "MapSetToQuery." + setId;
            configQuery = config.getString(deprecatedProperty, null);
            if (configQuery == null) {
                throw e;
            }
            LogManager.getLogger().warn(
                "Property '{}' is deprecated and support will be removed. Please rename to '{}' soon!",
                deprecatedProperty, queryProperty);
        }
        return configQuery;
    }

    @Override
    public MCROAISetResolver<String, SolrDocument> getSetResolver(Collection<SolrDocument> result) {
        if (searchField == null) {
            MCROAIQuerySetResolver resolver = new MCROAIQuerySetResolver(configQuery);
            resolver.init(getConfigPrefix(), getHandlerPrefix(), getSetMap(), result,
                MCROAISolrSetHandler::getIdentifier);
            return resolver;
        }
        MCROAIParameterQuerySetResolver resolver = new MCROAIParameterQuerySetResolver(searchField);
        resolver.init(getConfigPrefix(), getHandlerPrefix(), getSetMap(), result, MCROAISolrSetHandler::getIdentifier);
        return resolver;
    }

}
