package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.solr.logging.MCRSolrLogLevels;

/**
 * Commits the files of a derivate to solr, be aware that the files are
 * not indexed directly, but added to a list of sub index handlers.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrFilesIndexHandler implements MCRSolrIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrFilesIndexHandler.class);

    protected String derivateID;

    protected SolrServer solrServer;

    protected List<MCRSolrIndexHandler> subHandlerList;

    public MCRSolrFilesIndexHandler(String derivateID, SolrServer solrServer) {
        this.derivateID = derivateID;
        this.solrServer = solrServer;
        this.subHandlerList = new ArrayList<>();
    }

    @Override
    public void index() throws IOException, SolrServerException {
        List<MCRFile> files = MCRUtils.getFiles(getDerivateID());
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending files (" + files.size() + ") for derivate \"" + getDerivateID() + "\"");
        for (MCRFile file : files) {
            try {
                this.subHandlerList.add(MCRSolrIndexer.getIndexHandler(file, this.solrServer));
            } catch (Exception ex) {
                LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread", ex);
            }
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return this.subHandlerList;
    }

    public String getDerivateID() {
        return derivateID;
    }

}
