package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * Solr index task which handles <code>MCRSolrIndexHandler</code>'s.
 * Surrounds the indexHandler with a hibernate session.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrIndexTask implements Callable<List<MCRSolrIndexHandler>> {

    final static Logger LOGGER = Logger.getLogger(MCRSolrIndexTask.class);

    protected MCRSolrIndexHandler indexHandler;

    public MCRSolrIndexTask(MCRSolrIndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    @Override
    public List<MCRSolrIndexHandler> call() throws SolrServerException, IOException {
        Session session = null;
        Transaction transaction = null;
        try {
            session = MCRHIBConnection.instance().getSession();
            transaction = session.beginTransaction();
            this.indexHandler.index();
            return this.indexHandler.getSubHandlers();
        } finally {
            try {
                session.clear();
                if(transaction != null) {
                    transaction.commit();
                }
            } finally {
                session.close();
            }
        }
    }

}
