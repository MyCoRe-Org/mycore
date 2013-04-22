package org.mycore.solr.index.cs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.datamodel.metadata.MCRBase;

import com.google.common.base.Charsets;

/**
 * Content stream suitable for wrapping {@link MCRBase} and {@link Document} objects.
 * 
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrContentStream extends MCRSolrAbstractContentStream<MCRContent> {

    final static Logger LOGGER = Logger.getLogger(MCRSolrContentStream.class);

    private final static MCRXSLTransformer TRANSFORMER;

    static {
        String stylesheet = MCRConfiguration.instance().getString("MCR.Module-solr.cs.stylesheet", "xsl/mycoreobject-solr.xsl");
        TRANSFORMER = new MCRXSLTransformer(stylesheet);
    }

    /**
     * @param objectOrDerivate
     * @param content
     */
    public MCRSolrContentStream(String id, MCRContent content) {
        super(content);
        this.setName(id);
    }

    @Override
    protected void setup() throws IOException {
        MCRContent content = getSource();
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        getTransformer().transform(content, out);
        byte[] byteArray = out.toByteArray();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(new String(byteArray, Charsets.UTF_8));
        }
        this.setSourceInfo(content.getSystemId());
        this.setContentType(getTransformer().getMimeType());
        this.setSize((long) byteArray.length);
        this.setInputStream(new ByteArrayInputStream(byteArray));
    }

    public static MCRContentTransformer getTransformer() {
        return TRANSFORMER;
    }

}
