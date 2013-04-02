package org.mycore.solr.index.cs;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.mycore.common.MCRConfiguration;

/**
 * This class can handle a index process for a content stream.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrDefaultIndexHandler extends MCRSolrAbstractIndexHandler {

    final static Logger LOGGER = Logger.getLogger(MCRSolrDefaultIndexHandler.class);

    final static String STYLESHEET = MCRConfiguration.instance().getString("MCR.Module-solr.transform", "object2fields.xsl");

    final static String UPDATE_PATH = MCRConfiguration.instance().getString("MCR.Module-solr.UpdatePath", "/update");

    public MCRSolrDefaultIndexHandler(MCRSolrAbstractContentStream<?> stream) {
        super(stream);
    }

    public MCRSolrDefaultIndexHandler(MCRSolrAbstractContentStream<?> stream, SolrServer solrServer) {
        super(stream, solrServer);
    }

    /**
     * Invokes an index request for the current content stream.
     */
    public void index() throws IOException, SolrServerException {
        LOGGER.trace("Solr: indexing data of\"" + this.getStream().getName() + "\"");
        long tStart = System.currentTimeMillis();
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(UPDATE_PATH);
        updateRequest.addContentStream(getStream());
        updateRequest.setParam("tr", STYLESHEET);
        if(getCommitWithin() != null) {
            updateRequest.setCommitWithin(getCommitWithin());
        }
        getSolrServer().request(updateRequest);
        LOGGER.trace("Solr: indexing data of\"" + this.getStream().getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
    }

}
