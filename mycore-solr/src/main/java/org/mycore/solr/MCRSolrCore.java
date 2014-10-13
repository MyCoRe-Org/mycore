package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;

/**
 * Core instance of a solr server.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrCore {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrCore.class);

    private static boolean USE_CONCURRENT_SERVER;

    protected String serverURL;

    protected String name;

    protected HttpSolrServer server;

    protected ConcurrentUpdateSolrServer concurrentServer;

    static {
        USE_CONCURRENT_SERVER = MCRConfiguration.instance().getBoolean(
            CONFIG_PREFIX + "ConcurrentUpdateSolrServer.Enabled", true);
    }

    /**
     * Creates a new solr server core instance. The last part of this url
     * should be the core.
     * 
     * @param serverURL whole url e.g. http://localhost:8296/docportal
     */
    public MCRSolrCore(String serverURL) {
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        int i = serverURL.lastIndexOf("/") + 1;
        setup(serverURL.substring(0, i), serverURL.substring(i));
    }

    /**
     * Creates a new solr server core instance.
     * 
     * @param serverURL base url of the solr server e.g. http://localhost:8296
     * @param name name of the core e.g. docportal
     */
    public MCRSolrCore(String serverURL, String name) {
        setup(serverURL, name);
    }

    protected void setup(String serverURL, String name) {
        if (!serverURL.endsWith("/")) {
            serverURL += serverURL + "/";
        }
        this.serverURL = serverURL;
        this.name = name;
        String coreURL = serverURL + name;

        // default server
        server = new HttpSolrServer(coreURL);
        server.setRequestWriter(new BinaryRequestWriter());
        // concurrent server
        if (USE_CONCURRENT_SERVER) {
            int queueSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "ConcurrentUpdateSolrServer.QueueSize",
                100);
            int threadSize = MCRConfiguration.instance().getInt(
                CONFIG_PREFIX + "ConcurrentUpdateSolrServer.ThreadSize", 4);
            concurrentServer = new ConcurrentUpdateSolrServer(coreURL, queueSize, threadSize);
            concurrentServer.setRequestWriter(new BinaryRequestWriter());
        }
        // shutdown handler
        MCRShutdownHandler.getInstance().addCloseable(new MCRShutdownHandler.Closeable() {

            @Override
            public void prepareClose() {
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE;
            }

            @Override
            public void close() {
                SolrServer solrServer = getServer();
                LOGGER.info("Shutting down solr server: " + solrServer);
                solrServer.shutdown();
                SolrServer concurrentSolrServer = getConcurrentServer();
                LOGGER.info("Shutting down concurrent solr server: " + concurrentSolrServer);
                concurrentSolrServer.shutdown();
                LOGGER.info("Solr shutdown process completed.");
            }
        });
    }

    public void shutdown() {
        SolrServer solrServer = getServer();
        LOGGER.info("Shutting down solr server: " + solrServer);
        solrServer.shutdown();
        SolrServer concurrentSolrServer = getConcurrentServer();
        LOGGER.info("Shutting down concurrent solr server: " + concurrentSolrServer);
        concurrentSolrServer.shutdown();
        LOGGER.info("Solr shutdown process completed.");
    }

    /**
     * Returns the name of the core.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the default solr server instance. Use this for queries.
     * 
     * @return
     */
    public HttpSolrServer getServer() {
        return server;
    }

    /**
     * Returns the concurrent solr server instance. Use this for indexing.
     * 
     * @return
     */
    public SolrServer getConcurrentServer() {
        return concurrentServer != null ? concurrentServer : server;
    }

}
