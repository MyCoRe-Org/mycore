package org.mycore.solr.common.xml;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Hashtable;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.log4j.Logger;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.solr.SolrServerFactory;
import org.mycore.solr.search.SolrURL;

/**
 * 
 * @author Matthias Eichner
 */
public class MCRSolrQueryResolver implements URIResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrQueryResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String key = href.substring(href.indexOf(":") + 1);

        Hashtable<String, String> params = MCRURIResolver.getParameterMap(key);

        String query;
        try {
            query = URLDecoder.decode(params.get("q"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Cannot format query " + params.get("q"), e);
            return null;
        }
        String sort = params.get("sort");
        String start = params.get("start");
        String rows = params.get("rows");

        SolrURL solrURL = new SolrURL(SolrServerFactory.getSolrServer());
        solrURL.setQueryParamter(query);
        if (start != null) {
            solrURL.setStart(Integer.valueOf(start));
        }
        if (rows != null) {
            solrURL.setRows(Integer.valueOf(rows));
        }
        solrURL.addSortOption(sort);
        try {
            MCRStreamContent result = new MCRStreamContent(solrURL.openStream(), solrURL.getUrl().toString());
            return result.getSource();
        } catch (Exception exc) {
            LOGGER.error("Unable to build solr document", exc);
        }
        return null;
    }

}
