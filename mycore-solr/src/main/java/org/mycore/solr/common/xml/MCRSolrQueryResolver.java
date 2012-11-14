package org.mycore.solr.common.xml;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.log4j.Logger;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.search.MCRSolrURL;

/**
 * 
 * @author Matthias Eichner
 */
public class MCRSolrQueryResolver implements URIResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrQueryResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String urlQuery = href.substring(href.indexOf(":") + 1);
        MCRSolrURL solrURL = new MCRSolrURL(MCRSolrServerFactory.getSolrServer(), urlQuery);
        try {
            MCRStreamContent result = new MCRStreamContent(solrURL.openStream(), solrURL.getUrl().toString());
            return result.getSource();
        } catch (IOException e) {
            LOGGER.error("Unable to get input stream from solr", e);
            throw new TransformerException("Unable to get input stream from solr", e);
        }
    }
}
