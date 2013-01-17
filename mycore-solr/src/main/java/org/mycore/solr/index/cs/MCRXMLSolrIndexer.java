package org.mycore.solr.index.cs;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.logging.MCRSolrLogLevels;

/**
 * This class is recommended for batch updates. It uses a {@link ConcurrentUpdateSolrServer} for better performance.
 */
public class MCRXMLSolrIndexer implements Runnable {

    final private static String UPDATE_PATH = MCRConfiguration.instance().getString("MCR.Module-solr.UpdatePath", "/update");

    final static Logger LOGGER = Logger.getLogger(MCRXMLSolrIndexer.class);

    final static String TRANSFORM = MCRConfiguration.instance().getString("MCR.Module-solr.transform", "object2fields.xsl");
    static {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, MCRXMLSolrIndexer.class.getName() + " will use " + TRANSFORM);
    }

    private ContentStreamBase stream;

    /**
     * @param stream
     */
    public MCRXMLSolrIndexer(ContentStreamBase stream) {
        this.stream = stream;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.beginTransaction();
        try {
            index();
        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error sending content to solr through content stream " + getClass(), ex);
            session.rollbackTransaction();
            return;
        }
        session.commitTransaction();
    }

    /**
     * @throws SolrServerException
     * @throws IOException
     */
    void index() throws SolrServerException, IOException {
        LOGGER.trace("Indexing data of\"" + stream.getName() + "\"");
        long tStart = System.currentTimeMillis();
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(UPDATE_PATH);
        updateRequest.addContentStream(stream);
        updateRequest.setParam("tr", TRANSFORM);
        MCRSolrServerFactory.getConcurrentSolrServer().request(updateRequest);
        LOGGER.trace("Indexing data of\"" + stream.getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
    }
}
