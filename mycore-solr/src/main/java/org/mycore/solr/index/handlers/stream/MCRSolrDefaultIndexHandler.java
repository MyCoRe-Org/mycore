package org.mycore.solr.index.handlers.stream;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.UPDATE_PATH;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.NamedList;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.solr.index.cs.MCRSolrAbstractContentStream;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

/**
 * This class can handle a index process for a content stream.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrDefaultIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    private final static Logger LOGGER = LogManager.getLogger(MCRSolrDefaultIndexHandler.class);

    final static String STYLESHEET = MCRConfiguration.instance()
        .getString(CONFIG_PREFIX + "IndexHandler.ContentStream.ServerStyleSheet");

    public MCRSolrDefaultIndexHandler(MCRSolrAbstractContentStream<?> stream) {
        super(stream);
    }

    public MCRSolrDefaultIndexHandler(MCRSolrAbstractContentStream<?> stream, SolrClient solrClient) {
        super(stream, solrClient);
    }

    /**
     * Invokes an index request for the current content stream.
     */
    public void index() throws IOException, SolrServerException {
        long tStart = System.currentTimeMillis();
        SolrClient solrClient = getSolrClient();
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(UPDATE_PATH);
        updateRequest.addContentStream(getStream());
        if (STYLESHEET.length() > 0) {
            updateRequest.setParam("tr", STYLESHEET);
        }
        updateRequest.setCommitWithin(getCommitWithin());
        NamedList<Object> request = solrClient.request(updateRequest);
        if (LOGGER.isDebugEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Solr: indexing data of \"");
            stringBuilder.append(this.getStream().getName());
            stringBuilder.append("\" (");
            stringBuilder.append((System.currentTimeMillis() - tStart));
            stringBuilder.append("ms)");
            for (Map.Entry<String, Object> entry : request) {
                stringBuilder.append('\n');
                stringBuilder.append(entry.getKey());
                stringBuilder.append('=');
                stringBuilder.append(entry.getValue().toString());
            }
            LOGGER.debug(stringBuilder.toString());
        }
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.XML;
    }

}
