package org.mycore.solr.common.xml;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.solr.MCRSolrServerFactory;

public class MCRSolrXMLFunctions {

    /**
     * Deletes the given MyCoRe object from the solr index.
     * 
     * @param id MyCoRe ID
     * @throws SolrServerException
     * @throws IOException
     */
    public static void delete(String id) throws SolrServerException, IOException {
        MCRSolrServerFactory.getSolrServer().deleteById(id);
    }

}
