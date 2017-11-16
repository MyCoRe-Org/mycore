package org.mycore.solr.index;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;

/**
 * General interface to handle a single solr index process.
 * 
 * @author Matthias Eichner
 */
public interface MCRSolrIndexHandler {

    /**
     * Commits something to solr.
     */
    void index() throws IOException, SolrServerException;

    /**
     * Returns a list of index handlers which should be executed after
     * the default index process. Return an empty list if no sub handlers
     * are defined.
     * 
     * @return list of <code>MCRSolrIndexHandler</code>
     */
    List<MCRSolrIndexHandler> getSubHandlers();

    /**
     * Time in milliseconds solr should index the stream.
     *  -1 by default, says that solr decide when to commit.
     */
    void setCommitWithin(int commitWithin);

    int getCommitWithin();

    SolrClient getSolrClient();

    void setSolrServer(SolrClient solrClient);

    MCRSolrIndexStatistic getStatistic();

    int getDocuments();

}
