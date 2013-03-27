package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.mycore.common.content.MCRJDOMContent;

public class MCRSolrCollectorContentStream extends MCRSolrAbstractContentStream<MCRJDOMContent> {

    public MCRSolrCollectorContentStream(MCRJDOMContent content) {
        super(content);
    }

    @Override
    protected void setup() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        MCRSolrAppender.getTransformer().transform(getSource(), out);
        byte[] byteArray = out.toByteArray();
        
        this.setName("MCRSolrObjs");
        this.setSourceInfo(getSource().getSystemId());
        this.setContentType(MCRSolrAppender.getTransformer().getMimeType());  
        this.setSize((long) byteArray.length);
        this.setInputStream(new ByteArrayInputStream(byteArray));
    }

}
