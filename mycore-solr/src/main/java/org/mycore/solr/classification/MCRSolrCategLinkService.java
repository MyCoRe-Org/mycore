package org.mycore.solr.classification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategLinkServiceImpl;
import org.mycore.solr.search.MCRSolrSearchUtils;

/**
 * Solr extension of the category link service. Updates the solr index on set and delete
 * operations.
 * 
 * @author Matthias Eichner
 *
 */
public class MCRSolrCategLinkService extends MCRCategLinkServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrCategLinkService.class);

    @Override
    public void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories) {
        super.setLinks(objectReference, categories);
        MCRSolrClassificationUtil.reindex(categories);
    }

    @Override
    public void deleteLink(MCRCategLinkReference reference) {
        super.deleteLink(reference);
        try {
            List<MCRCategory> linkedCategories = getLinkedCategories(reference.getObjectID());
            MCRSolrClassificationUtil.bulkIndex(linkedCategories);
        } catch (Exception e) {
            LOGGER.error("Unable to delete link of category", e);
        }
    }

    @Override
    public void deleteLinks(Collection<MCRCategLinkReference> references) {
        super.deleteLinks(references);
        Set<MCRCategory> linkedCategories = new HashSet<>();
        for (MCRCategLinkReference ref : references) {
            try {
                linkedCategories.addAll(getLinkedCategories(ref.getObjectID()));
            } catch (Exception exc) {
                LOGGER.error("Unable to delete linked categories of object " + ref.getObjectID(), exc);
            }
        }
        MCRSolrClassificationUtil.bulkIndex(linkedCategories);
    }

    /**
     * Returns a list of categories which are linked with the given object.
     * 
     * @param objectId
     * @return
     * @throws SolrServerException
     */
    protected List<MCRCategory> getLinkedCategories(String objectId) throws SolrServerException {
        SolrServer solrServer = MCRSolrClassificationUtil.getCore().getServer();
        List<String> idList = MCRSolrSearchUtils.listIDs(solrServer, "link:" + objectId);
        List<MCRCategory> categoryList = new ArrayList<>(idList.size());
        for (String id : idList) {
            MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(MCRCategoryID.fromString(id), 0);
            categoryList.add(category);
        }
        return categoryList;
    }
}
