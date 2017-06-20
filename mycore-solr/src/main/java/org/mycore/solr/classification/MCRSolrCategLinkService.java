package org.mycore.solr.classification;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
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

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrCategLinkService.class);

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
            SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
            delete(solrClient, reference);
        } catch (Exception exc) {
            LOGGER.error("Unable to delete links of object " + reference.getObjectID(), exc);
        }
    }

    @Override
    public void deleteLinks(Collection<MCRCategLinkReference> references) {
        super.deleteLinks(references);
        // solr
        SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
        for (MCRCategLinkReference reference : references) {
            try {
                delete(solrClient, reference);
            } catch (Exception exc) {
                LOGGER.error("Unable to delete links of object " + reference.getObjectID(), exc);
            }
        }
    }

    /**
     * Delete the given reference in solr.
     */
    protected void delete(SolrClient solrClient, MCRCategLinkReference reference) throws SolrServerException,
        IOException {
        solrClient.deleteByQuery("+type:link +object:" + reference.getObjectID(), 500);
    }

}
