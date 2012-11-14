/**
 * 
 */
package org.mycore.solr.index.cs;

import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;

/**
 * @author shermann
 *
 */
public class MCRSolrAppender {

    private static MCRXSLTransformer transformer = new MCRXSLTransformer("xsl/mycoreobject-solr.xsl");

    public static MCRContentTransformer getTransformer() {
        return transformer;
    }

}
