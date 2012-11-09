package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayContentStream extends AbstractSolrContentStream<byte[]> {

    /**
     * @param xmlAsByteArr
     * @param name
     * @param sourceInfo
     * @throws IOException
     */
    public ByteArrayContentStream(byte[] xmlAsByteArr, String name) throws IOException {
        super();
        this.name = name;
        this.sourceInfo = xmlAsByteArr.getClass().getName();
        contentType = "text/xml";
        source = xmlAsByteArr;
    }

    @Override
    protected void setup() {
        size = (long)(source).length;
        inputStream = new ByteArrayInputStream((source));
    }
}
