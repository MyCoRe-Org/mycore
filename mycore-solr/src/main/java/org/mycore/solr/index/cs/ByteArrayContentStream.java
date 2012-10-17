package org.mycore.solr.index.cs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayContentStream extends BaseContentStream {

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
        length = ((byte[]) source).length;
        inputStream = new BufferedInputStream(new ByteArrayInputStream(((byte[]) source)));
    }
}
