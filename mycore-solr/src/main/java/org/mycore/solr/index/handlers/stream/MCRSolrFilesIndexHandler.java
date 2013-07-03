package org.mycore.solr.index.handlers.stream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.file.MCRSolrMCRFileDocumentFactory;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentsHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;

/**
 * Commits <code>MCRFile</code> objects to solr, be aware that the files are
 * not indexed directly, but added to a list of sub index handlers.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrFilesIndexHandler extends MCRSolrAbstractIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrFilesIndexHandler.class);

    protected String mcrID;

    protected List<MCRSolrIndexHandler> subHandlerList;

    /**
     * Creates a new solr file index handler.
     * 
     * @param mcrID id of the derivate or mcrobject, if you put a mcrobject id here
     * all files of each derivate are indexed
     * @param solrServer where to index
     */
    public MCRSolrFilesIndexHandler(String mcrID, SolrServer solrServer) {
        this.mcrID = mcrID;
        this.solrServer = solrServer;
        this.subHandlerList = new ArrayList<>();
        this.commitWithin = -1;
    }

    @Override
    public void index() throws IOException, SolrServerException {
        MCRObjectID mcrID = MCRObjectID.getInstance(getID());
        if (mcrID.getTypeId().equals("derivate")) {
            indexDerivate(mcrID);
        } else {
            indexObject(mcrID);
        }
    }

    protected void indexDerivate(MCRObjectID derivateID) {
        MCRSolrIndexHandlerFactory ihf = MCRSolrIndexHandlerFactory.getInstance();
        List<MCRFile> files = MCRUtils.getFiles(derivateID.toString());
        int fileCount = files.size();
        List<SolrInputDocument> docs = new ArrayList<>(fileCount);
        LOGGER.info("Sending " + fileCount + " file(s) for derivate \"" + derivateID + "\"");
        for (MCRFile file : files) {
            boolean sendContent = ihf.checkFile(file);
            try {
                if (sendContent) {
                    this.subHandlerList.add(ihf.getIndexHandler(file, this.solrServer, true));
                } else {
                    SolrInputDocument fileDoc = MCRSolrMCRFileDocumentFactory.getInstance().getDocument(file);
                    docs.add(fileDoc);
                }
            } catch (Exception ex) {
                LOGGER.error("Error creating transfer thread", ex);
            }
        }
        if (!docs.isEmpty()) {
            MCRSolrInputDocumentsHandler subHandler = new MCRSolrInputDocumentsHandler(docs, solrServer);
            subHandler.setCommitWithin(getCommitWithin());
            this.subHandlerList.add(subHandler);
        }
    }

    protected void indexObject(MCRObjectID objectID) {
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(objectID, 0, TimeUnit.MILLISECONDS);
        for (MCRObjectID derivateID : derivateIds) {
            indexDerivate(derivateID);
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return this.subHandlerList;
    }

    public String getID() {
        return mcrID;
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return new MCRSolrIndexStatistic("no index operation");
    }

    @Override
    public int getDocuments() {
        return 0;
    }

}
