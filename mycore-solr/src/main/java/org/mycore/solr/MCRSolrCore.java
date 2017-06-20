package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
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

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrCore.class);

    private static boolean USE_CONCURRENT_SERVER;

    protected String serverURL;

    protected String name;

    protected HttpSolrClient solrClient;

    protected ConcurrentUpdateSolrClient concurrentClient;

    static {
        USE_CONCURRENT_SERVER = MCRConfiguration.instance()
            .getBoolean(CONFIG_PREFIX + "ConcurrentUpdateSolrClient.Enabled");
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
        int connectionTimeout = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "SolrClient.ConnectionTimeout");
        int socketTimeout = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "SolrClient.SocketTimeout");

        // default server
        solrClient = new HttpSolrClient.Builder(coreURL).build();
        solrClient.setRequestWriter(new BinaryRequestWriter());
        solrClient.setConnectionTimeout(connectionTimeout);
        solrClient.setSoTimeout(socketTimeout);
        // concurrent server
        if (USE_CONCURRENT_SERVER) {
            int queueSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "ConcurrentUpdateSolrClient.QueueSize");
            int threadCount = MCRConfiguration.instance()
                .getInt(CONFIG_PREFIX + "ConcurrentUpdateSolrClient.ThreadCount");
            concurrentClient = new ConcurrentUpdateSolrClient.Builder(coreURL)
                .withQueueSize(queueSize)
                .withThreadCount(threadCount)
                .build();
            concurrentClient.setRequestWriter(new BinaryRequestWriter());
            concurrentClient.setConnectionTimeout(connectionTimeout);
            concurrentClient.setSoTimeout(socketTimeout);
        }
        // shutdown handler
        MCRShutdownHandler.getInstance().addCloseable(new MCRShutdownHandler.Closeable() {

            @Override
            public void prepareClose() {
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 5;
            }

            @Override
            public void close() {
                shutdown();
            }
        });
    }

    public synchronized void shutdown() {
        int shutdownSocketTimeout = MCRConfiguration.instance()
            .getInt(CONFIG_PREFIX + "SolrClient.ShutdownSocketTimeout");
        solrClient.setSoTimeout(shutdownSocketTimeout);
        concurrentClient.setSoTimeout(shutdownSocketTimeout);
        try {
            shutdownGracefully(solrClient);
            solrClient = null;
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while shutting down SOLR client.", e);
        }
        try {
            shutdownGracefully(concurrentClient);
            concurrentClient = null;
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while shutting down SOLR client.", e);
        }
        LOGGER.info("Solr shutdown process completed.");
    }

    private void shutdownGracefully(SolrClient client) throws SolrServerException, IOException {
        if (client != null) {
            LOGGER.info("Shutting down solr client: " + client);
            client.commit(false, false);
            client.close();
        }
    }

    /**
     * Returns the name of the core.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the default solr client instance. Use this for queries.
     */
    public HttpSolrClient getClient() {
        return solrClient;
    }

    /**
     * Returns the concurrent solr client instance. Use this for indexing.
     */
    public SolrClient getConcurrentClient() {
        return concurrentClient != null ? concurrentClient : solrClient;
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

}
