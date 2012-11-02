package org.mycore.solr.index.cs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.jdom.Document;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.solr.SolrServerFactory;

/**
 * Content stream suitable for wrapping {@link MCRBase} and {@link Document} objects. 
 * 
 * @author shermann
 *
 */
public class BaseContentStream extends AbstractSolrContentStream {

    private MCRContent content;

    /***/
    protected BaseContentStream() {
        super();
    }

    /**
     * @param objectOrDerivate
     */
    public BaseContentStream(MCRBase objectOrDerivate) {
        this();
        name = objectOrDerivate.getId().toString();
        sourceInfo = objectOrDerivate.getClass().getSimpleName();
        contentType = "text/xml";
        source = objectOrDerivate;
    }

    /**
     * @param objectOrDerivate
     * @param content
     */
    public BaseContentStream(MCRBase objectOrDerivate, MCRContent content) {
        this(objectOrDerivate);
        this.content = content;
    }

    /**
     * @param doc
     * @param name 
     * @throws IOException
     */
    public BaseContentStream(Document doc, String name) throws IOException {
        this();
        this.name = name;
        this.sourceInfo = doc.getClass().getName();
        contentType = "text/xml";
        source = doc;
    }

    @Override
    protected void setup() {
        byte[] inputStreamSrc = new byte[0];
        SolrAppender solrAppender = new SolrAppender();

        if (source instanceof MCRBase && content != null) {
            try {
                inputStreamSrc = MCRUtils.getByteArray(solrAppender.transform(content.getSource()));
            } catch (Exception e) {
                LOGGER.error("Could not get source object from " + content.getClass(), e);
            }
        } else if (source instanceof MCRBase) {
            inputStreamSrc = MCRUtils.getByteArray(solrAppender.transform(((MCRBase) source).createXML()));
        } else if (source instanceof Document) {
            inputStreamSrc = MCRUtils.getByteArray(solrAppender.transform((Document) source));
        }
        length = inputStreamSrc.length;
        inputStream = new BufferedInputStream(new ByteArrayInputStream(inputStreamSrc));
    }

    protected void index() {
        try {
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\"");
            long tStart = System.currentTimeMillis();
            ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/xslt");
            updateRequest.addContentStream(this);
            updateRequest.setParam("tr", "object2fields.xsl");

            SolrServerFactory.getSolrServer().request(updateRequest);
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
        } catch (Exception ex) {
            LOGGER.error("Error sending content to solr through content stream " + this, ex);
        }
    }
}