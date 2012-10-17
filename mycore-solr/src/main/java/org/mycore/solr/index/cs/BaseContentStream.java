package org.mycore.solr.index.cs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.jdom.Document;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.solr.SolrServerFactory;


/**
 * Content stream suitable for wrapping {@link MCRBase} and {@link Document} objects. 
 * 
 * @author shermann
 *
 */
public class BaseContentStream extends AbstractSolrContentStream {

    private byte[] asByteArray;

    /***/
    protected BaseContentStream() {
        super();
        asByteArray = null;
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
        SolrAppender solrAppender = new SolrAppender();
        if (source instanceof MCRBase) {
            asByteArray = MCRUtils.getByteArray(solrAppender.transform(((MCRBase) source).createXML()));
        } else if (source instanceof Document) {
            asByteArray = MCRUtils.getByteArray(solrAppender.transform((Document) source));
        }
        length = asByteArray.length;
        inputStream = new BufferedInputStream(new ByteArrayInputStream(asByteArray));
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