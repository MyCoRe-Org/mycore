package org.mycore.solr.index;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Solr index task which handles <code>MCRSolrIndexHandler</code>'s.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrIndexTask implements Callable<List<MCRSolrIndexHandler>> {

    final static Logger LOGGER = LogManager.getLogger(MCRSolrIndexTask.class);

    protected MCRSolrIndexHandler indexHandler;

    /**
     * Creates a new solr index task.
     * 
     * @param indexHandler
     *            handles the index process
     */
    public MCRSolrIndexTask(MCRSolrIndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    @Override
    public List<MCRSolrIndexHandler> call() throws SolrServerException, IOException {
        long start = System.currentTimeMillis();
        this.indexHandler.index();
        long end = System.currentTimeMillis();
        indexHandler.getStatistic().addDocument(indexHandler.getDocuments());
        indexHandler.getStatistic().addTime(end - start);
        return this.indexHandler.getSubHandlers();
    }

    @Override
    public String toString() {
        return "Solr: " + this.indexHandler.toString();
    }

}
