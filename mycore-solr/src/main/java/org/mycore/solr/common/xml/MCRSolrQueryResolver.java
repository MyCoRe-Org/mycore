package org.mycore.solr.common.xml;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.common.content.MCRURLContent;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.search.MCRSolrURL;

/**
 * 
 * @author Matthias Eichner
 */
public class MCRSolrQueryResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String urlQuery = href.substring(href.indexOf(":") + 1);
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        MCRSolrURL solrURL = new MCRSolrURL((HttpSolrClient) solrClient, urlQuery);
        try {
            MCRURLContent result = new MCRURLContent(solrURL.getUrl());
            return result.getSource();
        } catch (IOException e) {
            throw new TransformerException("Unable to get input stream from solr: " + solrURL.getUrl(), e);
        }
    }
}
