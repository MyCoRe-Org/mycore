package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.SERVER_URL;

import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;

/**
 * @author shermann
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrServerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrServerFactory.class);

    private static SolrServer SOLR_SERVER;

    private static SolrServer CONCURRENT_SOLR_SERVER;

    private static boolean USE_CONCURRENT_SERVER = MCRConfiguration.instance().getBoolean(
        "MCR.Module-solr.ConcurrentUpdateSolrServer.Enabled", true);

    static {
        try {
            setSolrServer(SERVER_URL);
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
                    SolrServer solrServer = getSolrServer();
                    LOGGER.info("Shutting down solr server: " + solrServer);
                    solrServer.shutdown();
                    SolrServer concurrentSolrServer = getConcurrentSolrServer();
                    LOGGER.info("Shutting down concurrent solr server: " + concurrentSolrServer);
                    concurrentSolrServer.shutdown();
                    LOGGER.info("Solr shutdown process completed.");
                }
            });
        } catch (Exception e) {
            LOGGER.error("Exception creating solr server object", e);
        } catch (Error error) {
            LOGGER.error("Error creating solr server object", error);
        } finally {
            LOGGER.info(MessageFormat.format("Using server at address \"{0}\"",
                SOLR_SERVER instanceof HttpSolrServer ? ((HttpSolrServer) SOLR_SERVER).getBaseURL() : "n/a"));
        }
    }

    public static SolrServer createSolrServer(String solrServerUrl) {
        HttpSolrServer hss = new HttpSolrServer(solrServerUrl);
        hss.setRequestWriter(new BinaryRequestWriter());
        return hss;
    }

    public static SolrServer createConcurrentUpdateSolrServer(String solrServerUrl) {
        if (USE_CONCURRENT_SERVER) {
            int queueSize = MCRConfiguration.instance().getInt("MCR.Module-solr.ConcurrentUpdateSolrServer.QueueSize", 100);
            int threadSize = MCRConfiguration.instance().getInt("MCR.Module-solr.ConcurrentUpdateSolrServer.ThreadSize", 4);
            ConcurrentUpdateSolrServer cuss = new ConcurrentUpdateSolrServer(solrServerUrl, queueSize, threadSize);
            cuss.setRequestWriter(new BinaryRequestWriter());
            return cuss;
        }
        return createSolrServer(solrServerUrl);
    }

    /**
     * Hide constructor.
     * */
    private MCRSolrServerFactory() {
    }

    /**
     * Returns an instance of {@link SolrServer}.
     * 
     * @return an instance of {@link SolrServer}
     */
    public static SolrServer getSolrServer() {
        return SOLR_SERVER;
    }

    public static SolrServer getConcurrentSolrServer() {
        return CONCURRENT_SOLR_SERVER;
    }

    public static void setSolrServer(SolrServer solrServer) {
        if (solrServer instanceof ConcurrentUpdateSolrServer) {
            LOGGER.error("Do not set a ConcurrentUpdateSolrServer instance as current solr server!");
            return;
        }
        replaceSolrServer(solrServer);
    }

    public static void setConcurrentSolrServer(ConcurrentUpdateSolrServer concurrentUpdateSolrServer) {
        replaceConcurrentUpdateSolrServer(concurrentUpdateSolrServer);
    }

    public static void setSolrServer(String solrServerURL) {
        replaceSolrServer(createSolrServer(solrServerURL));
        replaceConcurrentUpdateSolrServer(createConcurrentUpdateSolrServer(solrServerURL));
    }

    private synchronized static void replaceConcurrentUpdateSolrServer(SolrServer server) {
        if (CONCURRENT_SOLR_SERVER == server) {
            return;
        }
        SolrServer oldServer = CONCURRENT_SOLR_SERVER;
        CONCURRENT_SOLR_SERVER = server;
        if (oldServer != null) {
            oldServer.shutdown();
        }
    }

    private synchronized static void replaceSolrServer(SolrServer server) {
        if (SOLR_SERVER == server) {
            return;
        }
        SolrServer oldServer = SOLR_SERVER;
        SOLR_SERVER = server;
        if (oldServer != null) {
            oldServer.shutdown();
        }
    }

}
