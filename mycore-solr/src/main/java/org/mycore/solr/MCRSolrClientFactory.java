package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.CORE;
import static org.mycore.solr.MCRSolrConstants.SERVER_BASE_URL;
import static org.mycore.solr.MCRSolrConstants.SERVER_URL;

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
 */
public abstract class MCRSolrClientFactory {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrClientFactory.class);

    private static String DEFAULT_CORE_NAME;

    private static Map<String, MCRSolrCore> CORE_MAP;

    static {
        try {
            CORE_MAP = Collections.synchronizedMap(new HashMap<String, MCRSolrCore>());
            if (CORE != null) {
                setSolrClient(SERVER_BASE_URL, CORE);
            } else {
                setSolrClient(SERVER_URL);
            }
        } catch (Throwable t) {
            LOGGER.error("Exception creating solr client object", t);
        } finally {
            LOGGER.info(MessageFormat.format("Using server at address \"{0}\"", getDefaultSolrCore().getClient()
                .getBaseURL()));
        }
    }

    public static void add(MCRSolrCore core) {
        CORE_MAP.put(core.getName(), core);
    }

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
     * Returns the solr client of the default core.
     */
    public static SolrClient getSolrClient() {
        return getDefaultSolrCore().getClient();
    }

    /**
     * Returns the concurrent solr client of the default core.
     */
    public static SolrClient getConcurrentSolrClient() {
        return getDefaultSolrCore().getConcurrentClient();
    }

    /**
     * Sets the new solr url including the core.
     */
    public static void setSolrClient(String serverURL) {
        removeDefaultCore();
        MCRSolrCore defaultCore = new MCRSolrCore(serverURL);
        add(defaultCore);
        DEFAULT_CORE_NAME = defaultCore.getName();
    }

    /**
     * Sets the new solr url.
     * 
     * @param serverURL base solr url
     * @param core core of the server
     */
    public static void setSolrClient(String serverURL, String core) {
        removeDefaultCore();
        add(new MCRSolrCore(serverURL, core));
        DEFAULT_CORE_NAME = core;
    }

    private static void removeDefaultCore() {
        MCRSolrCore defaultCore = getDefaultSolrCore();
        if (defaultCore != null) {
            defaultCore.shutdown();
            CORE_MAP.remove(defaultCore.getName());
            DEFAULT_CORE_NAME = null;
        }
    }
}
