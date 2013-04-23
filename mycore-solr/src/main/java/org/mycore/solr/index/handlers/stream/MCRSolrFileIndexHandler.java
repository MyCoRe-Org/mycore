package org.mycore.solr.index.handlers.stream;

import static org.mycore.solr.MCRSolrConstants.EXTRACT_PATH;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.cs.MCRSolrFileContentStream;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

public class MCRSolrFileIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    final static Logger LOGGER = Logger.getLogger(MCRSolrFileIndexHandler.class);

    final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    public MCRSolrFileIndexHandler(MCRSolrFileContentStream stream) {
        super(stream);
    }

    public MCRSolrFileIndexHandler(MCRSolrFileContentStream stream, SolrServer solrServer) {
        super(stream, solrServer);
    }

    @Override
    public void index() throws SolrServerException, IOException {
        MCRFile file = getStream().getSource();
        String solrID = file.getID();
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(file.getOwnerID()));
        String idOfMCRObjectForDerivate = null;
        if (derivate != null) {
            idOfMCRObjectForDerivate = derivate.getOwnerID().toString();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: indexing file \"" + file.getAbsolutePath() + " (" + solrID + ")\"");
        }
        /* create the update request object */
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(EXTRACT_PATH);
        MCRSolrFileContentStream fileContentStream = getStream();
        updateRequest.addContentStream(fileContentStream);

        /* set the additional parameters */
        updateRequest.setParam("literal.id", solrID);
        updateRequest.setParam("literal.DerivateID", file.getOwnerID());
        if (idOfMCRObjectForDerivate != null) {
            updateRequest.setParam("literal.returnId", idOfMCRObjectForDerivate);
        }
        updateRequest.setParam("literal.filePath", file.getAbsolutePath());
        updateRequest.setParam("literal.objectType", "data_file");
        updateRequest.setParam("literal.fileName", file.getName());
        updateRequest.setParam("literal.objectProject", MCRObjectID.getInstance(file.getOwnerID()).getProjectId());
        updateRequest.setParam("literal.modified", DATE_FORMATTER.format(file.getLastModified().getTime()));
        //set tika fields
        updateRequest.setParam("literal.stream_size", String.valueOf(fileContentStream.getSize()));
        updateRequest.setParam("literal.stream_source_info", fileContentStream.getSourceInfo());
        updateRequest.setParam("literal.stream_name", fileContentStream.getName());
        updateRequest.setParam("literal.stream_content_type", fileContentStream.getContentType());
        updateRequest.setCommitWithin(getCommitWithin());

        String urn = null;
        if ((urn = derivate.getUrnMap().get(file.getAbsolutePath())) != null) {
            updateRequest.setParam("literal.urn", urn);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: sending binary data (" + file.getAbsolutePath() + " (" + solrID + "), size is " + file.getSizeFormatted()
                + ") to solr server.");
        }
        long t = System.currentTimeMillis();
        /* actually send the request */
        getSolrServer().request(updateRequest);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: sending binary data \"" + file.getAbsolutePath() + " (" + solrID + ")\"" + " done in "
                + (System.currentTimeMillis() - t) + "ms");
        }
    }

    @Override
    public MCRSolrFileContentStream getStream() {
        return (MCRSolrFileContentStream) super.getStream();
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.fileTransfer;
    }

    @Override
    public int getDocuments() {
        return 1;
    }

}
