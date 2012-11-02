/**
 * 
 */
package org.mycore.solr.index.cs;

import javax.xml.transform.Source;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.xml.MCRLayoutService;

/**
 * @author shermann
 *
 */
public class SolrAppender {

    protected final static Logger LOGGER = Logger.getLogger(SolrAppender.class);

    /**
     * Applies the xslt stylesheets to the given document
     * 
     * @param doc {@link Document} to transform
     * @return
     */
    public Document transform(Document doc) {
        JDOMResult res = null;
        try {
            res = MCRLayoutService.instance().doLayout(new JDOMSource(doc), "xsl/mycoreobject-solr.xsl");
        } catch (Exception e) {
            LOGGER.error("Error transforming document", e);
        }

        return res.getDocument();
    }

    /**
     * @param source
     * @return
     */
    public Document transform(Source source) {
        JDOMResult res = null;
        try {
            res = MCRLayoutService.instance().doLayout(source, "xsl/mycoreobject-solr.xsl");
        } catch (Exception e) {
            LOGGER.error("Error transforming document", e);
        }

        return res.getDocument();
    }
}
