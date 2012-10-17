package org.mycore.solr;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.mycore.common.MCRConfiguration;

/**
 * @author shermann
 */
public class SolrServerFactory {

    private static final Logger LOGGER = Logger.getLogger(SolrServerFactory.class);

    private static CommonsHttpSolrServer _solrServer;

    static {
        try {
            String solrServerUrl = MCRConfiguration.instance().getString("MCR.Solr.Server.URL", "http://127.0.0.1:8080/solr");
            _solrServer = new CommonsHttpSolrServer(solrServerUrl);
            _solrServer.setRequestWriter(new BinaryRequestWriter());
        } catch (MalformedURLException e) {
            LOGGER.error("Error creating solr server object", e);
        } finally {
            LOGGER.info("Solr: using server at address \"" + _solrServer.getBaseURL() + "\"");
        }
    }

    /**
     * Hide constructor.
     * */
    private SolrServerFactory() {

    }

    /**
     * Returns an instance of {@link CommonsHttpSolrServer}. The underlying HttpClient is threadsafe.
     * 
     * @return an instance of {@link CommonsHttpSolrServer}
     */
    public static CommonsHttpSolrServer getSolrServer() {
        return SolrServerFactory._solrServer;
    }
}
