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
public class MCRBaseContentStream extends MCRAbstractSolrContentStream<MCRContent> {

    /***/
    protected MCRBaseContentStream() {
        super();
    }

    /**
     * @param objectOrDerivate
     * @param content
     */
    public MCRBaseContentStream(String id, MCRContent content) {
        this();
        this.name = id;
        this.sourceInfo = content.getSystemId();
        this.contentType = getTransformer().getMimeType();
        this.source = content;
    }

    @Override
    protected void setup() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        getTransformer().transform(source, out);
        byte[] byteArray = out.toByteArray();
        this.size = (long) byteArray.length;
        inputStream = new ByteArrayInputStream(byteArray);
    }

    public MCRContentTransformer getTransformer() {
        return MCRSolrAppender.getTransformer();
    }

}
