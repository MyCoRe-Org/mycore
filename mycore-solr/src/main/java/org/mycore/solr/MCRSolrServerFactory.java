package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.SERVER_URL;
import static org.mycore.solr.MCRSolrConstants.CORE;
import static org.mycore.solr.MCRSolrConstants.SERVER_BASE_URL;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;

/**
 * @author shermann
 * @author Thomas Scheffler (yagee)
 * @author Matthias Eichner
 */
public abstract class MCRSolrServerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrServerFactory.class);

    private static String DEFAULT_CORE_NAME;

    private static Map<String, MCRSolrCore> CORE_MAP;

    static {
        try {
            CORE_MAP = Collections.synchronizedMap(new HashMap<String, MCRSolrCore>());
            if (CORE != null) {
                setSolrServer(SERVER_BASE_URL, CORE);
            } else {
                setSolrServer(SERVER_URL);
            }
        } catch (Exception e) {
            LOGGER.error("Exception creating solr server object", e);
        } catch (Error error) {
            LOGGER.error("Error creating solr server object", error);
        } finally {
            LOGGER.info(MessageFormat.format("Using server at address \"{0}\"", getDefaultSolrCore().getServer()
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
     * Returns an instance of {@link SolrServer}.
     * 
     * @return an instance of {@link SolrServer}
     */
    public static SolrServer getSolrServer() {
        return getDefaultSolrCore().getServer();
    }

    public static SolrServer getConcurrentSolrServer() {
        return getDefaultSolrCore().getConcurrentServer();
    }

    /**
     * Sets the new solr url including the core.
     * 
     * @param serverURL
     */
    public static void setSolrServer(String serverURL) {
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
    public static void setSolrServer(String serverURL, String core) {
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
