package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrServerFactory;

public class MCRXMLContentSolrStream extends ContentStreamBase {
    final static Logger LOGGER = Logger.getLogger(MCRXMLContentSolrStream.class);

    static String TRANSFORM = MCRConfiguration.instance().getString("MCR.Module-solr.transform", "object2fields.xsl");

    private MCRContent content;

    private Element mcrObjs;

    public MCRXMLContentSolrStream() {
        mcrObjs = new Element("mcrObjs");
    }

    private void setContent(Element root) {
        this.content = new MCRJDOMContent(root);
        setName("MCRSolrObjs");
        setSourceInfo(content.getSystemId());
        setContentType(MCRSolrAppender.getTransformer().getMimeType());
    }

    public void addMCRObj(String id) {
        Document mcrObjXML = MCRXMLMetadataManager.instance().retrieveXML(MCRObjectID.getInstance(id));
        mcrObjs.addContent(mcrObjXML.getRootElement().detach());
    }

    void index() {
        try {
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\"");
            setContent(mcrObjs);
            long tStart = System.currentTimeMillis();
            ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/xslt");
            updateRequest.addContentStream(this);
            updateRequest.setParam("tr", TRANSFORM);
            MCRSolrServerFactory.getConcurrentSolrServer().request(updateRequest);
            LOGGER.trace("Solr: indexing data of\"" + getName() + "\" (" + (System.currentTimeMillis() - tStart) + "ms)");
            mcrObjs = new Element("mcrObjs");
        } catch (Exception ex) {
            LOGGER.error("Error sending content to solr through content stream " + this, ex);
        }
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
