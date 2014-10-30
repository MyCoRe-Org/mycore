package org.mycore.solr.classification;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl;

/**
 * Solr extension of the category link service. Updates the solr index on set and delete
 * operations.
 * 
 * @see MCRSolrCategoryLink
 * @author Matthias Eichner
 */
public class MCRSolrCategLinkService extends MCRCategLinkServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrCategLinkService.class);

    @Override
    public void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories) {
        super.setLinks(objectReference, categories);
        // solr
        List<SolrInputDocument> solrDocumentList = MCRSolrClassificationUtil
            .toSolrDocument(objectReference, categories);
        MCRSolrClassificationUtil.bulkIndex(solrDocumentList);
    }

    @Override
    public void deleteLink(MCRCategLinkReference reference) {
        super.deleteLink(reference);
        // solr
        try {
            SolrServer solrServer = MCRSolrClassificationUtil.getCore().getServer();
            delete(solrServer, reference);
            solrServer.commit();
        } catch (Exception exc) {
            LOGGER.error("Unable to delete links of object " + reference.getObjectID(), exc);
        }
    }

    @Override
    public void deleteLinks(Collection<MCRCategLinkReference> references) {
        super.deleteLinks(references);
        // solr
        SolrServer solrServer = MCRSolrClassificationUtil.getCore().getServer();
        for (MCRCategLinkReference reference : references) {
            try {
                delete(solrServer, reference);
            } catch (Exception exc) {
                LOGGER.error("Unable to delete links of object " + reference.getObjectID(), exc);
            }
        }
        try {
            solrServer.commit();
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Unable to commit.", e);
        }
    }

    /**
     * Delete the given reference in solr.
     * 
     * @param solrServer
     * @param reference
     * @throws SolrServerException
     * @throws IOException
     */
    protected void delete(SolrServer solrServer, MCRCategLinkReference reference) throws SolrServerException,
        IOException {
        solrServer.deleteByQuery("+type:link +object:" + reference.getObjectID());
    }

}
