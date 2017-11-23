/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCore;

import com.google.common.collect.Lists;

/**
 * Some solr classification utility stuff.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRSolrClassificationUtil {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrClassificationUtil.class);

    private static final Object CREATE_LOCK = new Object();

    public static final String CLASSIFICATION_CORE_NAME;

    static {
        MCRSolrCore defaultCore = MCRSolrClientFactory.getDefaultSolrCore();
        CLASSIFICATION_CORE_NAME = MCRConfiguration.instance().getString("MCR.Module-solr.Classification.Core",
            defaultCore != null ? defaultCore.getName() + "_class" : "classification");
    }

    /**
     * Reindex the whole classification system.
     */
    public static void rebuildIndex() {
        LOGGER.info("rebuild classification index...");
        // categories
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        List<MCRCategoryID> rootCategoryIDs = categoryDAO.getRootCategoryIDs();
        for (MCRCategoryID rootID : rootCategoryIDs) {
            LOGGER.info("rebuild classification '{}'...", rootID);
            MCRCategory rootCategory = categoryDAO.getCategory(rootID, -1);
            List<MCRCategory> categoryList = getDescendants(rootCategory);
            categoryList.add(rootCategory);
            List<SolrInputDocument> solrDocumentList = toSolrDocument(categoryList);
            bulkIndex(solrDocumentList);
        }
        // links
        MCRCategLinkService linkService = MCRCategLinkServiceFactory.getInstance();
        Collection<String> linkTypes = linkService.getTypes();
        for (String linkType : linkTypes) {
            LOGGER.info("rebuild '{}' links...", linkType);
            bulkIndex(linkService.getLinks(linkType).stream()
                .map(link -> new MCRSolrCategoryLink(link.getCategory().getId(),
                    link.getObjectReference()))
                .map(MCRSolrCategoryLink::toSolrDocument)
                .collect(Collectors.toList()));
        }
    }

    /**
     * Bulk index. The collection is split into parts of one thousand.
     * 
     * @param solrDocumentList the list to index
     */
    public static void bulkIndex(List<SolrInputDocument> solrDocumentList) {
        SolrClient solrClient = getCore().getConcurrentClient();
        List<List<SolrInputDocument>> partitionList = Lists.partition(solrDocumentList, 1000);
        int docNum = solrDocumentList.size();
        int added = 0;
        for (List<SolrInputDocument> part : partitionList) {
            try {
                solrClient.add(part, 500);
                LOGGER.info("Added {}/{} documents", added += part.size(), docNum);
            } catch (SolrServerException | IOException e) {
                LOGGER.error("Unable to add classification documents.", e);
            }
        }
    }

    /**
     * Drops the whole solr classification index.
     */
    public static void dropIndex() {
        try {
            SolrClient solrClient = getCore().getConcurrentClient();
            solrClient.deleteByQuery("*:*");
        } catch (Exception exc) {
            LOGGER.error("Unable to drop solr classification index", exc);
        }
    }

    /**
     * Returns a list of all descendants. The list is unordered.
     * 
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
     * @return list of ancestors
     */
    public static LinkedList<MCRCategory> getAncestors(MCRCategory category) {
        LinkedList<MCRCategory> ancestors = new LinkedList<>();
        MCRCategory parent = category.getParent();
        while (parent != null) {
            ancestors.addFirst(parent);
            parent = parent.getParent();
        }
        return ancestors;
    }

    /**
     * Creates a new list of {@link SolrInputDocument} based on the given category list.
     */
    public static List<SolrInputDocument> toSolrDocument(Collection<MCRCategory> categoryList) {
        return categoryList.stream()
            .map(MCRSolrCategory::new)
            .map(MCRSolrCategory::toSolrDocument)
            .collect(Collectors.toList());
    }

    /**
     * Creates a new list of {@link SolrInputDocument} based on the given categories and the link.
     */
    public static List<SolrInputDocument> toSolrDocument(MCRCategLinkReference linkReference,
        Collection<MCRCategoryID> categories) {
        return categories.stream()
            .map(categoryId -> new MCRSolrCategoryLink(categoryId,
                linkReference))
            .map(MCRSolrCategoryLink::toSolrDocument)
            .collect(Collectors.toList());
    }

    /**
     * Reindex a bunch of {@link MCRCategory}. Be aware that this method does not fail
     * if a reindex of a single category causes an exception (its just logged).
     * 
     * @param categories the categories to reindex
     */
    public static void reindex(MCRCategory... categories) {
        SolrClient solrClient = getCore().getClient();
        for (MCRCategory category : categories) {
            if (category == null) {
                continue;
            }
            MCRSolrCategory solrCategory = new MCRSolrCategory(category);
            try {
                solrClient.add(solrCategory.toSolrDocument());
            } catch (Exception exc) {
                LOGGER.error("Unable to reindex {}", category.getId(), exc);
            }
        }
        try {
            solrClient.commit();
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
        List<MCRCategoryID> idList = new ArrayList<>(categoryIds.size());
        for (String categoyId : categoryIds) {
            idList.add(MCRCategoryID.fromString(categoyId));
        }
        return idList;
    }

    public static void reindex(Collection<MCRCategoryID> categoryIds) {
        List<MCRCategory> categoryList = new ArrayList<>(categoryIds.size());
        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        for (MCRCategoryID categoryId : categoryIds) {
            MCRCategory category = dao.getCategory(categoryId, 0);
            categoryList.add(category);
        }
        reindex(categoryList.toArray(new MCRCategory[categoryList.size()]));
    }

    /**
     * Returns the solr classification core.
     */
    public static MCRSolrCore getCore() {
        MCRSolrCore classCore = MCRSolrClientFactory.get(CLASSIFICATION_CORE_NAME);
        if (classCore == null) {
            synchronized (CREATE_LOCK) {
                classCore = MCRSolrClientFactory.get(CLASSIFICATION_CORE_NAME);
                if (classCore == null) {
                    classCore = new MCRSolrCore(MCRSolrConstants.SERVER_BASE_URL, CLASSIFICATION_CORE_NAME);
                    MCRSolrClientFactory.add(classCore);
                }
            }
        }
        return classCore;
    }

    /**
     * Encodes the mycore category id to a solr usable one.
     * 
     * @param classId the id to encode
     */
    public static String encodeCategoryId(MCRCategoryID classId) {
        return classId.toString().replaceAll("\\:", "\\\\:");
    }

}
