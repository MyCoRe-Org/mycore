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
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.END_LEVEL;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.INCREMENT;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.LANG;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.LEFT;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.RIGHT;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.ROOT_ID;
import static org.mycore.datamodel.classifications2.impl.MCRCategoryQueryParameter.TEXT;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Thomas Scheffler (yagee)
 *
 * @since 2.0
 */
public class MCRCategoryDAOImpl implements MCRCategoryDAO {

    private static final int LEVEL_START_VALUE = 0;

    private static final int LEFT_START_VALUE = 0;

    private static long lastModified = System.currentTimeMillis();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String NAMED_QUERY_NAMESPACE = "MCRCategory.";

    private static final Map<String, Long> LAST_MODIFIED_MAP = new HashMap<>();

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category) {
        int position = -1;
        if (category instanceof MCRCategoryImpl catImpl) {
            position = catImpl.getPositionInParent();
        }
        return addCategory(parentID, category, position);
    }

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        if (exist(category.getId())) {
            throw new MCRException("Cannot add category. A category with ID " + category.getId() + " already exists");
        }
        return withoutFlush(MCREntityManagerProvider.getCurrentEntityManager(), false, entityManager -> {
            //we do direct DB manipulation, so flush and clear session first
            entityManager.flush();
            entityManager.clear();
            int leftStart = LEFT_START_VALUE;
            int levelStart = LEVEL_START_VALUE;
            MCRCategoryImpl parent = null;
            if (parentID != null) {
                parent = getByNaturalID(entityManager, parentID);
                levelStart = parent.getLevel() + 1;
                leftStart = parent.getRight();
                if (position > parent.getChildren().size()) {
                    throw new IndexOutOfBoundsException(
                        "Cannot add category as child #" + position + ", when there are only "
                            + parent.getChildren().size() + " children.");
                }
            }
            LOGGER.debug("Calculating LEFT,RIGHT and LEVEL attributes...");
            final MCRCategoryImpl wrapCategory = MCRCategoryImpl.wrapCategory(category, parent,
                parent == null ? category.getRoot() : parent.getRoot());
            wrapCategory.calculateLeftRightAndLevel(leftStart, levelStart);
            // always add +1 for the current node
            int nodes = 1 + (wrapCategory.getRight() - wrapCategory.getLeft()) / 2;
            LOGGER.debug("Calculating LEFT,RIGHT and LEVEL attributes. Done! Nodes: {}", nodes);
            if (parentID != null) {
                final int increment = nodes * 2;
                int parentLeft = parent.getLeft();
                updateLeftRightValue(entityManager, parentID.getRootID(), leftStart, increment);
                entityManager.flush();
                if (position < 0) {
                    parent.getChildren().add(category);
                } else {
                    parent.getChildren().add(position, category);
                }
                parent.calculateLeftRightAndLevel(Integer.MAX_VALUE / 2, parent.getLevel());
                entityManager.flush();
                parent.calculateLeftRightAndLevel(parentLeft, parent.getLevel());
            }
            entityManager.persist(category);
            LOGGER.info("Category {} saved.", category::getId);
            updateTimeStamp();

            updateLastModified(category.getRoot().getId().toString());
            return parent;
        });
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        LOGGER.debug("Will get: {}", id);
        MCRCategoryImpl category = getByNaturalID(entityManager, id);
        try {
            entityManager.refresh(category); //for MCR-1863
        } catch (EntityNotFoundException e) {
            //required since hibernate 5.3 if category is deleted within same transaction.
            //junit: testLicenses()
        }
        if (category == null) {
            throw new MCRPersistenceException("Category " + id + " was not found. Delete aborted.");
        }
        LOGGER.debug("Will delete: {}", category::getId);
        MCRCategory parent = category.parent;
        category.detachFromParent();
        entityManager.remove(category);
        if (parent != null) {
            entityManager.flush();
            LOGGER.debug("Left: {} Right: {}", category::getLeft, category::getRight);
            // always add +1 for the currentNode
            int nodes = 1 + (category.getRight() - category.getLeft()) / 2;
            final int increment = nodes * -2;
            // decrement left and right values by nodes
            updateLeftRightValue(entityManager, category.getRootID(), category.getLeft(), increment);
        }
        updateTimeStamp();
        updateLastModified(category.getRootID());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.datamodel.classifications2.MCRCategoryDAO#exist(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @Override
    public boolean exist(MCRCategoryID id) {
        return getLeftRightLevelValues(MCREntityManagerProvider.getCurrentEntityManager(), id) != null;
    }

    @Override
    public List<MCRCategory> getCategoriesByLabel(final String lang, final String text) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        return cast(entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE + "byLabel", MCRCategoryImpl.class)
            .setParameter(LANG, lang)
            .setParameter(TEXT, text)
            .getResultList());
    }

    @Override
    public List<MCRCategory> getCategoriesByClassAndLang(final String classId, final String lang) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        return cast(entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE + "byClassAndLang", MCRCategoryImpl.class)
            .setParameter(CLASS_ID, classId)
            .setParameter(LANG, lang)
            .getResultList());
    }

    @Override
    public List<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategoryDTO leftRight = getLeftRightLevelValues(entityManager, baseID);
        return cast(entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "byLabelInClass", MCRCategoryImpl.class)
            .setParameter(ROOT_ID, baseID.getRootID())
            .setParameter(LEFT, leftRight.leftValue)
            .setParameter(RIGHT, leftRight.rightValue)
            .setParameter(LANG, lang)
            .setParameter(TEXT, text)
            .getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        final boolean fetchAllChildren = childLevel < 0;
        Query q;
        if (id.isRootID()) {
            q = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE
                + (fetchAllChildren ? "prefetchClassQuery" : "prefetchClassLevelQuery"));
            if (!fetchAllChildren) {
                q.setParameter(END_LEVEL, childLevel);
            }
            q.setParameter(CLASS_ID, id.getRootID());
        } else {
            //normal category
            MCRCategoryDTO leftRightLevel = getLeftRightLevelValues(entityManager, id);
            if (leftRightLevel == null) {
                return null;
            }
            q = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE
                + (fetchAllChildren ? "prefetchCategQuery" : "prefetchCategLevelQuery"));
            if (!fetchAllChildren) {
                q.setParameter(END_LEVEL, leftRightLevel.level + childLevel);
            }
            q.setParameter(CLASS_ID, id.getRootID());
            q.setParameter(LEFT, leftRightLevel.leftValue);
            q.setParameter(RIGHT, leftRightLevel.rightValue);
        }
        List<MCRCategoryDTO> result = q.getResultList();
        if (result.isEmpty()) {
            LOGGER.warn("Could not load category: {}", id);
            return null;
        }
        return buildCategoryFromPrefetchedList(result, id);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @Override
    public List<MCRCategory> getChildren(MCRCategoryID cid) {
        LOGGER.debug("Get children of category: {}", cid);
        return Optional.ofNullable(cid)
            .map(id -> getCategory(id, 1))
            .map(MCRCategory::getChildren)
            .map(l -> l
                .parallelStream()
                .toList() //temporary copy for detachFromParent
                .parallelStream()
                .map(MCRCategoryImpl.class::cast)
                .peek(MCRCategoryImpl::detachFromParent)
                .map(MCRCategory.class::cast)
                .collect(Collectors.toList()))
            .orElse(new MCRCategoryChildList(null, null));
    }

    @Override
    public List<MCRCategory> getParents(MCRCategoryID id) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategoryDTO leftRight = getLeftRightLevelValues(entityManager, id);
        if (leftRight == null) {
            return null;
        }
        Query parentQuery = entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "parentQuery")
            .setParameter(CLASS_ID, id.getRootID())
            .setParameter(CATEGORY_ID, id.getId())
            .setParameter(LEFT, leftRight.leftValue)
            .setParameter(RIGHT, leftRight.rightValue);
        @SuppressWarnings("unchecked")
        List<MCRCategoryDTO> resultList = parentQuery.getResultList();
        MCRCategory category = buildCategoryFromPrefetchedList(resultList, id);
        List<MCRCategory> parents = new ArrayList<>();
        while (category.getParent() != null) {
            category = category.getParent();
            parents.add(category);
        }
        return parents;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MCRCategoryID> getRootCategoryIDs() {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        return entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE + "rootIds").getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MCRCategory> getRootCategories() {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        List<MCRCategoryDTO> resultList = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE + "rootCategs")
            .getResultList();
        BiConsumer<List<MCRCategory>, MCRCategoryImpl> merge = (l, c) -> {
            MCRCategoryImpl last = (MCRCategoryImpl) l.getLast();
            if (last.getInternalID() != c.getInternalID()) {
                l.add(c);
            } else {
                last.getLabels().addAll(c.getLabels());
            }
        };
        return resultList.parallelStream()
            .map(c -> c.merge(null))
            .collect(Collector.of(ArrayList::new,
                (List<MCRCategory> l, MCRCategoryImpl c) -> {
                    if (l.isEmpty()) {
                        l.add(c);
                    } else {
                        merge.accept(l, c);
                    }
                }, (l, r) -> {
                    if (l.isEmpty()) {
                        return r;
                    }
                    if (r.isEmpty()) {
                        return l;
                    }
                    MCRCategoryImpl first = (MCRCategoryImpl) r.getFirst();
                    merge.accept(l, first);
                    l.addAll(r.subList(1, r.size()));
                    return l;
                }));
    }

    @Override
    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        return Optional.ofNullable(getCategory(baseID, childLevel))
            .map(c -> {
                if (baseID.isRootID()) {
                    return c;
                }
                List<MCRCategory> parents = getParents(baseID);
                MCRCategory parent = parents.getFirst();
                c.getChildren()
                    .stream()
                    .toList()
                    .stream()
                    .map(MCRCategoryImpl.class::cast)
                    .peek(MCRCategoryImpl::detachFromParent)
                    .forEachOrdered(parent.getChildren()::add);
                // return root node
                return parents.getLast();
            })
            .orElse(null);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#hasChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @Override
    public boolean hasChildren(MCRCategoryID cid) {
        // SELECT * FROM MCRCATEGORY WHERE PARENTID=(SELECT INTERNALID FROM
        // MCRCATEGORY WHERE rootID=cid.getRootID() and ID...);
        return getNumberOfChildren(MCREntityManagerProvider.getCurrentEntityManager(), cid) > 0;
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
        int index = getNumberOfChildren(MCREntityManagerProvider.getCurrentEntityManager(), newParentID);
        moveCategory(id, newParentID, index);
    }

    private MCRCategoryImpl getCommonAncestor(EntityManager entityManager, MCRCategoryImpl node1,
        MCRCategoryImpl node2) {
        if (!node1.getRootID().equals(node2.getRootID())) {
            return null;
        }
        if (node1.getLeft() == 0) {
            return node1;
        }
        if (node2.getLeft() == 0) {
            return node2;
        }
        int left = Math.min(node1.getLeft(), node2.getLeft());
        int right = Math.max(node1.getRight(), node2.getRight());
        Query q = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE + "commonAncestor")
            .setMaxResults(1)
            .setParameter(LEFT, left)
            .setParameter(RIGHT, right)
            .setParameter(ROOT_ID, node1.getRootID());
        return getSingleResult(q);
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        withoutFlush(MCREntityManagerProvider.getCurrentEntityManager(), true, e -> {
            MCRCategoryImpl subTree = getByNaturalID(MCREntityManagerProvider.getCurrentEntityManager(), id);
            MCRCategoryImpl oldParent = subTree.getParent();
            MCRCategoryImpl newParent = getByNaturalID(MCREntityManagerProvider.getCurrentEntityManager(), newParentID);
            MCRCategoryImpl commonAncestor = getCommonAncestor(MCREntityManagerProvider.getCurrentEntityManager(),
                oldParent, newParent);
            subTree.detachFromParent();
            LOGGER.debug("Add subtree to new Parent at index: {}", index);
            newParent.getChildren().add(index, subTree);
            subTree.parent = newParent;
            MCREntityManagerProvider.getCurrentEntityManager().flush();
            int left = commonAncestor.getLeft();
            commonAncestor.calculateLeftRightAndLevel(Integer.MAX_VALUE / 2, commonAncestor.getLevel());
            e.flush();
            commonAncestor.calculateLeftRightAndLevel(left, commonAncestor.getLevel());
            updateTimeStamp();
            updateLastModified(id.getRootID());
        });
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategoryImpl category = getByNaturalID(entityManager, id);
        category.getLabel(lang).ifPresent(oldLabel -> {
            category.getLabels().remove(oldLabel);
            updateTimeStamp();
            updateLastModified(category.getRootID());
        });
        return category;
    }

    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        if (!exist(newCategory.getId())) {
            throw new IllegalArgumentException(
                "MCRCategory can not be replaced. MCRCategoryID '" + newCategory.getId() + "' is unknown.");
        }
        return withoutFlush(MCREntityManagerProvider.getCurrentEntityManager(), true, em -> {
            MCRCategoryImpl oldCategory = getByNaturalID(MCREntityManagerProvider.getCurrentEntityManager(),
                newCategory.getId());
            int oldLevel = oldCategory.getLevel();
            int oldLeft = oldCategory.getLeft();
            // old Map with all Categories referenced by ID
            Map<MCRCategoryID, MCRCategoryImpl> oldMap = toMap(oldCategory);
            final MCRCategoryImpl copyDeepImpl = copyDeep(newCategory, -1);
            MCRCategoryImpl newCategoryImpl = MCRCategoryImpl.wrapCategory(copyDeepImpl, oldCategory.getParent(),
                oldCategory.getRoot());
            // new Map with all Categories referenced by ID
            Map<MCRCategoryID, MCRCategoryImpl> newMap = toMap(newCategoryImpl);
            //remove;
            oldMap
                .entrySet()
                .stream()
                .filter(c -> !newMap.containsKey(c.getKey()))
                .map(Map.Entry::getValue)
                .peek(MCRCategoryDAOImpl::remove)
                .forEach(c -> LOGGER.info("remove category: {}", c.getId()));
            oldMap.clear();
            oldMap.putAll(toMap(oldCategory));
            //sync labels/uris;
            MCRStreamUtils
                .flatten(oldCategory, MCRCategory::getChildren, Collection::stream)
                .filter(c -> newMap.containsKey(c.getId()))
                .map(MCRCategoryImpl.class::cast)
                .map(c -> new AbstractMap.SimpleEntry<>(c, newMap.get(c.getId())))
                // key: category of old version, value: category of new version
                .peek(e -> syncLabels(e.getValue(), e.getKey())) //sync from new to old version
                .forEach(e -> e.getKey().setURI(e.getValue().getURI()));
            //detach all categories, we will rebuild tree structure later
            oldMap
                .values()
                .stream()
                .filter(c -> c.getInternalID() != oldCategory.getInternalID()) //do not detach root of subtree
                .forEach(MCRCategoryImpl::detachFromParent);
            //rebuild
            MCRStreamUtils
                .flatten(newCategoryImpl, MCRCategory::getChildren, Collection::stream)
                .forEachOrdered(c -> {
                    MCRCategoryImpl oldC = oldMap.get(c.getId());
                    oldC.setChildren(
                        c
                            .getChildren()
                            .stream()
                            .map(cc -> {
                                //to categories of stored version or copy from new version
                                MCRCategoryImpl oldCC = oldMap.get(cc.getId());
                                if (oldCC == null) {
                                    oldCC = new MCRCategoryImpl();
                                    oldCC.setId(cc.getId());
                                    oldCC.setURI(cc.getURI());
                                    oldCC.getLabels().addAll(cc.getLabels());
                                    oldMap.put(oldCC.getId(), oldCC);
                                }
                                return oldCC;
                            })
                            .collect(Collectors.toList()));
                });
            oldCategory.calculateLeftRightAndLevel(Integer.MAX_VALUE / 2, oldLevel);
            em.flush();
            oldCategory.calculateLeftRightAndLevel(oldLeft, oldLevel);
            updateTimeStamp();
            updateLastModified(newCategory.getId().getRootID());
            return newMap.values();
        });
    }

    private static Map<MCRCategoryID, MCRCategoryImpl> toMap(MCRCategoryImpl oldCategory) {
        return MCRStreamUtils
            .flatten(oldCategory, MCRCategory::getChildren, Collection::stream)
            .collect(Collectors.toMap(MCRCategory::getId, MCRCategoryImpl.class::cast));
    }

    private static void remove(MCRCategoryImpl category) {
        if (category.hasChildren()) {
            int parentPos = category.getPositionInParent();
            MCRCategoryImpl parent = category.getParent();
            List<MCRCategoryImpl> copy = new ArrayList(category.children);
            copy.forEach(MCRCategoryImpl::detachFromParent);
            parent.children.addAll(parentPos, copy);
            copy.forEach(c -> c.parent = parent); //fixes MCR-1963
        }
        category.detachFromParent();
        MCREntityManagerProvider.getCurrentEntityManager().remove(category);
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategoryImpl category = getByNaturalID(entityManager, id);
        category.getLabel(label.getLang()).ifPresent(category.getLabels()::remove);
        category.getLabels().add(label);
        updateTimeStamp();
        updateLastModified(category.getRootID());
        return category;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, SortedSet<MCRLabel> labels) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategoryImpl category = getByNaturalID(entityManager, id);
        category.setLabels(labels);
        updateTimeStamp();
        updateLastModified(category.getRootID());
        return category;
    }

    @Override
    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRCategoryImpl category = getByNaturalID(entityManager, id);
        category.setURI(uri);
        updateTimeStamp();
        updateLastModified(category.getRootID());
        return category;
    }

    public void repairLeftRightValue(String classID) {
        final MCRCategoryID rootID = new MCRCategoryID(classID);
        withoutFlush(MCREntityManagerProvider.getCurrentEntityManager(), true, entityManager -> {
            MCRCategoryImpl classification = getByNaturalID(entityManager, rootID);
            classification.calculateLeftRightAndLevel(Integer.MAX_VALUE / 2, LEVEL_START_VALUE);
            entityManager.flush();
            classification.calculateLeftRightAndLevel(LEFT_START_VALUE, LEVEL_START_VALUE);
        });
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    private static void updateTimeStamp() {
        lastModified = System.currentTimeMillis();
    }

    private static MCRCategoryImpl buildCategoryFromPrefetchedList(List<MCRCategoryDTO> list, MCRCategoryID returnID) {
        LOGGER.debug(() -> "using prefetched list: " + list);
        MCRCategoryImpl predecessor = null;
        for (MCRCategoryDTO entry : list) {
            predecessor = entry.merge(predecessor);
        }
        return MCRStreamUtils.flatten(predecessor.getRoot(), MCRCategory::getChildren, Collection::parallelStream)
            .filter(c -> c.getId().equals(returnID))
            .findFirst()
            .map(MCRCategoryImpl.class::cast)
            .orElseThrow(() -> new MCRException("Could not find " + returnID + " in database result."));
    }

    private static MCRCategoryImpl copyDeep(MCRCategory category, int level) {
        if (category == null) {
            return null;
        }
        MCRCategoryImpl newCateg = new MCRCategoryImpl();
        int childAmount;
        try {
            childAmount = level != 0 ? category.getChildren().size() : 0;
        } catch (RuntimeException e) {
            LOGGER.error(() -> "Cannot get children size for category: " + category.getId(), e);
            throw e;
        }
        newCateg.setChildren(new ArrayList<>(childAmount));
        newCateg.setId(category.getId());
        newCateg.setLabels(category.getLabels());
        newCateg.setRoot(category.getRoot());
        newCateg.setURI(category.getURI());
        newCateg.setLevel(category.getLevel());
        if (category instanceof MCRCategoryImpl catImpl) {
            //to allow optimized hasChildren() to work without db query
            newCateg.setLeft(catImpl.getLeft());
            newCateg.setRight(catImpl.getRight());
            newCateg.setInternalID(catImpl.getInternalID());
        }
        if (childAmount > 0) {
            for (MCRCategory child : category.getChildren()) {
                newCateg.getChildren().add(copyDeep(child, level - 1));
            }
        }
        return newCateg;
    }

    /**
     * Returns database backed MCRCategoryImpl
     * <p>
     * every change to the returned MCRCategory is reflected in the database.
     */
    public static MCRCategoryImpl getByNaturalID(EntityManager entityManager, MCRCategoryID id) {
        TypedQuery<MCRCategoryImpl> naturalIDQuery = entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "byNaturalId", MCRCategoryImpl.class)
            .setParameter(CLASS_ID, id.getRootID())
            .setParameter(CATEGORY_ID, id.getId());
        return getSingleResult(naturalIDQuery);
    }

    private static void syncLabels(MCRCategoryImpl source, MCRCategoryImpl target) {
        for (MCRLabel newLabel : source.getLabels()) {
            Optional<MCRLabel> label = target.getLabel(newLabel.getLang());
            if (label.isEmpty()) {
                // copy new label
                target.getLabels().add(newLabel);
            }
            label.ifPresent(oldLabel -> {
                if (!oldLabel.getText().equals(newLabel.getText())) {
                    oldLabel.setText(newLabel.getText());
                }
                if (!oldLabel.getDescription().equals(newLabel.getDescription())) {
                    oldLabel.setDescription(newLabel.getDescription());
                }
            });
        }
        // remove labels that are not present in new version
        target.getLabels().removeIf(mcrLabel -> !source.getLabel(mcrLabel.getLang()).isPresent());
    }

    private static MCRCategoryDTO getLeftRightLevelValues(EntityManager entityManager, MCRCategoryID id) {
        return getSingleResult(entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "leftRightLevelQuery")
            .setParameter(CATEGORY_ID, id));
    }

    private static int getNumberOfChildren(EntityManager entityManager, MCRCategoryID id) {
        return getSingleResult(entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "childCount")
            .setParameter(CLASS_ID, id.getRootID())
            .setParameter(CATEGORY_ID, id.getId()));
    }

    private static void updateLeftRightValue(EntityManager entityManager, String classID, int left,
        final int increment) {
        withoutFlush(entityManager, true, e -> {
            LOGGER.debug("LEFT AND RIGHT values need updates. Left={}, increment by: {}", left, increment);
            Query leftQuery = e
                .createNamedQuery(NAMED_QUERY_NAMESPACE + "updateLeft")
                .setParameter(LEFT, left)
                .setParameter(INCREMENT, increment)
                .setParameter(CLASS_ID, classID);
            int leftChanges = leftQuery.executeUpdate();
            Query rightQuery = e
                .createNamedQuery(NAMED_QUERY_NAMESPACE + "updateRight")
                .setParameter(LEFT, left)
                .setParameter(INCREMENT, increment)
                .setParameter(CLASS_ID, classID);
            int rightChanges = rightQuery.executeUpdate();
            LOGGER.debug("Updated {} left and {} right values.", leftChanges, rightChanges);
        });
    }

    /**
     * Method updates the last modified timestamp, for the given root id.
     *
     */
    protected synchronized void updateLastModified(String root) {
        LAST_MODIFIED_MAP.put(root, System.currentTimeMillis());
    }

    /**
     * Gets the timestamp for the given root id. If there is not timestamp at the moment -1 is returned.
     *
     * @return the last modified timestamp (if any) or -1
     */
    @Override
    public long getLastModified(String root) {
        Long long1 = LAST_MODIFIED_MAP.get(root);
        if (long1 != null) {
            return long1;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getSingleResult(Query query) {
        try {
            return (T) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static List<MCRCategory> cast(List<MCRCategoryImpl> list) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<MCRCategory> temp = (List) list;
        return temp;
    }

    private static <T> T withoutFlush(EntityManager entityManager, boolean flushAtEnd,
        Function<EntityManager, T> task) {
        FlushModeType fm = entityManager.getFlushMode();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        try {
            T result = task.apply(entityManager);
            if (flushAtEnd) {
                entityManager.flush();
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } finally {
            entityManager.setFlushMode(fm);
        }
    }

    private static void withoutFlush(EntityManager entityManager, boolean flushAtEnd, Consumer<EntityManager> task) {
        withoutFlush(entityManager, flushAtEnd, e -> {
            task.accept(e);
            return null;
        });
    }
}
