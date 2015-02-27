package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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

    protected HttpSolrClient solrClient;

    protected ConcurrentUpdateSolrClient concurrentClient;

    static {
        USE_CONCURRENT_SERVER = MCRConfiguration.instance().getBoolean(
            CONFIG_PREFIX + "ConcurrentUpdateSolrClient.Enabled");
    }

    /**
     * Creates a new solr server core instance. The last part of this url should be the core.
     * 
     * @param serverURL
     *            whole url e.g. http://localhost:8296/docportal
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
     * @param serverURL
     *            base url of the solr server e.g. http://localhost:8296
     * @param name
     *            name of the core e.g. docportal
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
        solrClient = new HttpSolrClient(coreURL);
        solrClient.setRequestWriter(new BinaryRequestWriter());
        // concurrent server
        if (USE_CONCURRENT_SERVER) {
            int queueSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "ConcurrentUpdateSolrClient.QueueSize");
            int threadSize = MCRConfiguration.instance()
                .getInt(CONFIG_PREFIX + "ConcurrentUpdateSolrClient.ThreadSize");
            concurrentClient = new ConcurrentUpdateSolrClient(coreURL, queueSize, threadSize);
            concurrentClient.setRequestWriter(new BinaryRequestWriter());
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
                SolrClient solrClient = getClient();
                LOGGER.info("Shutting down solr client: " + solrClient);
                solrClient.shutdown();
                SolrClient concurrentSolrServer = getConcurrentClient();
                LOGGER.info("Shutting down concurrent solr client: " + concurrentSolrServer);
                concurrentSolrServer.shutdown();
                LOGGER.info("Solr shutdown process completed.");
            }
        });
    }

    public void shutdown() {
        SolrClient solrClient = getClient();
        LOGGER.info("Shutting down solr client: " + solrClient);
        solrClient.shutdown();
        SolrClient concurrentSolrServer = getConcurrentClient();
        LOGGER.info("Shutting down concurrent solr client: " + concurrentSolrServer);
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
     * Returns the default solr client instance. Use this for queries.
     * 
     * @return
     */
    public HttpSolrClient getClient() {
        return solrClient;
    }

    /**
     * Returns the concurrent solr client instance. Use this for indexing.
     * 
     * @return
     */
    public SolrClient getConcurrentClient() {
        return concurrentClient != null ? concurrentClient : solrClient;
    }

}
