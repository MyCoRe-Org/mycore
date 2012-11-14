package org.mycore.solr;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.mycore.common.MCRConfiguration;

/**
 * @author shermann
 */
public class MCRSolrServerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrServerFactory.class);

    private static HttpSolrServer _solrServer;

    static {
        try {
            String solrServerUrl = MCRConfiguration.instance().getString("MCR.Solr.Server.URL", "http://127.0.0.1:8080/solr");
            _solrServer = new HttpSolrServer(solrServerUrl);
            _solrServer.setRequestWriter(new BinaryRequestWriter());
        } catch (Exception e) {
            LOGGER.error("Exception creating solr server object", e);
        } catch (Error error) {
            LOGGER.error("Error creating solr server object", error);
        } finally {
            LOGGER.info("Solr: using server at address \"" + _solrServer.getBaseURL() + "\"");
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
}
