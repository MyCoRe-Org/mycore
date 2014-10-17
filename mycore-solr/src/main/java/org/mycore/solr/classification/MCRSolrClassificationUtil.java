package org.mycore.solr.classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrServerFactory;

import com.google.common.collect.Lists;

/**
 * Some solr classification utility stuff.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRSolrClassificationUtil {

    public static final String CLASSIFICATION_CORE_NAME = "classification";

    private static final Logger LOGGER = Logger.getLogger(MCRSolrClassificationUtil.class);

    private static final Object CREATE_LOCK = new Object();

    /**
     * Reindex the whole classification system.
     */
    public static void rebuildIndex() {
        LOGGER.info("rebuild classification index...");
        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        List<MCRCategoryID> rootCategoryIDs = dao.getRootCategoryIDs();
        for (MCRCategoryID rootID : rootCategoryIDs) {
            LOGGER.info("rebuild " + rootID + "...");
            MCRCategory rootCategory = dao.getCategory(rootID, -1);
            List<MCRCategory> categoryList = getDescendants(rootCategory);
            categoryList.add(rootCategory);
            bulkIndex(categoryList);
        }
    }

    /**
     * Bulk index categories. The collection is split into parts of one hundred.
     * 
     * @param categoryList the list to index
     */
    public static void bulkIndex(Collection<MCRCategory> categoryList) {
        SolrServer server = getCore().getConcurrentServer();
        List<SolrInputDocument> solrDocumentList = toSolrDocument(categoryList);
        List<List<SolrInputDocument>> partitionList = Lists.partition(solrDocumentList, 100);
        for (List<SolrInputDocument> part : partitionList) {
            try {
                server.add(part);
            } catch (SolrServerException | IOException e) {
                LOGGER.error("Unable to add classification documents.", e);
            }
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Unable to commit classification documents.", e);
        }
    }

    /**
     * Drops the whole solr classification index.
     */
    public static void dropIndex() {
        try {
            SolrServer server = getCore().getConcurrentServer();
            server.deleteByQuery("*:*");
        } catch (Exception exc) {
            LOGGER.error("Unable to drop solr classification index", exc);
        }
    }

    /**
     * Returns a list of all descendants. The list is unordered.
     * 
     * @param category
     * @return list of descendants.
     */
    public static List<MCRCategory> getDescendants(MCRCategory category) {
        List<MCRCategory> descendants = new ArrayList<>();
        for (MCRCategory child : category.getChildren()) {
            descendants.add(child);
            if (child.hasChildren()) {
                descendants.addAll(getDescendants(child));
            }
        }
        return descendants;
    }

    /**
     * Returns a list of all ancestors. The list is ordered. The first element is
     * always the root node and the last element is always the parent. If the
     * element has no ancestor an empty list is returned.
     * 
     * @param category
     * @return list of ancestors
     */
    public static LinkedList<MCRCategory> getAncestors(MCRCategory category) {
        LinkedList<MCRCategory> ancestors = new LinkedList<MCRCategory>();
        MCRCategory parent = category.getParent();
        while (parent != null) {
            ancestors.add(parent);
            parent = parent.getParent();
        }
        return ancestors;
    }

    /**
     * Creates a new list of {@link SolrInputDocument} based on the given category list.
     * 
     * @param categoryList
     * @return
     */
    public static List<SolrInputDocument> toSolrDocument(Collection<MCRCategory> categoryList) {
        List<SolrInputDocument> solrDocumentList = new ArrayList<>();
        for (MCRCategory category : categoryList) {
            MCRSolrCategory mcrSolrCategory = new MCRSolrCategory(category);
            solrDocumentList.add(mcrSolrCategory.toSolrDocument());
        }
        return solrDocumentList;
    }

    /**
     * Reindex a bunch of {@link MCRCategory}. Be aware that this method does not fail
     * if a reindex of a single category causes an exception (its just logged).
     * 
     * @param categories the categories to reindex
     */
    public static void reindex(MCRCategory... categories) {
        SolrServer server = getCore().getServer();
        for (MCRCategory category : categories) {
            MCRSolrCategory solrCategory = new MCRSolrCategory(category);
            try {
                server.add(solrCategory.toSolrDocument());
            } catch (Exception exc) {
                LOGGER.error("Unable to reindex " + category.getId(), exc);
            }
        }
        try {
            server.commit();
        } catch (Exception exc) {
            LOGGER.error("Unable to commit reindexed categories", exc);
        }
    }

    /**
     * Returns a collection of category id instances.
     * 
     * @param categoryIds list of category ids as string
     */
    public static Collection<MCRCategoryID> fromString(Collection<String> categoryIds) {
        List<MCRCategoryID> idList = new ArrayList<MCRCategoryID>(categoryIds.size());
        for (String categoyId : categoryIds) {
            idList.add(MCRCategoryID.fromString(categoyId));
        }
        return idList;
    }

    public static void reindex(Collection<MCRCategoryID> categoryIds) {
        List<MCRCategory> categoryList = new ArrayList<MCRCategory>(categoryIds.size());
        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        for (MCRCategoryID categoryId : categoryIds) {
            MCRCategory category = dao.getCategory(categoryId, 0);
            categoryList.add(category);
        }
        reindex(categoryList.toArray(new MCRCategory[categoryList.size()]));
    }

    /**
     * Returns the solr classification core.
     * 
     * @return
     */
    public static MCRSolrCore getCore() {
        MCRSolrCore classCore = MCRSolrServerFactory.get(CLASSIFICATION_CORE_NAME);
        if (classCore == null) {
            synchronized (CREATE_LOCK) {
                classCore = MCRSolrServerFactory.get(CLASSIFICATION_CORE_NAME);
                if (classCore == null) {
                    classCore = new MCRSolrCore(MCRSolrConstants.SERVER_BASE_URL, CLASSIFICATION_CORE_NAME);
                }
            }
        }
        return classCore;
    }

    /**
     * Encodes the mycore category id to a solr usable one.
     * 
     * @param classId the id to encode
     * @return
     */
    public static String encodeCategoryId(MCRCategoryID classId) {
        return classId.toString().replaceAll("\\:", "\\\\:");
    }

}
