/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.solr.MCRSolrIndex;
import org.mycore.solr.MCRSolrIndexRegistryManager;
import org.mycore.solr.MCRSolrIndexType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.search.MCRSolrSearchUtils;

import com.google.common.collect.Lists;

/**
 * Some solr classification utility stuff.
 *
 * @author Matthias Eichner
 */
public final class MCRSolrClassificationUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRSolrIndexType CLASSIFICATION_INDEX_TYPE = MCRSolrIndexType.CLASSIFICATION;

    private MCRSolrClassificationUtil() {
    }

    /**
     * Reindex the whole classification system with the default classification solr core.
     */
    public static void rebuildIndex() {
        rebuildIndex(getIndexList());
    }

    /**
     * Reindex the whole classification system.
     *
     * @param indexList the solr cores to use
     */
    public static void rebuildIndex(List<MCRSolrIndex> indexList) {
        LOGGER.info("rebuild classification index...");
        // categories
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.obtainInstance();
        List<MCRCategoryID> rootCategoryIDs = categoryDAO.getRootCategoryIDs();
        for (MCRCategoryID rootID : rootCategoryIDs) {
            LOGGER.info("rebuild classification '{}'...", rootID);
            MCRCategory rootCategory = categoryDAO.getCategory(rootID, -1);
            List<MCRCategory> categoryList = getDescendants(rootCategory);
            categoryList.add(rootCategory);
            List<SolrInputDocument> solrDocumentList = toSolrDocument(categoryList);

            bulkIndex(indexList, solrDocumentList);
        }
        // links
        MCRCategLinkService linkService = MCRCategLinkService.obtainInstance();
        Collection<String> linkTypes = linkService.getTypes();
        for (String linkType : linkTypes) {
            LOGGER.info("rebuild '{}' links...", linkType);
            bulkIndex(indexList, linkService.getLinks(linkType)
                .stream()
                .map(link -> new MCRSolrCategoryLink(link.getCategory().getId(),
                    link.getObjectReference()))
                .map(MCRSolrCategoryLink::toSolrDocument)
                .collect(Collectors.toList()));
        }
    }

    /**
     * Async bulk index. The collection is split into parts of one thousand.
     *
     * @param solrDocumentList the list to index
     */
    public static void bulkIndex(final List<MCRSolrIndex> indexList, List<SolrInputDocument> solrDocumentList) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            List<List<SolrInputDocument>> partitionList = Lists.partition(solrDocumentList, 1000);
            int docNum = solrDocumentList.size();
            int added = 0;
            for (List<SolrInputDocument> part : partitionList) {
                try {
                    UpdateRequest req = new UpdateRequest();
                    req.add(part);
                    req.setCommitWithin(500);
                    MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(req,
                        MCRSolrAuthenticationLevel.INDEX);
                    for (MCRSolrIndex index : indexList) {
                        req.process(index.getClient());
                    }
                    added += part.size();
                    LOGGER.info("Added {}/{} documents", added, docNum);
                } catch (SolrServerException | IOException e) {
                    LOGGER.error("Unable to add classification documents.", e);
                }
            }
        });
    }

    /**
     * Drops the whole solr classification index.
     */
    public static void dropIndex() {
        // remove all descendants and itself
        getIndexList().stream().map(MCRSolrIndex::getClient).forEach(solrClient -> {
            try {
                UpdateRequest req = new UpdateRequest();
                MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(req,
                    MCRSolrAuthenticationLevel.INDEX);
                req.deleteByQuery("*:*");
                req.process(solrClient);
            } catch (Exception exc) {
                LOGGER.error("Unable to drop solr classification index", exc);
            }
        });
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
    public static Deque<MCRCategory> getAncestors(MCRCategory category) {
        Deque<MCRCategory> ancestors = new ArrayDeque<>();
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
            .map(categoryId -> new MCRSolrCategoryLink(categoryId, linkReference))
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
        // remove all descendants and itself
        getIndexList().stream().map(MCRSolrIndex::getClient).forEach(solrClient -> {
            for (MCRCategory category : categories) {
                if (category == null) {
                    continue;
                }
                MCRSolrCategory solrCategory = new MCRSolrCategory(category);
                try {
                    UpdateRequest req = new UpdateRequest();
                    req.add(solrCategory.toSolrDocument());
                    MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(req,
                        MCRSolrAuthenticationLevel.INDEX);
                    req.process(solrClient);
                } catch (Exception exc) {
                    LOGGER.error(() -> "Unable to reindex " + category.getId(), exc);
                }
            }
            try {
                UpdateRequest commitRequest = new UpdateRequest();
                commitRequest.setAction(UpdateRequest.ACTION.COMMIT, true, true);
                MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(commitRequest,
                    MCRSolrAuthenticationLevel.ADMIN);
                commitRequest.process(solrClient);
            } catch (Exception exc) {
                LOGGER.error("Unable to commit reindexed categories", exc);
            }
        });
    }

    /**
     * Returns a collection of category id instances.
     *
     * @param categoryIds list of category ids as string
     */
    public static Collection<MCRCategoryID> fromString(Collection<String> categoryIds) {
        List<MCRCategoryID> idList = new ArrayList<>(categoryIds.size());
        for (String categoryId : categoryIds) {
            idList.add(MCRCategoryID.ofString(categoryId));
        }
        return idList;
    }

    public static void reindex(Collection<MCRCategoryID> categoryIds) {
        List<MCRCategory> categoryList = new ArrayList<>(categoryIds.size());
        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
        for (MCRCategoryID categoryId : categoryIds) {
            MCRCategory category = dao.getCategory(categoryId, 0);
            categoryList.add(category);
        }
        reindex(categoryList.toArray(MCRCategory[]::new));
    }

    /**
     * Returns the solr classification core.
     */
    public static List<MCRSolrIndex> getIndexList() {
        return MCRSolrIndexRegistryManager.obtainRegistry().getIndexByType(CLASSIFICATION_INDEX_TYPE);
    }

    /**
     * Encodes the mycore category id to a solr usable one.
     *
     * @param classId the id to encode
     */
    public static String encodeCategoryId(MCRCategoryID classId) {
        return classId.toString().replaceAll(":", "\\\\:");
    }

    static void solrDelete(MCRCategoryID id, MCRCategory parent) {
        // remove all descendants and itself
        getIndexList().stream().map(MCRSolrIndex::getClient).forEach(solrClient -> {
            try {
                List<String> toDelete = MCRSolrSearchUtils.listIDs(solrClient,
                    "ancestor:" + encodeCategoryId(id));
                toDelete.add(id.toString());
                UpdateRequest req = new UpdateRequest();
                req.deleteById(toDelete);
                MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(req,
                    MCRSolrAuthenticationLevel.INDEX);
                req.process(solrClient);
                // reindex parent
                if (parent != null) {
                    reindex(parent);
                }
            } catch (Exception exc) {
                LOGGER.error("Solr: unable to delete categories of parent {}", id);
            }
        });
    }

    static void solrMove(MCRCategoryID id, MCRCategoryID newParentID) {
        // remove all descendants and itself
        getIndexList().stream().map(MCRSolrIndex::getClient).forEach(solrClient -> {
            try {
                List<String> reindexList = MCRSolrSearchUtils.listIDs(solrClient,
                    "ancestor:" + encodeCategoryId(id));
                reindexList.add(id.toString());
                reindexList.add(newParentID.toString());
                reindex(fromString(reindexList));
            } catch (Exception exc) {
                LOGGER.error("Solr: unable to move categories of category {} to {}", id,
                    newParentID);
            }
        });
    }
}
