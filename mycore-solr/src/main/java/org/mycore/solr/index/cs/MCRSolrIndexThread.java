package org.mycore.solr.index.cs;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * Index solr content streams.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrIndexThread implements Runnable {

    final static Logger LOGGER = Logger.getLogger(MCRSolrIndexThread.class);

    protected MCRAbstractSolrContentStream<?> contentStream;

    public MCRSolrIndexThread(MCRAbstractSolrContentStream<?> contentStream) {
        this.contentStream = contentStream;
    }

    @Override
    public void run() {
        Session session = null;
        try {
            session = MCRHIBConnection.instance().getSession();
            session.beginTransaction();
            this.contentStream.index();
        } catch (Exception ex) {
            LOGGER.error("Error executing index task for object " + this.contentStream.getSourceInfo(), ex);
        } finally {
            session.close();
        }
    }

}
