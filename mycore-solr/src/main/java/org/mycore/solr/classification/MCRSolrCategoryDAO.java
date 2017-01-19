package org.mycore.solr.classification;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.solr.search.MCRSolrSearchUtils;

/**
 * Extends the default category dao with solr support. Every create/write/delete operation
 * on a classification/category results in a solr reindex additionally.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrCategoryDAO extends MCRCategoryDAOImpl {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrCategoryDAO.class);

    @Override
    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        MCRCategory category = super.setURI(id, uri);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        MCRCategory category = super.setLabel(id, label);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, Set<MCRLabel> labels) {
        MCRCategory category = super.setLabels(id, labels);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        MCRCategory category = super.removeLabel(id, lang);
        MCRSolrClassificationUtil.reindex(category);
        return category;
    }

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        MCRCategory parent = super.addCategory(parentID, category, position);
        MCRSolrClassificationUtil.reindex(category, parent);
        return parent;
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(id, 0);
        MCRCategory parent = category.getParent();
        super.deleteCategory(id);
        solrDelete(id, parent);
    }

    protected void solrDelete(MCRCategoryID id, MCRCategory parent) {
        try {
            // remove all descendants and itself
            HttpSolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
            List<String> toDelete = MCRSolrSearchUtils.listIDs(solrClient,
                "ancestor:" + MCRSolrClassificationUtil.encodeCategoryId(id));
            toDelete.add(id.toString());
            solrClient.deleteById(toDelete);
            // reindex parent
            MCRSolrClassificationUtil.reindex(parent);
        } catch (Exception exc) {
            LOGGER.error("Solr: unable to delete categories of parent " + id);
        }
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        super.moveCategory(id, newParentID, index);
        solrMove(id, newParentID, index);
    }

    protected void solrMove(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        try {
            SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
            List<String> reindexList = MCRSolrSearchUtils.listIDs(solrClient,
                "ancestor:" + MCRSolrClassificationUtil.encodeCategoryId(id));
            reindexList.add(id.toString());
            reindexList.add(newParentID.toString());
            MCRSolrClassificationUtil.reindex(MCRSolrClassificationUtil.fromString(reindexList));
        } catch (Exception exc) {
            LOGGER.error("Solr: unable to move categories of category " + id + " to " + newParentID);
        }
    }

    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        Collection<MCRCategoryImpl> replacedCategories = super.replaceCategory(newCategory);
        // remove all old categories
        solrDelete(newCategory.getId(), newCategory.getParent());
        // reindex all new
        MCRSolrClassificationUtil.reindex(replacedCategories.toArray(new MCRCategoryImpl[replacedCategories.size()]));
        return replacedCategories;
    }

}
