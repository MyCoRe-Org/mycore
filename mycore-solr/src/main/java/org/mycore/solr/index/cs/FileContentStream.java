package org.mycore.solr.index.cs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.SolrServerFactory;

import experimental.solr.payloadsupport.analyzers.XML2StringWithPayloadProvider;

/**
 * Content stream suitable for wrapping {@link MCRFile}.
 * 
 * 
 * @author shermann
 *
 */
public class FileContentStream extends AbstractSolrContentStream<MCRFile> {

    /**
     * @param file
     * @throws IOException
     */

    public FileContentStream(MCRFile file) throws IOException {
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
            if (!"alto.xml".equals(file.getName())) {
                inputStream = new BufferedInputStream(file.getContentAsInputStream());
            } else {
                XML2StringWithPayloadProvider payloadProvider = new XML2StringWithPayloadProvider(file.getContentAsInputStream());
                Document payloads = new Document(new Element("payloads"));
                Element payload = payloads.getRootElement();

                payload.setAttribute("id", file.getID());
                payload.setAttribute("owner", file.getOwnerID());
                payload.setAttribute("path", file.getAbsolutePath());
                payload.setAttribute("file_name", file.getName());
                payload.setAttribute("object_type", "file");
                payload.setAttribute("modifydate", DATE_FORMATTER.format(file.getLastModified().getTime()));
                payload.addContent(new Element("payload").setText(payloadProvider.getFlatDocument()));

                inputStream = new BufferedInputStream(new ByteArrayInputStream(MCRUtils.getByteArray(payloads)));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error initializing MCRObjectContentStream", ex);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.cs.AbstractSolrContentStream#index()
     */
    protected void index() {
        try {
            if (!(source instanceof MCRFile)) {
                return;
            }
            indexRawFile((MCRFile) source);
        } catch (SolrServerException solrEx) {
            LOGGER.error(MessageFormat.format("Error sending file content to solr: \n{0}", (MCRFile) source), solrEx);
        } catch (IOException ioEx) {
            LOGGER.error(MessageFormat.format("Error sending file content to solr: \n{0}", (MCRFile) source), ioEx);
        }
    }

    /**
     * @param file
     * @throws Exception
     */
    private void indexRawFile(MCRFile file) throws SolrServerException, IOException {
        String solrID = file.getID();
        LOGGER.trace("Solr: indexing file \"" + file.getAbsolutePath() + " (" + solrID + ")\"");
        /* create the update request object */
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/extract");
        updateRequest.addContentStream(this);

        /* set the additional parameters */
        updateRequest.setParam("literal.id", solrID);
        updateRequest.setParam("literal.owner", file.getOwnerID());
        updateRequest.setParam("literal.path", file.getAbsolutePath());
        updateRequest.setParam("literal.file_name", file.getName());
        updateRequest.setParam("literal.object_project", MCRObjectID.getInstance(file.getOwnerID()).getProjectId());
        updateRequest.setParam("literal.modifydate", AbstractSolrContentStream.DATE_FORMATTER.format(file.getLastModified().getTime()));

        LOGGER.trace("Solr: sending binary data (" + file.getAbsolutePath() + " (" + solrID + "), size is " + file.getSizeFormatted()
                + ") to solr server.");
        long t = System.currentTimeMillis();

        /* actually send the request */
        SolrServerFactory.getSolrServer().request(updateRequest);
        LOGGER.trace("Solr: sending binary data \"" + file.getAbsolutePath() + " (" + solrID + ")\"" + " done in "
                + (System.currentTimeMillis() - t) + "ms");
    }

    /**
     * @param file
     * @throws Exception
     */
    private void indexFileWithPayload(MCRFile file) throws Exception {
        try {
            LOGGER.trace("Solr: indexing payload data of\"" + getName() + "\"");
            long tStart = System.currentTimeMillis();
            ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/xslt");
            updateRequest.addContentStream(this);
            updateRequest.setParam("tr", "payloads2fields.xsl");

            SolrServerFactory.getSolrServer().request(updateRequest);
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
        } catch (Exception ex) {
            LOGGER.error("Error sending content to solr through content stream " + this, ex);
        }
    }
}
