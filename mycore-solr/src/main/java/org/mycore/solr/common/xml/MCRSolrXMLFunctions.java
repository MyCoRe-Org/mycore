package org.mycore.solr.common.xml;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.solr.MCRSolrClientFactory;

public class MCRSolrXMLFunctions {

    /**
     * Deletes the given MyCoRe object from the solr index.
     * 
     * @param id MyCoRe ID
     */
    public static void delete(String id) throws SolrServerException, IOException {
        MCRSolrClientFactory.getSolrClient().deleteById(id);
    }

}
