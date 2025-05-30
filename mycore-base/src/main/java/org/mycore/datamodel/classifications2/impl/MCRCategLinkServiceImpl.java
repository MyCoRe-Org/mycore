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

package org.mycore.datamodel.classifications2.impl;

import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.CATEGORY_ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.CLASS_ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.IDS;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.OBJECT_ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.PARENT_ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.ROOT_ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.TYPE;

import java.io.Serial;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.jpa.AvailableHints;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkReference_;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategoryLink;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 *
 * @author Thomas Scheffler (yagee)
 *
 * @since 2.0
 */
public class MCRCategLinkServiceImpl implements MCRCategLinkService {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Class<MCRCategoryLinkImpl> LINK_CLASS = MCRCategoryLinkImpl.class;

    private static final String NAMED_QUERY_NAMESPACE = "MCRCategoryLink.";

    private static final MCRCache<MCRCategoryID, MCRCategory> CATEGORY_CACHE = new MCRCache<>(
        MCRConfiguration2.getInt("MCR.Classifications.LinkServiceImpl.CategCache.Size").orElse(1000),
        "MCRCategLinkService category cache");

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.obtainInstance();

    @Override
    public Map<MCRCategoryID, Number> countLinks(MCRCategory parent, boolean childrenOnly) {
        return countLinksForType(parent, null, childrenOnly);
    }

    @Override
    public Map<MCRCategoryID, Number> countLinksForType(MCRCategory parent, String type, boolean childrenOnly) {
        boolean restrictedByType = type != null;
        String queryName = determineLinkCountQueryName(childrenOnly, restrictedByType);
        Map<MCRCategoryID, Number> countLinks = initializeCountLinkMap(childrenOnly, parent);
        final EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategory parentRoot = getEffectiveParentRoot(parent, childrenOnly, entityManager);

        LOGGER.info("parentID:{}", parentRoot::getId);
        String classID = parentRoot.getId().getRootID();
        TypedQuery<Object[]> query = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE + queryName, Object[].class);
        // query can take long time, please cache result
        setCacheable(query);
        setReadOnly(query);
        setQueryParametersForLinkCount(classID, query, parent, childrenOnly, restrictedByType, type);
        populateCountLinkMap(countLinks, query, classID);

        return countLinks;
    }

    private void setQueryParametersForLinkCount(String classID, TypedQuery<Object[]> query, MCRCategory parent,
        boolean childrenOnly, boolean restrictedByType, String type) {
        query.setParameter(CLASS_ID, classID);
        if (childrenOnly) {
            query.setParameter(PARENT_ID, ((MCRCategoryImpl) parent).getInternalID());
        }
        if (restrictedByType) {
            query.setParameter(TYPE, type);
        }
        // get object count for every category (not accumulated)
    }

    private String determineLinkCountQueryName(boolean childrenOnly, boolean restrictedByType) {
        String queryName;
        if (childrenOnly) {
            queryName = restrictedByType ? "NumberByTypePerChildOfParentID" : "NumberPerChildOfParentID";
        } else {
            queryName = restrictedByType ? "NumberByTypePerClassID" : "NumberPerClassID";
        }
        return queryName;
    }

    private MCRCategory getEffectiveParentRoot(MCRCategory parent, boolean childrenOnly, EntityManager entityManager) {
        //have to use rootID here if childrenOnly=false
        //old classification browser/editor could not determine links correctly otherwise
        MCRCategory parentRoot = parent;
        if (!childrenOnly) {
            parentRoot = parent.getRoot();
        } else if (!(parent instanceof MCRCategoryImpl) || ((MCRCategoryImpl) parent).getInternalID() == 0) {
            parentRoot = MCRCategoryDAOImpl.getByNaturalID(entityManager, parent.getId());
        }
        return parentRoot;
    }

    private Map<MCRCategoryID, Number> initializeCountLinkMap(boolean childrenOnly, MCRCategory parent) {
        Map<MCRCategoryID, Number> countLinks = new HashMap<>();
        Collection<MCRCategoryID> ids = childrenOnly ? getAllChildIDs(parent) : getAllCategIDs(parent);
        for (MCRCategoryID id : ids) {
            // initialize all categIDs with link count of zero
            countLinks.put(id, 0);
        }
        return countLinks;
    }

    private void populateCountLinkMap(Map<MCRCategoryID, Number> countLinks, TypedQuery<Object[]> query,
        String classID) {
        List<Object[]> result = query.getResultList();
        for (Object[] streamResult : result) {
            MCRCategoryID key = new MCRCategoryID(classID, streamResult[0].toString());
            Number value = (Number) streamResult[1];
            countLinks.put(key, value);
        }
    }

    @Override
    public void deleteLink(MCRCategLinkReference reference) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        Query q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "deleteByObjectID");
        q.setParameter(ID, reference.getObjectID());
        q.setParameter(TYPE, reference.getType());
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: {}", deleted);
    }

    @Override
    public void deleteLinks(final Collection<MCRCategLinkReference> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Map<String, Collection<String>> typeMap = new HashMap<>();
        //prepare
        Collection<String> objectIds = new ArrayList<>();
        String currentType = ids.iterator().next().getType();
        typeMap.put(currentType, objectIds);
        //collect per type
        for (MCRCategLinkReference ref : ids) {
            if (!currentType.equals(ref.getType())) {
                currentType = ref.getType();
                objectIds = typeMap.computeIfAbsent(ref.getType(), k -> new ArrayList<>());
            }
            objectIds.add(ref.getObjectID());
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        Query q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "deleteByObjectCollection");
        int deleted = 0;
        for (Map.Entry<String, Collection<String>> entry : typeMap.entrySet()) {
            q.setParameter(IDS, entry.getValue());
            q.setParameter(TYPE, entry.getKey());
            deleted += q.executeUpdate();
        }
        LOGGER.debug("Number of Links deleted: {}", deleted);
    }

    @Override
    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "ObjectIDByCategory", String.class);
        setCacheable(q);
        q.setParameter(ID, id);
        setReadOnly(q);
        return q.getResultList();
    }

    @Override
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "ObjectIDByCategoryAndType", String.class);
        setCacheable(q);
        q.setParameter(ID, id);
        q.setParameter(TYPE, type);
        setReadOnly(q);
        return q.getResultList();
    }

    @Override
    public Collection<MCRCategoryID> getLinksFromReference(MCRCategLinkReference reference) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRCategoryID> q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "categoriesByObjectID",
            MCRCategoryID.class);
        setCacheable(q);
        q.setParameter(ID, reference.getObjectID());
        q.setParameter(TYPE, reference.getType());
        setReadOnly(q);
        return q.getResultList();
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
                        debugMessage.append('(').append(((MCRCategoryImpl) linkedCategory).getInternalID())
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
        MCRCategory categ = CATEGORY_CACHE.getIfUpToDate(categID, DAO.getLastModified());
        if (categ != null) {
            return categ;
        }
        categ = MCRCategoryDAOImpl.getByNaturalID(entityManager, categID);
        if (categ == null) {
            return null;
        }
        CATEGORY_CACHE.put(categID, categ);
        return categ;
    }

    @Override
    public Map<MCRCategoryID, Boolean> checkForLinks(MCRCategory category) {
        if (category == null) {
            return checkLinksForClassifications();
        }

        MCRCategoryImpl rootImpl = (MCRCategoryImpl) MCRCategoryDAOFactory.obtainInstance()
            .getCategory(category.getRoot().getId(), -1);
        if (rootImpl == null) {
            //Category does not exist, so it has no links
            return getNoLinksMap(category);
        }
        Map<MCRCategoryID, Boolean> boolMap = new HashMap<>();
        final BitSet linkedInternalIds = getLinkedInternalIds();
        storeHasLinkValues(boolMap, linkedInternalIds, rootImpl);
        return boolMap;
    }

    private Map<MCRCategoryID, Boolean> checkLinksForClassifications() {
        Map<MCRCategoryID, Boolean> boolMap = new HashMap<>() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean get(Object key) {
                return Optional.ofNullable(super.get(key)).orElse(Boolean.FALSE);
            }
        };
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> linkedClassifications = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "linkedClassifications",
            String.class);
        setReadOnly(linkedClassifications);
        linkedClassifications.getResultList()
            .stream().map(MCRCategoryID::new)
            .forEach(id -> boolMap.put(id, true));
        return boolMap;
    }

    private Map<MCRCategoryID, Boolean> getNoLinksMap(MCRCategory category) {
        Map<MCRCategoryID, Boolean> boolMap = new HashMap<>();
        for (MCRCategoryID categID : getAllCategIDs(category)) {
            boolMap.put(categID, false);
        }
        return boolMap;
    }

    private void storeHasLinkValues(Map<MCRCategoryID, Boolean> boolMap, BitSet internalIds,
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

    private void addParentHasValues(Map<MCRCategoryID, Boolean> boolMap, MCRCategory parent) {
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

        int maxSize = result.isEmpty() ? 1 : result.getFirst().intValue() + 1;
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
        return !checkForLinks(mcrCategory).isEmpty();
    }

    @Override
    public boolean isInCategory(MCRCategLinkReference reference, MCRCategoryID id) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        Query q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "CategoryAndObjectID");
        setCacheable(q);
        setReadOnly(q);
        q.setParameter(ROOT_ID, id.getRootID());
        q.setParameter(CATEGORY_ID, id.getId());
        q.setParameter(OBJECT_ID, reference.getObjectID());
        q.setParameter(TYPE, reference.getType());
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
            .setHint(AvailableHints.HINT_READ_ONLY, "true")
            .getResultList();
    }

    @Override
    public Collection<String> getTypes() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "types", String.class);
        return q.getResultList();
    }

    @Override
    public Collection<MCRCategoryLink> getLinks(String type) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRCategoryLink> q = em.createNamedQuery(NAMED_QUERY_NAMESPACE + "links", MCRCategoryLink.class);
        q.setParameter(TYPE, type);
        return q.getResultList();
    }

    private static void setReadOnly(Query query) {
        query.setHint("org.hibernate.readOnly", Boolean.TRUE);
    }

    private static void setCacheable(Query query) {
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
    }

}
