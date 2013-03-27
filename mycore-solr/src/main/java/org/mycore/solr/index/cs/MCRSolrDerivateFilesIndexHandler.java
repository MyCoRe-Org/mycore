package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.solr.logging.MCRSolrLogLevels;

/**
 * Commits the files of a derivate to solr, be aware that the files are
 * not indexed directly, but added to a list of sub index handlers.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrDerivateFilesIndexHandler implements MCRSolrIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrDerivateFilesIndexHandler.class);

    protected String derivateID;

    protected SolrServer solrServer;

    protected List<MCRSolrIndexHandler> subHandlerList;

    public MCRSolrDerivateFilesIndexHandler(String derivateID, SolrServer solrServer) {
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
                if(LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");
                }
                if (file.getSize() > MCRSolrIndexer.OVER_THE_WIRE_THRESHOLD) {
                    MCRSolrContentStream contentStream = new MCRSolrContentStream(file.getID(), new MCRJDOMContent(file.createXML()));
                    MCRSolrIndexHandler indexHandler = new MCRSolrDefaultIndexHandler(contentStream, solrServer);
                    this.subHandlerList.add(indexHandler);
                } else {
                    /* extract metadata with tika */
                    MCRSolrFileContentStream contentStream = new MCRSolrFileContentStream(file);
                    MCRSolrFileIndexHandler fileIndexHandler = new MCRSolrFileIndexHandler(contentStream, solrServer);
                    this.subHandlerList.add(fileIndexHandler);
                }
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
