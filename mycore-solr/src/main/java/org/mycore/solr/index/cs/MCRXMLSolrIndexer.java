package org.mycore.solr.index.cs;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.mycore.common.MCRConfiguration;
import org.mycore.solr.MCRSolrServerFactory;

public class MCRXMLSolrIndexer {
    
    private static final String UPDATE_PATH = MCRConfiguration.instance().getString("MCR.Module-solr.UpdatePath", "/update");

    final static Logger LOGGER = Logger.getLogger(MCRXMLSolrIndexer.class);

    static String TRANSFORM = MCRConfiguration.instance().getString("MCR.Module-solr.transform", "object2fields.xsl");
    static {
        LOGGER.info(MCRXMLSolrIndexer.class.getName() + " will use " + TRANSFORM);
    }

    public void index(ContentStreamBase stream) {
        try {
            LOGGER.trace("Solr: indexing data of\"" + stream.getName() + "\"");
            long tStart = System.currentTimeMillis();
            ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(UPDATE_PATH);
            updateRequest.addContentStream(stream);
            updateRequest.setParam("tr", TRANSFORM);
            MCRSolrServerFactory.getConcurrentSolrServer().request(updateRequest);
            LOGGER.trace("Solr: indexing data of\"" + stream.getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
        } catch (Exception ex) {
            LOGGER.error("Error sending content to solr through content stream " + this, ex);
        }
    }
}
