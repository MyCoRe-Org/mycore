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

import org.apache.logging.log4j.LogManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Default implementation for a set configuration. Loads the information from the
 * {@link MCRConfiguration} (MCR.OAIDataProvider.myprovider.Sets.<b>SET_ID</b>).
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrSetConfiguration implements MCROAISetConfiguration<SolrQuery, SolrDocument, String> {

    private static final String SETS_PREFIX = "Sets.";

    private String id;

    private String uri;

    private MCROAISetHandler<SolrQuery, SolrDocument, String> handler;

    public MCROAISolrSetConfiguration(String configPrefix, String setId) {
        String setConfigPrefix = configPrefix + SETS_PREFIX + setId;
        String defaultname = getFallbackHandler(configPrefix, setId);
        MCROAISetHandler<SolrQuery, SolrDocument, String> handler = defaultname == null
            ? MCRConfiguration2.getOrThrow(setConfigPrefix + ".Handler", MCRConfiguration2::instantiateClass)
            : MCRConfiguration2.<MCROAISetHandler<SolrQuery, SolrDocument, String>>getInstanceOf(
                setConfigPrefix + ".Handler")
                .orElseGet(() -> MCRConfiguration2.instantiateClass(defaultname));
        handler.init(configPrefix, setId);
        this.id = setId;
        this.uri = getURI(setConfigPrefix);
        this.handler = handler;
    }

    private String getURI(String setConfigPrefix) {
        String uriProperty = setConfigPrefix + ".URI";
        try {
            return MCRConfiguration2.getStringOrThrow(uriProperty);
        } catch (MCRConfigurationException e) {
            String legacy = MCRConfiguration2.getString(setConfigPrefix).orElseThrow(() -> e);
            LogManager.getLogger().warn("Please rename deprecated property '{}' to '{}'.", setConfigPrefix,
                uriProperty);
            return legacy;
        }
    }

    private String getFallbackHandler(String configPrefix, String setId) {
        String queryProperty = configPrefix + SETS_PREFIX + setId + ".Query";
        if (MCRConfiguration2.getString(queryProperty)
            .orElse(MCRConfiguration2.getString(configPrefix + "MapSetToQuery." + setId).orElse(null)) != null) {
            return MCROAIQueryToSetHandler.class.getName();
        }
        String classProperty = configPrefix + SETS_PREFIX + setId + ".Classification";
        if (MCRConfiguration2.getString(classProperty)
            .orElse(MCRConfiguration2.getString(configPrefix + "MapSetToClassification." + setId)
                .orElse(null)) != null) {
            return MCROAIClassificationToSetHandler.class.getName();
        }
        if (MCRConfiguration2.getString(configPrefix + SETS_PREFIX + setId + ".Handler").orElse(null) == null) {
            LogManager.getLogger().error(
                "Neither '{}' nor '{}' is defined. Please map set '{}' to classification or query.", classProperty,
                queryProperty, setId);
            return MCROAIClassificationToSetHandler.class.getName();
        }
        return null;
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
    public MCROAISetHandler<SolrQuery, SolrDocument, String> getHandler() {
        return this.handler;
    }

}
