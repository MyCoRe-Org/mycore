package org.mycore.solr.index.cs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrServerFactory;

/**
 * Content stream suitable for wrapping {@link MCRFile}.
 * 
 * 
 * @author shermann
 *
 */
public class MCRFileContentStream extends MCRAbstractSolrContentStream<MCRFile> {

    private static final String EXTRACT_PATH = MCRConfiguration.instance().getString("MCR.Module-solr.ExtractPath", "/update/extract");

    /**
     * @param file
     * @throws IOException
     */

    public MCRFileContentStream(MCRFile file) throws IOException {
        super();
        name = file.getAbsolutePath();
        sourceInfo = file.getClass().getSimpleName();
        contentType = file.getContentType().getLabel();
        source = file;
    }

    @Override
    protected void setup() {
        MCRFile file = ((MCRFile) source);
        size = file.getSize();
        try {
            inputStream = new BufferedInputStream(file.getContentAsInputStream());
        } catch (IOException ex) {
            throw new RuntimeException("Error initializing MCRObjectContentStream", ex);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.cs.MCRAbstractSolrContentStream#index()
     */
    protected void index() {
        try {
            if (!(source instanceof MCRFile)) {
                return;
            }
            indexRawFile((MCRFile) source);
        } catch (IOException | SolrServerException ex) {
            LOGGER.error(MessageFormat.format("Error sending file content to solr: \n{0}", (MCRFile) source), ex);
        }
    }

    /**
     * @param file
     * @throws Exception
     */
    private void indexRawFile(MCRFile file) throws SolrServerException, IOException {
        String solrID = file.getID();
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(file.getOwnerID()));
        String idOfMCRObjectForDerivate = null;
        if (derivate != null) {
            idOfMCRObjectForDerivate = derivate.getOwnerID().toString();
        }
        LOGGER.trace("Solr: indexing file \"" + file.getAbsolutePath() + " (" + solrID + ")\"");
        /* create the update request object */
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(EXTRACT_PATH);
        updateRequest.addContentStream(this);

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
        updateRequest.setParam("literal.fileDateModified",
                MCRAbstractSolrContentStream.DATE_FORMATTER.format(file.getLastModified().getTime()));

        String urn = null;
        if ((urn = derivate.getUrnMap().get(file.getAbsolutePath())) != null) {
            updateRequest.setParam("literal.urn", urn);
        }

        LOGGER.trace("Solr: sending binary data (" + file.getAbsolutePath() + " (" + solrID + "), size is " + file.getSizeFormatted()
                + ") to solr server.");
        long t = System.currentTimeMillis();
        /* actually send the request */
        MCRSolrServerFactory.getSolrServer().request(updateRequest);
        LOGGER.trace("Solr: sending binary data \"" + file.getAbsolutePath() + " (" + solrID + ")\"" + " done in "
                + (System.currentTimeMillis() - t) + "ms");
    }
}
