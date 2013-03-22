package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;

public class MCRByteArrayContentStream extends MCRAbstractSolrContentStream<byte[]> {

    /**
     * @param xmlAsByteArr
     * @param name
     * @param sourceInfo
     * @throws IOException
     */
    public MCRByteArrayContentStream(SolrServer solrServer, byte[] xmlAsByteArr, String name) throws IOException {
        super(solrServer);
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
