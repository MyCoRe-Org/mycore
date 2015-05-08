package org.mycore.solr.index;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;

/**
 * Solr index task which handles <code>MCRSolrIndexHandler</code>'s. Surrounds the indexHandler with a hibernate
 * session.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrIndexTask implements Callable<List<MCRSolrIndexHandler>> {

    final static Logger LOGGER = Logger.getLogger(MCRSolrIndexTask.class);

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
        //Can run in current thread or parallel executor
        boolean reUseSession = MCRSessionMgr.hasCurrentSession();
        //this.indexHandler.index() creates a session anyway
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        if (!reUseSession) {
            mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        }
        Session session = null;
        Transaction transaction = null;
        boolean readOnly;
        try {
            session = MCRHIBConnection.instance().getSession();
            transaction = reUseSession ? null : session.beginTransaction();
            readOnly = session.isDefaultReadOnly();
            session.setDefaultReadOnly(true);
            long start = System.currentTimeMillis();
            this.indexHandler.index();
            long end = System.currentTimeMillis();
            indexHandler.getStatistic().addDocument(indexHandler.getDocuments());
            indexHandler.getStatistic().addTime(end - start);
            session.setDefaultReadOnly(readOnly);
            return this.indexHandler.getSubHandlers();
        } finally {
            try {
                if (transaction != null) {
                    transaction.commit();
                }
            } finally {
                if (!reUseSession) {
                    session.close();
                    MCRSessionMgr.releaseCurrentSession();
                    mcrSession.close();
                }
            }
        }
    }
}
