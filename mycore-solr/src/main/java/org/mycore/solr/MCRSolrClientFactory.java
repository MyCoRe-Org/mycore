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

package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_MAIN;
import static org.mycore.solr.MCRSolrConstants.SOLR_SERVER_URL;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;

/**
 * @author shermann
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 * @author Jens Kupferschmidt
 */
public abstract class MCRSolrClientFactory {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrClientFactory.class);

    private static String DEFAULT_CORE_NAME;

    private static Map<String, MCRSolrCore> CORE_MAP;

    /**
     * Load the cores defined by the properties MCR.Solr.ServerURL (included core name) or
     * MCR.Solr.ServerURL (without core name) and MCR.Solr.Core.Main .
     */
    static {
        try {
            CORE_MAP = Collections.synchronizedMap(new HashMap<String, MCRSolrCore>());
            setSolrDefaultClient(SOLR_SERVER_URL, SOLR_CORE_MAIN);
        } catch (Throwable t) {
            LOGGER.error("Exception creating solr client object", t);
        } finally {
            LOGGER.info(MessageFormat.format("Using server at address \"{0}\"", getDefaultSolrCore().getClient()
                .getBaseURL()));
        }
    }

    /**
     * Add a SOLR core instance to the list
     * 
     * @param core the MCRSolrCore instance 
     */
    public static void add(MCRSolrCore core) {
        CORE_MAP.put(core.getName(), core);
    }

    /**
     * Remove a SOLR core instance from the list
     * 
     * @param coreName the name of the MCRSolrCore instance 
     */
    public static MCRSolrCore remove(String coreName) {
        return CORE_MAP.remove(coreName);
    }

    public static MCRSolrCore get(String coreName) {
        return CORE_MAP.get(coreName);
    }

    public static MCRSolrCore getDefaultSolrCore() {
        return DEFAULT_CORE_NAME != null ? CORE_MAP.get(DEFAULT_CORE_NAME) : null;
    }

    /**
     * Returns a SolrClient from the SOLR base URL.
     * This does not reference a specific SOLR core.
     */
    public static SolrClient getSolrBaseClient() {
        return CORE_MAP.get("").getClient();
    }

    /**
     * Returns the solr client of the default core.
     */
    public static SolrClient getSolrDefaultClient() {
        return getDefaultSolrCore().getClient();
    }

    /**
     * Returns the concurrent solr client of the default core.
     */
    public static SolrClient getConcurrentSolrDefaultClient() {
        return getDefaultSolrCore().getConcurrentClient();
    }

    /**
     * Sets the new solr url including the core.
     */
    /**
    public static void setSolrClient(String serverURL) {
        removeDefaultCore();
        MCRSolrCore defaultCore = new MCRSolrCore(serverURL);
        add(defaultCore);
        MCRSolrCore baseCore = new MCRSolrCore(defaultCore.serverURL, "");
        add(baseCore);
        DEFAULT_CORE_NAME = defaultCore.getName();
    }
    **/

    /**
     * Sets the new solr server url and core name as default client.
     * 
     * @param serverURL base solr url
     * @param core core of the server
     */
    public static void setSolrDefaultClient(String serverURL, String core) {
        removeDefaultCore();
        add(new MCRSolrCore(serverURL, core));
        DEFAULT_CORE_NAME = core;
    }

    /**
     * Sets the new solr server url and core name.
     * 
     * @param serverURL base solr url
     * @param core core of the server
     */
    public static void setSolrClient(String serverURL, String core) {
        add(new MCRSolrCore(serverURL, core));
    }

    private static void removeDefaultCore() {
        MCRSolrCore defaultCore = getDefaultSolrCore();
        if (defaultCore != null) {
            defaultCore.shutdown();
            CORE_MAP.remove(defaultCore.getName());
            DEFAULT_CORE_NAME = null;
        }
        MCRSolrCore baseCore = CORE_MAP.get("");
        if (baseCore != null) {
            baseCore.shutdown();
            CORE_MAP.remove(baseCore.getName());
        }
    }
}
