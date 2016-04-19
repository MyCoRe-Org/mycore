/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date: 2008-02-06 17:27:24 +0000 (Mi, 06 Feb
 *          2008) $
 * @since 2.0
 */
public class MCRCategoryDAOImpl implements MCRCategoryDAO {

    private static MCRHIBConnection HIB_CONNECTION_INSTANCE;

    private static final int LEVEL_START_VALUE = 0;

    private static final int LEFT_START_VALUE = 0;

    private static long LAST_MODIFIED = System.currentTimeMillis();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Class<MCRCategoryImpl> CATEGRORY_CLASS = MCRCategoryImpl.class;

    private static final String NAMED_QUERY_NAMESPACE = "MCRCategory.";

    private static HashMap<String, Long> LAST_MODIFIED_MAP = new HashMap<String, Long>();

    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category) {
        int position = -1;
        if (category instanceof MCRCategoryImpl) {
            position = ((MCRCategoryImpl) category).getPositionInParent();
        }
        return addCategory(parentID, category, position);
    }

    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        if (exist(category.getId())) {
            throw new MCRException("Cannot add category. A category with ID " + category.getId() + " already exists");
        }
        int leftStart = LEFT_START_VALUE;
        int levelStart = LEVEL_START_VALUE;
        Session session = getHibConnection().getSession();
        //we do direct DB manipulation, so flush and clear session first
        session.flush();
        session.clear();
        MCRCategoryImpl parent = null;
        if (parentID != null) {
            parent = getByNaturalID(session, parentID);
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
        LOGGER.debug("Calculating LEFT,RIGHT and LEVEL attributes. Done! Nodes: " + nodes);
        if (parentID != null) {
            final int increment = nodes * 2;
            if (position < 0) {
                updateLeftRightValue(getHibConnection(), parentID.getRootID(), leftStart, increment);
                parent.getChildren().add(category);
            } else {
                parent.getChildren().add(position, category);
                parent.calculateLeftRightAndLevel(parent.getLeft(), parent.getLevel());
            }
        }
        session.save(category);
        LOGGER.info("Category " + category.getId() + " saved.");
        updateTimeStamp();

        updateLastModified(category.getRoot().getId().toString());
        return parent;
    }

    public void deleteCategory(MCRCategoryID id) {
        final MCRHIBConnection connection = getHibConnection();
        Session session = connection.getSession();
        LOGGER.debug("Will get: " + id);
        MCRCategoryImpl category = getByNaturalID(session, id);
        if (category == null) {
            throw new MCRPersistenceException("Category " + id + " was not found. Delete aborted.");
        }
        LOGGER.debug("Will delete: " + category.getId());
        MCRCategory parent = category.parent;
        category.detachFromParent();
        session.delete(category);
        if (parent != null) {
            LOGGER.debug("Left: " + category.getLeft() + " Right: " + category.getRight());
            // always add +1 for the currentNode
            int nodes = 1 + (category.getRight() - category.getLeft()) / 2;
            final int increment = nodes * -2;
            // decrement left and right values by nodes
            updateLeftRightValue(connection, category.getRootID(), category.getLeft(), increment);
        }
        updateTimeStamp();
        updateLastModified(category.getRootID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRCategoryDAO#exist(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public boolean exist(MCRCategoryID id) {
        Criteria criteria = getHibConnection().getSession().createCriteria(CATEGRORY_CLASS);
        criteria.setProjection(Projections.rowCount()).add(getCategoryCriterion(id));
        criteria.setCacheable(true);
        Number result = (Number) criteria.uniqueResult();
        return result != null && result.intValue() > 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MCRCategory> getCategoriesByLabel(final String lang, final String text) {
        final Query q = getHibConnection().getNamedQuery(NAMED_QUERY_NAMESPACE + "byLabel");
        q.setString("lang", lang);
        q.setString("text", text);
        q.setCacheable(true);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public List<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        MCRCategoryDTO leftRight = getLeftRightLevelValues(baseID);
        Query q = getHibConnection().getNamedQuery(NAMED_QUERY_NAMESPACE + "byLabelInClass");
        q.setString("rootID", baseID.getRootID());
        q.setInteger("left", leftRight.leftValue);
        q.setInteger("right", leftRight.rightValue);
        q.setString("lang", lang);
        q.setString("text", text);
        q.setCacheable(true);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        final boolean fetchAllChildren = childLevel < 0;
        javax.persistence.Query q;
        if (id.isRootID()) {
            q = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE
                + (fetchAllChildren ? "prefetchClassQuery" : "prefetchClassLevelQuery"));
            if (!fetchAllChildren) {
                q.setParameter("endlevel", childLevel);
            }
            q.setParameter("classID", id.getRootID());
        } else {
            //normal category
            MCRCategoryDTO leftRightLevel = getLeftRightLevelValues(id);
            if (leftRightLevel == null) {
                return null;
            }
            q = entityManager.createNamedQuery(NAMED_QUERY_NAMESPACE
                + (fetchAllChildren ? "prefetchCategQuery" : "prefetchCategLevelQuery"));
            if (!fetchAllChildren) {
                q.setParameter("endlevel", leftRightLevel.level + childLevel);
            }
            q.setParameter("classID", id.getRootID());
            q.setParameter("left", leftRightLevel.leftValue);
            q.setParameter("right", leftRightLevel.rightValue);
        }
        List<MCRCategoryDTO> result = q.getResultList();
        if (result.isEmpty()) {
            LOGGER.warn("Could not load category: " + id);
            return null;
        }
        MCRCategoryImpl categoryImpl = buildCategoryFromPrefetchedList(result, id);
        return categoryImpl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public List<MCRCategory> getChildren(MCRCategoryID cid) {
        LOGGER.debug("Get children of category: " + cid);
        return Optional.ofNullable(cid)
            .map(id -> getCategory(id, 1))
            .map(MCRCategory::getChildren)
            .map(l -> l
                .parallelStream()
                .collect(Collectors.toList()) //temporary copy for detachFromParent
                .parallelStream()
                .map(MCRCategoryImpl.class::cast)
                .peek(MCRCategoryImpl::detachFromParent)
                .map(MCRCategory.class::cast)
                .collect(Collectors.toList()))
            .orElse(new MCRCategoryChildList(null, null));
    }

    public List<MCRCategory> getParents(MCRCategoryID id) {
        MCRCategoryDTO leftRight = getLeftRightLevelValues(id);
        if (leftRight == null) {
            return null;
        }
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        javax.persistence.Query parentQuery = entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "parentQuery")
            .setParameter("classID", id.getRootID())
            .setParameter("categID", id.getID())
            .setParameter("left", leftRight.leftValue)
            .setParameter("right", leftRight.rightValue);
        @SuppressWarnings("unchecked")
        List<MCRCategoryDTO> resultList = parentQuery.getResultList();
        MCRCategory category = buildCategoryFromPrefetchedList(resultList, id);
        List<MCRCategory> parents = new ArrayList<MCRCategory>();
        while (category.getParent() != null) {
            category = category.getParent();
            parents.add(category);
        }
        return parents;
    }

    @SuppressWarnings("unchecked")
    public List<MCRCategoryID> getRootCategoryIDs() {
        Session session = getHibConnection().getSession();
        FlushMode fm = session.getFlushMode();
        session.setFlushMode(FlushMode.MANUAL);
        try {
            return (List<MCRCategoryID>) session.createCriteria(CATEGRORY_CLASS)
                .add(Restrictions.eq("left", LEFT_START_VALUE))
                .setProjection(Projections.property("id"))
                .setCacheable(true)
                .list()
                .stream()
                .map(MCRCategoryID.class::cast)
                .collect(Collectors.toList());
        } finally {
            session.setFlushMode(fm);
        }
    }

    @SuppressWarnings("unchecked")
    public List<MCRCategory> getRootCategories() {
        Session session = getHibConnection().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS);
        c.add(Restrictions.eq("left", LEFT_START_VALUE));
        c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        c.setCacheable(true);
        List<MCRCategoryImpl> result = c.list();
        List<MCRCategory> classes = new ArrayList<MCRCategory>(result.size());
        for (MCRCategoryImpl cat : result) {
            classes.add(copyDeep(cat, 0));
        }
        return classes;
    }

    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        if (baseID.isRootID()) {
            return getCategory(baseID, childLevel);
        }
        Session session = getHibConnection().getSession();
        FlushMode fm = session.getFlushMode();
        session.setFlushMode(FlushMode.MANUAL);
        try {
            List<MCRCategory> parents = getParents(baseID);
            if (parents == null || parents.size() == 0) {
                return null;
            }

            List<MCRCategoryImpl> parentsCopy = new ArrayList<MCRCategoryImpl>(parents.size());
            for (int i = parents.size() - 1; i >= 0; i--) {
                parentsCopy.add(copyDeep(parents.get(i), 0));
            }

            if (parentsCopy.size() == 0) {
                return null;
            }

            MCRCategoryImpl root = parentsCopy.get(0);
            for (int i = 1; i < parentsCopy.size(); i++) {
                parentsCopy.get(i).setRoot(root);
                parentsCopy.get(i).setParent(parentsCopy.get(i - 1));
            }
            MCRCategoryImpl node = getByNaturalID(session, baseID);
            // prepare a temporary copy for the deepCopy process
            MCRCategoryImpl tempCopy = new MCRCategoryImpl();
            tempCopy.setInternalID(node.getInternalID());
            tempCopy.setId(node.getId());
            tempCopy.setLabels(node.getLabels());
            tempCopy.setLevel(node.getLevel());
            tempCopy.children = node.children;
            tempCopy.root = root;
            // attach deep node copy to its parent
            copyDeep(tempCopy, childLevel).setParent(parentsCopy.get(parentsCopy.size() - 1));
            // return root node
            return root;
        } finally {
            session.setFlushMode(fm);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#hasChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public boolean hasChildren(MCRCategoryID cid) {
        // SELECT * FROM MCRCATEGORY WHERE PARENTID=(SELECT INTERNALID FROM
        // MCRCATEGORY WHERE rootID=cid.getRootID() and ID...);
        return getNumberOfChildren(cid) > 0;
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
        int index = getNumberOfChildren(newParentID);
        moveCategory(id, newParentID, index);
    }

    private MCRCategoryImpl getCommonAncestor(MCRHIBConnection connection, MCRCategoryImpl node1,
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
        Query q = connection.getNamedQuery(NAMED_QUERY_NAMESPACE + "commonAncestor");
        q.setMaxResults(1);
        int left = Math.min(node1.getLeft(), node2.getLeft());
        int right = Math.max(node1.getRight(), node2.getRight());
        q.setInteger("left", left);
        q.setInteger("right", right);
        q.setString("rootID", node1.getRootID());
        return (MCRCategoryImpl) q.uniqueResult();
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        final MCRHIBConnection connection = getHibConnection();
        Session session = connection.getSession();
        MCRCategoryImpl subTree = getByNaturalID(session, id);
        MCRCategoryImpl oldParent = (MCRCategoryImpl) subTree.getParent();
        MCRCategoryImpl newParent = getByNaturalID(session, newParentID);
        MCRCategoryImpl commonAncestor = getCommonAncestor(connection, oldParent, newParent);
        subTree.detachFromParent();
        LOGGER.debug("Add subtree to new Parent at index: " + index);
        newParent.getChildren().add(index, subTree);
        subTree.parent = newParent;
        commonAncestor.calculateLeftRightAndLevel(commonAncestor.getLeft(), commonAncestor.getLevel());
        updateTimeStamp();
        updateLastModified(id.getRootID());
    }

    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        category.getLabel(lang).ifPresent(oldLabel -> {
            category.getLabels().remove(oldLabel);
            updateTimeStamp();
            updateLastModified(category.getRootID());
        });
        return category;
    }

    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        if (!exist(newCategory.getId())) {
            throw new IllegalArgumentException(
                "MCRCategory can not be replaced. MCRCategoryID '" + newCategory.getId() + "' is unknown.");
        }
        final MCRHIBConnection connection = getHibConnection();
        Session session = connection.getSession();
        MCRCategoryImpl oldCategory = getByNaturalID(session, newCategory.getId());
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
            .forEach(c -> LOGGER.info("remove category: " + c.getId()));
        oldMap.clear();
        oldMap.putAll(toMap(oldCategory));
        //sync labels/uris;
        MCRStreamUtils
            .flatten(oldCategory, MCRCategory::getChildren, false)
            .filter(c -> newMap.containsKey(c.getId()))
            .map(MCRCategoryImpl.class::cast)
            .map(c -> new AbstractMap.SimpleEntry<>(c, newMap.get(c.getId()))) // key: category of old version, value: category of new version
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
            .flatten(newCategoryImpl, MCRCategory::getChildren, false)
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
        oldCategory.calculateLeftRightAndLevel(oldLeft, oldLevel);
        session.update(oldCategory);
        updateTimeStamp();
        updateLastModified(newCategory.getId().getRootID());
        return newMap.values();
    }

    private static Map<MCRCategoryID, MCRCategoryImpl> toMap(MCRCategoryImpl oldCategory) {
        return MCRStreamUtils
            .flatten(oldCategory, MCRCategory::getChildren, false)
            .collect(Collectors.toMap(MCRCategory::getId, MCRCategoryImpl.class::cast));
    }

    private static void remove(MCRCategoryImpl category) {
        if (category.hasChildren()) {
            int parentPos = category.getPositionInParent();
            MCRCategoryImpl parent = (MCRCategoryImpl) category.getParent();
            parent.children.addAll(parentPos, category.children
                .stream()
                .map(MCRCategoryImpl.class::cast)
                .collect(Collectors.toList()) //temporary list so we do not modify 'children' directly
                .stream()
                .peek(MCRCategoryImpl::detachFromParent)
                .collect(Collectors.toList()));
        }
        category.detachFromParent();
    }

    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        category.getLabel(label.getLang()).ifPresent(category.getLabels()::remove);
        category.getLabels().add(label);
        session.update(category);
        updateTimeStamp();
        updateLastModified(category.getRootID());
        return category;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, Set<MCRLabel> labels) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        category.setLabels(labels);
        session.update(category);
        updateTimeStamp();
        updateLastModified(category.getRootID());
        return category;
    }

    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        category.setURI(uri);
        session.update(category);
        updateTimeStamp();
        updateLastModified(category.getRootID());
        return category;
    }

    public void repairLeftRightValue(String classID) {
        final Session session = MCRHIBConnection.instance().getSession();
        final MCRCategoryID rootID = MCRCategoryID.rootID(classID);
        MCRCategoryImpl classification = MCRCategoryDAOImpl.getByNaturalID(session, rootID);
        classification.calculateLeftRightAndLevel(0, 0);
    }

    public long getLastModified() {
        return LAST_MODIFIED;
    }

    private static void updateTimeStamp() {
        LAST_MODIFIED = System.currentTimeMillis();
    }

    static Criterion getCategoryCriterion(MCRCategoryID id) {
        return Restrictions.eq("id", id);
    }

    private static MCRCategoryImpl buildCategoryFromPrefetchedList(List<MCRCategoryDTO> list, MCRCategoryID returnID) {
        LOGGER.debug(() -> "using prefetched list: " + list.toString());
        MCRCategoryImpl predecessor = null;
        for (MCRCategoryDTO entry : list) {
            predecessor = entry.merge(predecessor);
        }
        return MCRStreamUtils.flatten(predecessor.getRoot(), MCRCategory::getChildren, true)
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
            LOGGER.error("Cannot get children size for category: " + category.getId(), e);
            throw e;
        }
        newCateg.setChildren(new ArrayList<MCRCategory>(childAmount));
        newCateg.setId(category.getId());
        newCateg.setLabels(category.getLabels());
        newCateg.setRoot(category.getRoot());
        newCateg.setURI(category.getURI());
        newCateg.setLevel(category.getLevel());
        if (category instanceof MCRCategoryImpl) {
            //to allow optimized hasChildren() to work without db query
            newCateg.setLeft(((MCRCategoryImpl) category).getLeft());
            newCateg.setRight(((MCRCategoryImpl) category).getRight());
            newCateg.setInternalID(((MCRCategoryImpl) category).getInternalID());
        }
        if (childAmount > 0) {
            for (MCRCategory child : category.getChildren()) {
                newCateg.getChildren().add(copyDeep(child, level - 1));
            }
        }
        return newCateg;
    }

    /**
     * returns database backed MCRCategoryImpl
     * 
     * every change to the returned MCRCategory is reflected in the database.
     */
    public static MCRCategoryImpl getByNaturalID(Session session, MCRCategoryID id) {
        return (MCRCategoryImpl) session.createCriteria(MCRCategoryImpl.class).add(getCategoryCriterion(id))
            .setCacheable(true).uniqueResult();
    }

    private static void syncLabels(MCRCategoryImpl source, MCRCategoryImpl target) {
        for (MCRLabel newLabel : source.getLabels()) {
            Optional<MCRLabel> label = target.getLabel(newLabel.getLang());
            if (!label.isPresent()) {
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
        Iterator<MCRLabel> labels = target.getLabels().iterator();
        while (labels.hasNext()) {
            // remove labels that are not present in new version
            if (!source.getLabel(labels.next().getLang()).isPresent()) {
                labels.remove();
            }
        }
    }

    private static MCRCategoryDTO getLeftRightLevelValues(MCRCategoryID id) {
        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        javax.persistence.Query leftRightQuery = entityManager
            .createNamedQuery(NAMED_QUERY_NAMESPACE + "leftRightLevelQuery");
        leftRightQuery.setParameter("categID", id);
        try {
            return (MCRCategoryDTO) leftRightQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static int getNumberOfChildren(MCRCategoryID id) {
        Session session = getHibConnection().getSession();
        FlushMode fm = session.getFlushMode();
        session.setFlushMode(FlushMode.MANUAL);
        try {
            Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.rowCount());
            c.add(Subqueries.propertyEq("parent", DetachedCriteria.forClass(CATEGRORY_CLASS)
                .setProjection(Projections.property("internalID")).add(getCategoryCriterion(id))));
            return ((Number) c.uniqueResult()).intValue();
        } finally {
            session.setFlushMode(fm);
        }
    }

    private static void updateLeftRightValue(MCRHIBConnection connection, String classID, int left,
        final int increment) {
        LOGGER.debug("LEFT AND RIGHT values need updates. Left=" + left + ", increment by: " + increment);
        Query leftQuery = getHibConnection().getNamedQuery(NAMED_QUERY_NAMESPACE + "updateLeft");
        leftQuery.setInteger("left", left);
        leftQuery.setInteger("increment", increment);
        leftQuery.setString("classID", classID);
        int leftChanges = leftQuery.executeUpdate();
        Query rightQuery = getHibConnection().getNamedQuery(NAMED_QUERY_NAMESPACE + "updateRight");
        rightQuery.setInteger("left", left);
        rightQuery.setInteger("increment", increment);
        rightQuery.setString("classID", classID);
        int rightChanges = rightQuery.executeUpdate();
        LOGGER.debug("Updated " + leftChanges + " left and " + rightChanges + " right values.");
    }

    private static MCRHIBConnection getHibConnection() {
        if (HIB_CONNECTION_INSTANCE == null) {
            HIB_CONNECTION_INSTANCE = MCRHIBConnection.instance();
        }
        return HIB_CONNECTION_INSTANCE;
    }

    /**
     * Method updates the last modified timestamp, for the given root id.
     * 
     */
    synchronized protected void updateLastModified(String root) {
        LAST_MODIFIED_MAP.put(root, System.currentTimeMillis());
    }

    /**
     * Gets the timestamp for the given root id. If there is not timestamp at the moment -1 is returned.
     * 
     * @return the last modified timestamp (if any) or -1
     */
    public long getLastModified(String root) {
        Long long1 = LAST_MODIFIED_MAP.get(root);
        if (long1 != null) {
            return long1;
        }
        return -1;
    }
}
