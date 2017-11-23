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
        MCRConfiguration config = MCRConfiguration.instance();
        String setConfigPrefix = configPrefix + SETS_PREFIX + setId;
        MCROAISetHandler<SolrQuery, SolrDocument, String> handler = config.getInstanceOf(
            setConfigPrefix + ".Handler",
            getFallbackHandler(configPrefix, setId));
        handler.init(configPrefix, setId);
        this.id = setId;
        this.uri = getURI(config, setConfigPrefix);
        this.handler = handler;
    }

    private String getURI(MCRConfiguration config, String setConfigPrefix) {
        String uriProperty = setConfigPrefix + ".URI";
        try {
            return config.getString(uriProperty).trim();
        } catch (MCRConfigurationException e) {
            String legacy = config.getString(setConfigPrefix, null);
            if (legacy == null) {
                throw e;
            }
            LogManager.getLogger().warn("Please rename deprecated property '{}' to '{}'.", setConfigPrefix,
                uriProperty);
            return legacy.trim();
        }
    }

    private String getFallbackHandler(String configPrefix, String setId) {
        MCRConfiguration config = MCRConfiguration.instance();
        String queryProperty = configPrefix + SETS_PREFIX + setId + ".Query";
        if (config.getString(queryProperty,
            config.getString(configPrefix + "MapSetToQuery." + setId, null)) != null) {
            return MCROAIQueryToSetHandler.class.getName();
        }
        String classProperty = configPrefix + SETS_PREFIX + setId + ".Classification";
        if (config.getString(classProperty,
            config.getString(configPrefix + "MapSetToClassification." + setId, null)) != null) {
            return MCROAIClassificationToSetHandler.class.getName();
        }
        if (config.getString(configPrefix + SETS_PREFIX + setId + ".Handler", null) == null) {
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
