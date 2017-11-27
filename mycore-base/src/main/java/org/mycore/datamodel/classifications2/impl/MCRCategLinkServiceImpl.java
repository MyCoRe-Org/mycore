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

package org.mycore.datamodel.classifications2.impl;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.QueryHints;
import org.hibernate.query.Query;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkReference_;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategoryLink;

/**
 *
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategLinkServiceImpl implements MCRCategLinkService {

    private static MCRHIBConnection HIB_CONNECTION_INSTANCE;

    private static Logger LOGGER = LogManager.getLogger(MCRCategLinkServiceImpl.class);

    private static Class<MCRCategoryLinkImpl> LINK_CLASS = MCRCategoryLinkImpl.class;

    private static final String NAMED_QUERY_NAMESPACE = "MCRCategoryLink.";

    private static MCRCache<MCRCategoryID, MCRCategory> categCache = new MCRCache<>(
        MCRConfiguration.instance().getInt("MCR.Classifications.LinkServiceImpl.CategCache.Size", 1000),
        "MCRCategLinkService category cache");

    private static MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    public MCRCategLinkServiceImpl() {
        HIB_CONNECTION_INSTANCE = MCRHIBConnection.instance();
    }

    @Override
    public Map<MCRCategoryID, Number> countLinks(MCRCategory parent, boolean childrenOnly) {
        return countLinksForType(parent, null, childrenOnly);
    }

    @Override
    public Map<MCRCategoryID, Number> countLinksForType(MCRCategory parent, String type, boolean childrenOnly) {
        boolean restrictedByType = type != null;
        String queryName;
        if (childrenOnly) {
            queryName = restrictedByType ? "NumberByTypePerChildOfParentID" : "NumberPerChildOfParentID";
        } else {
            queryName = restrictedByType ? "NumberByTypePerClassID" : "NumberPerClassID";
        }
        Map<MCRCategoryID, Number> countLinks = new HashMap<>();
        Collection<MCRCategoryID> ids = childrenOnly ? getAllChildIDs(parent) : getAllCategIDs(parent);
        for (MCRCategoryID id : ids) {
            // initialize all categIDs with link count of zero
            countLinks.put(id, 0);
        }
        //have to use rootID here if childrenOnly=false
        //old classification browser/editor could not determine links correctly otherwise
        if (!childrenOnly) {
            parent = parent.getRoot();
        } else if (!(parent instanceof MCRCategoryImpl) || ((MCRCategoryImpl) parent).getInternalID() == 0) {
            parent = MCRCategoryDAOImpl.getByNaturalID(MCREntityManagerProvider.getCurrentEntityManager(),
                parent.getId());
        }
        LOGGER.info("parentID:{}", parent.getId());
        String classID = parent.getId().getRootID();
        Query<?> q = HIB_CONNECTION_INSTANCE.getNamedQuery(NAMED_QUERY_NAMESPACE + queryName);
        // query can take long time, please cache result
        q.setCacheable(true);
        q.setReadOnly(true);
        q.setParameter("classID", classID);
        if (childrenOnly) {
            q.setParameter("parentID", ((MCRCategoryImpl) parent).getInternalID());
        }
        if (restrictedByType) {
            q.setParameter("type", type);
        }
        // get object count for every category (not accumulated)
        @SuppressWarnings("unchecked")
        List<Object[]> result = (List<Object[]>) q.getResultList();
        for (Object[] sr : result) {
            MCRCategoryID key = new MCRCategoryID(classID, sr[0].toString());
            Number value = (Number) sr[1];
            countLinks.put(key, value);
        }
        return countLinks;
    }

    @Override
    public void deleteLink(MCRCategLinkReference reference) {
        Query<?> q = HIB_CONNECTION_INSTANCE.getNamedQuery(NAMED_QUERY_NAMESPACE + "deleteByObjectID");
        q.setParameter("id", reference.getObjectID());
        q.setParameter("type", reference.getType());
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: {}", deleted);
    }

    @Override
    public void deleteLinks(final Collection<MCRCategLinkReference> ids) {
        if (ids.isEmpty()) {
            return;
        }
        HashMap<String, Collection<String>> typeMap = new HashMap<>();
        //prepare
        Collection<String> objectIds = new LinkedList<>();
        String currentType = ids.iterator().next().getType();
        typeMap.put(currentType, objectIds);
        //collect per type
        for (MCRCategLinkReference ref : ids) {
            if (!currentType.equals(ref.getType())) {
                currentType = ref.getType();
                objectIds = typeMap.computeIfAbsent(ref.getType(), k -> new LinkedList<>());
            }
            objectIds.add(ref.getObjectID());
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        javax.persistence.Query q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "deleteByObjectCollection");
        int deleted = 0;
        for (Map.Entry<String, Collection<String>> entry : typeMap.entrySet()) {
            q.setParameter("ids", entry.getValue());
            q.setParameter("type", entry.getKey());
            deleted += q.executeUpdate();
        }
        LOGGER.debug("Number of Links deleted: {}", deleted);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        Query<?> q = HIB_CONNECTION_INSTANCE.getNamedQuery(NAMED_QUERY_NAMESPACE + "ObjectIDByCategory");
        q.setCacheable(true);
        q.setParameter("id", id);
        q.setReadOnly(true);
        return (Collection<String>) q.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        Query<?> q = HIB_CONNECTION_INSTANCE.getNamedQuery(NAMED_QUERY_NAMESPACE + "ObjectIDByCategoryAndType");
        q.setCacheable(true);
        q.setParameter("id", id);
        q.setParameter("type", type);
        q.setReadOnly(true);
        return (Collection<String>) q.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MCRCategoryID> getLinksFromReference(MCRCategLinkReference reference) {
        Query<?> q = HIB_CONNECTION_INSTANCE.getNamedQuery(NAMED_QUERY_NAMESPACE + "categoriesByObjectID");
        q.setCacheable(true);
        q.setParameter("id", reference.getObjectID());
        q.setParameter("type", reference.getType());
        q.setReadOnly(true);
        return (Collection<MCRCategoryID>) q.getResultList();
    }

    @Override
    public void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        categories
            .stream()
            .distinct()
            .forEach(categID -> {
                final MCRCategory category = getMCRCategory(entityManager, categID);
                if (category == null) {
                    throw new MCRPersistenceException("Could not link to unknown category " + categID);
                }
                MCRCategoryLinkImpl link = new MCRCategoryLinkImpl(category, objectReference);
                if (LOGGER.isDebugEnabled()) {
                    MCRCategory linkedCategory = link.getCategory();
                    StringBuilder debugMessage = new StringBuilder("Adding Link from ").append(linkedCategory.getId());
                    if (linkedCategory instanceof MCRCategoryImpl) {
                        debugMessage.append("(").append(((MCRCategoryImpl) linkedCategory).getInternalID())
                            .append(") ");
                    }
                    debugMessage.append("to ").append(objectReference);
                    LOGGER.debug(debugMessage.toString());
                }
                entityManager.persist(link);
                LOGGER.debug("===DONE: {}", link.id);
            });
    }

    private static MCRCategory getMCRCategory(EntityManager entityManager, MCRCategoryID categID) {
        MCRCategory categ = categCache.getIfUpToDate(categID, DAO.getLastModified());
        if (categ != null) {
            return categ;
        }
        categ = MCRCategoryDAOImpl.getByNaturalID(entityManager, categID);
        if (categ == null) {
            return null;
        }
        categCache.put(categID, categ);
        return categ;
    }

    @Override
    public Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category) {
        if (category == null) {
            return hasLinksForClassifications();
        }

        MCRCategoryImpl rootImpl = (MCRCategoryImpl) MCRCategoryDAOFactory.getInstance()
            .getCategory(category.getRoot().getId(), -1);
        if (rootImpl == null) {
            //Category does not exist, so it has no links
            return getNoLinksMap(category);
        }
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<>();
        final BitSet linkedInternalIds = getLinkedInternalIds();
        storeHasLinkValues(boolMap, linkedInternalIds, rootImpl);
        return boolMap;
    }

    @SuppressWarnings("unchecked")
    private Map<MCRCategoryID, Boolean> hasLinksForClassifications() {
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<MCRCategoryID, Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean get(Object key) {
                return Optional.ofNullable(super.get(key)).orElse(Boolean.FALSE);
            }
        };
        Query<?> linkedClassifications = MCRHIBConnection.instance()
            .getNamedQuery(NAMED_QUERY_NAMESPACE + "linkedClassifications");
        linkedClassifications.setReadOnly(true);
        ((List<String>) linkedClassifications.getResultList()).stream().map(MCRCategoryID::rootID)
            .forEach(id -> boolMap.put(id, true));
        return boolMap;
    }

    private Map<MCRCategoryID, Boolean> getNoLinksMap(MCRCategory category) {
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<>();
        for (MCRCategoryID categID : getAllCategIDs(category)) {
            boolMap.put(categID, false);
        }
        return boolMap;
    }

    private void storeHasLinkValues(HashMap<MCRCategoryID, Boolean> boolMap, BitSet internalIds,
        MCRCategoryImpl parent) {
        final int internalID = parent.getInternalID();
        if (internalID < internalIds.size() && internalIds.get(internalID)) {
            addParentHasValues(boolMap, parent);
        } else {
            boolMap.put(parent.getId(), false);
        }
        for (MCRCategory child : parent.getChildren()) {
            storeHasLinkValues(boolMap, internalIds, (MCRCategoryImpl) child);
        }
    }

    private void addParentHasValues(HashMap<MCRCategoryID, Boolean> boolMap, MCRCategory parent) {
        boolMap.put(parent.getId(), true);
        if (parent.isCategory() && !Optional.ofNullable(boolMap.get(parent.getParent().getId())).orElse(false)) {
            addParentHasValues(boolMap, parent.getParent());
        }
    }

    private BitSet getLinkedInternalIds() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> query = cb.createQuery(Number.class);
        Root<MCRCategoryLinkImpl> li = query.from(LINK_CLASS);
        Path<Integer> internalId = li.get(MCRCategoryLinkImpl_.category).get(MCRCategoryImpl_.internalID);
        List<Number> result = em
            .createQuery(
                query.select(internalId)
                    .orderBy(cb.desc(internalId)))
            .getResultList();

        int maxSize = result.size() == 0 ? 1 : result.get(0).intValue() + 1;
        BitSet linkSet = new BitSet(maxSize);
        for (Number internalID : result) {
            linkSet.set(internalID.intValue(), true);
        }
        return linkSet;
    }

    private static Collection<MCRCategoryID> getAllCategIDs(MCRCategory category) {
        return MCRStreamUtils.flatten(category, MCRCategory::getChildren, Collection::parallelStream)
            .map(MCRCategory::getId)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private static Collection<MCRCategoryID> getAllChildIDs(MCRCategory category) {
        return category.getChildren()
            .stream()
            .map(MCRCategory::getId)
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public boolean hasLink(MCRCategory mcrCategory) {
        return !hasLinks(mcrCategory).isEmpty();
    }

    @Override
    public boolean isInCategory(MCRCategLinkReference reference, MCRCategoryID id) {
        Query<?> q = HIB_CONNECTION_INSTANCE.getNamedQuery(NAMED_QUERY_NAMESPACE + "CategoryAndObjectID");
        q.setCacheable(true);
        q.setReadOnly(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        q.setParameter("objectID", reference.getObjectID());
        q.setParameter("type", reference.getType());
        return !q.getResultList().isEmpty();
    }

    @Override
    public Collection<MCRCategLinkReference> getReferences(String type) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRCategLinkReference> query = cb.createQuery(MCRCategLinkReference.class);
        Root<MCRCategoryLinkImpl> li = query.from(LINK_CLASS);
        Path<MCRCategLinkReference> objectReferencePath = li.get(MCRCategoryLinkImpl_.objectReference);
        return em
            .createQuery(
                query.select(objectReferencePath)
                    .where(cb.equal(objectReferencePath.get(MCRCategLinkReference_.type), type)))
            .setHint(QueryHints.READ_ONLY, "true")
            .getResultList();
    }

    @Override
    public Collection<String> getTypes() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRCategoryLinkImpl> li = query.from(LINK_CLASS);
        return em
            .createQuery(
                query
                    .distinct(true)
                    .select(li.get(MCRCategoryLinkImpl_.objectReference).get(MCRCategLinkReference_.type)))
            .setHint(QueryHints.READ_ONLY, "true")
            .getResultList();
    }

    @Override
    public Collection<MCRCategoryLink> getLinks(String type) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRCategoryLink> query = cb.createQuery(MCRCategoryLink.class);
        Root<MCRCategoryLinkImpl> li = query.from(LINK_CLASS);
        return em
            .createQuery(
                query
                    .where(
                        cb.equal(li.get(MCRCategoryLinkImpl_.objectReference).get(MCRCategLinkReference_.type), type)))
            .setHint(QueryHints.READ_ONLY, "true")
            .getResultList();
    }

}
