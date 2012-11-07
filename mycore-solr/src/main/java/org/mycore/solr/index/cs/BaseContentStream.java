package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jdom.Document;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.datamodel.metadata.MCRBase;

/**
 * Content stream suitable for wrapping {@link MCRBase} and {@link Document} objects. 
 * 
 * @author shermann
 *
 */
public class BaseContentStream extends AbstractSolrContentStream<MCRContent> {

    private MCRContentTransformer transformer;

    /***/
    protected BaseContentStream() {
        super();
    }

    /**
     * @param objectOrDerivate
     * @param content
     */
    public BaseContentStream(String id, MCRContent content) {
        this();
        this.name = id;
        this.sourceInfo = content.getSystemId();
        transformer = SolrAppender.getTransformer();
        this.contentType = transformer.getMimeType();
        this.source = content;
    }

    @Override
    protected void setup() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64*1024);
        transformer.transform(source, out);
        byte[] byteArray = out.toByteArray();
        this.size = (long) byteArray.length;
        inputStream = new ByteArrayInputStream(byteArray);
    }
}
