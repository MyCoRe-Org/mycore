package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.solr.common.util.ContentStreamBase;
import org.jdom.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;

public class MCRXMLContentCollectorStream extends ContentStreamBase {

    private MCRContent content;
    
    public MCRXMLContentCollectorStream(Element mcrObjColector) {
        content = new MCRJDOMContent(mcrObjColector);
        setName("MCRSolrObjs");
        setSourceInfo(content.getSystemId());
        setContentType(MCRSolrAppender.getTransformer().getMimeType());
    }

    @Override
    public InputStream getStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        MCRSolrAppender.getTransformer().transform(content, out);
        byte[] byteArray = out.toByteArray();
        setSize((long) byteArray.length);
        return new ByteArrayInputStream(byteArray);
    }
}
