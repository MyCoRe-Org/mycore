package org.mycore.solr;

import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.mycore.common.MCRConfiguration;
import org.mycore.solr.logging.MCRSolrLogLevels;
import org.mycore.solr.utils.MCRSolrUtils;

/**
 * @author shermann
 */
public class MCRSolrServerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrServerFactory.class);

    private static HttpSolrServer _solrServer;

    private static ConcurrentUpdateSolrServer _concurrentSolrServer;

    static {
        try {
            String solrServerUrl = MCRSolrUtils.getSolrPropertyValue("ServerURL");
            _solrServer = new HttpSolrServer(solrServerUrl);
            _solrServer.setRequestWriter(new BinaryRequestWriter());

            String queueSize = MCRConfiguration.instance().getString("MCR.Solr.Server.queueSize", "10");
            String threadSize = MCRConfiguration.instance().getString("MCR.Solr.Server.threadSize", "10");
            _concurrentSolrServer = new ConcurrentUpdateSolrServer(solrServerUrl, Integer.parseInt(queueSize), Integer.parseInt(threadSize));
            _concurrentSolrServer.setRequestWriter(new BinaryRequestWriter());
        } catch (Exception e) {
            LOGGER.error("Exception creating solr server object", e);
        } catch (Error error) {
            LOGGER.error("Error creating solr server object", error);
        } finally {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO,
                    MessageFormat.format("Using server at address \"{0}\"", _solrServer != null ? _solrServer.getBaseURL() : "n/a"));
        }
    }

    /**
     * Hide constructor.
     * */
    private MCRSolrServerFactory() {

    }

    /**
     * Returns an instance of {@link HttpSolrServer}. The underlying HttpClient is threadsafe.
     * 
     * @return an instance of {@link HttpSolrServer}
     */
    public static HttpSolrServer getSolrServer() {
        return MCRSolrServerFactory._solrServer;
    }

    public static ConcurrentUpdateSolrServer getConcurrentSolrServer() {
        return MCRSolrServerFactory._concurrentSolrServer;
    }
}
