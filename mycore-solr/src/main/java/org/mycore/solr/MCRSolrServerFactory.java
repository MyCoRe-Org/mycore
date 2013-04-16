package org.mycore.solr;

import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.mycore.common.MCRConfiguration;
import org.mycore.solr.utils.MCRSolrUtils;

/**
 * @author shermann
 */
public class MCRSolrServerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrServerFactory.class);

    private static HttpSolrServer SOLR_SERVER;

    static {
        try {
            setSolrServer(MCRSolrUtils.getSolrPropertyValue("ServerURL"));
        } catch (Exception e) {
            LOGGER.error("Exception creating solr server object", e);
        } catch (Error error) {
            LOGGER.error("Error creating solr server object", error);
        } finally {
            LOGGER.info(MessageFormat.format("Using server at address \"{0}\"", SOLR_SERVER != null ? SOLR_SERVER.getBaseURL() : "n/a"));
        }
    }

    public static HttpSolrServer createSolrServer(String solrServerUrl) {
        HttpSolrServer hss = new HttpSolrServer(solrServerUrl);
        hss.setRequestWriter(new BinaryRequestWriter());
        return hss;
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
        return SOLR_SERVER;
    }

    public static void setSolrServer(HttpSolrServer solrServer) {
        SOLR_SERVER = solrServer;
    }

    public static void setSolrServer(String solrServerURL) {
        SOLR_SERVER = createSolrServer(solrServerURL);
    }

}
