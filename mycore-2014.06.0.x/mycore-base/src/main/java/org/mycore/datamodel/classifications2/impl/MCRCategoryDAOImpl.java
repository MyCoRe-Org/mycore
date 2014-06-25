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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
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

    private static final Logger LOGGER = Logger.getLogger(MCRCategoryDAOImpl.class);

    private static final Class<MCRCategoryImpl> CATEGRORY_CLASS = MCRCategoryImpl.class;

    private static HashMap<String, Long> LAST_MODIFIED_MAP = new HashMap<String, Long>();

    public void addCategory(MCRCategoryID parentID, MCRCategory category) {
        int position = -1;
        if (category instanceof MCRCategoryImpl) {
            position = ((MCRCategoryImpl) category).getPositionInParent();
        }
        addCategory(parentID, category, position);
    }

    public void addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        if (exist(category.getId())) {
            throw new MCRException("Cannot add category. A category with ID " + category.getId() + " allready exists");
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
                throw new IndexOutOfBoundsException("Cannot add category as child #" + position
                    + ", when there are only " + parent.getChildren().size() + " children.");
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
        final Query q = getHibConnection().getNamedQuery(CATEGRORY_CLASS.getName() + ".byLabel");
        q.setString("lang", lang);
        q.setString("text", text);
        q.setCacheable(true);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public List<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        Integer[] leftRight = getLeftRightValues(baseID);
        Query q = getHibConnection().getNamedQuery(CATEGRORY_CLASS.getName() + ".byLabelInClass");
        q.setString("rootID", baseID.getRootID());
        q.setInteger("left", leftRight[0]);
        q.setInteger("right", leftRight[1]);
        q.setString("lang", lang);
        q.setString("text", text);
        q.setCacheable(true);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        Session session = getHibConnection().getSession();
        final boolean fetchAllChildren = childLevel < 0;
        Query q;
        if (id.isRootID()) {
            q = getHibConnection().getNamedQuery(
                CATEGRORY_CLASS.getName() + (fetchAllChildren ? ".prefetchClassQuery" : ".prefetchClassLevelQuery"));
            if (!fetchAllChildren) {
                q.setInteger("endlevel", childLevel);
            }
            q.setString("classID", id.getRootID());
        } else {
            //normal category
            MCRCategoryImpl category = getByNaturalID(session, id);
            if (category == null) {
                return null;
            }
            q = getHibConnection().getNamedQuery(
                CATEGRORY_CLASS.getName() + (fetchAllChildren ? ".prefetchCategQuery" : ".prefetchCategLevelQuery"));
            if (!fetchAllChildren) {
                q.setInteger("endlevel", category.getLevel() + childLevel);
            }
            q.setString("classID", id.getRootID());
            q.setInteger("left", category.getLeft());
            q.setInteger("right", category.getRight());
        }
        List<MCRCategoryImpl> result = q.list();
        if (result.isEmpty()) {
            LOGGER.warn("Could not load category: " + id);
            return null;
        }
        MCRCategoryImpl categoryImpl = buildCategoryFromPrefetchedList(result);
        if (!fetchAllChildren) {
            categoryImpl = copyDeep(categoryImpl, childLevel);
        }
        return categoryImpl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @SuppressWarnings("unchecked")
    public List<MCRCategory> getChildren(MCRCategoryID cid) {
        LOGGER.debug("Get children of category: " + cid);
        Session session = getHibConnection().getSession();
        FlushMode fm = session.getFlushMode();
        session.setFlushMode(FlushMode.MANUAL);
        try {
            if (!exist(cid)) {
                return new MCRCategoryChildList(null, null);
            }
            Criteria c = session.createCriteria(CATEGRORY_CLASS).add(
                Subqueries.propertyEq(
                    "parent",
                    DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID"))
                        .add(getCategoryCriterion(cid))));
            c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
            c.setCacheable(true);
            return c.list();
        } finally {
            session.setFlushMode(fm);
        }
    }

    public List<MCRCategory> getParents(MCRCategoryID id) {
        // TODO: Make use of left and right value here
        Session session = getHibConnection().getSession();
        List<MCRCategory> parents = new ArrayList<MCRCategory>();
        MCRCategory category = getByNaturalID(session, id);
        if (category == null) {
            return null;
        }
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
            Criteria c = session.createCriteria(CATEGRORY_CLASS);
            c.add(Restrictions.eq("left", LEFT_START_VALUE));
            c.setProjection(Projections.projectionList().add(Projections.property("rootID"))
                .add(Projections.property("categID")));
            c.setCacheable(true);
            List<Object[]> result = c.list();
            List<MCRCategoryID> classIds = new ArrayList<MCRCategoryID>(result.size());
            for (Object[] cat : result) {
                classIds.add(new MCRCategoryID(cat[0].toString(), cat[1].toString()));
            }
            return classIds;
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
            List<MCRCategoryImpl> parentsCopy = new ArrayList<MCRCategoryImpl>(parents.size());
            for (int i = parents.size() - 1; i >= 0; i--) {
                parentsCopy.add(copyDeep(parents.get(i), 0));
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

    private MCRCategoryImpl getCommonAncestor(MCRHIBConnection connection, MCRCategoryImpl node1, MCRCategoryImpl node2) {
        if (!node1.getRootID().equals(node2.getRootID())) {
            return null;
        }
        if (node1.getLeft() == 0) {
            return node1;
        }
        if (node2.getLeft() == 0) {
            return node2;
        }
        Query q = connection.getNamedQuery(CATEGRORY_CLASS.getName() + ".commonAncestor");
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

    public void removeLabel(MCRCategoryID id, String lang) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        MCRLabel oldLabel = category.getLabel(lang);
        if (oldLabel != null) {
            category.getLabels().remove(oldLabel);
            updateTimeStamp();
            updateLastModified(category.getRootID());
        }
    }

    public void replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        if (!exist(newCategory.getId())) {
            throw new IllegalArgumentException("MCRCategory can not be replaced. MCRCategoryID '" + newCategory.getId()
                + "' is unknown.");
        }
        final MCRHIBConnection connection = getHibConnection();
        Session session = connection.getSession();
        MCRCategoryImpl oldCategory = getByNaturalID(session, newCategory.getId());
        // old Map with all Categories referenced by ID
        Map<MCRCategoryID, MCRCategoryImpl> oldMap = new HashMap<MCRCategoryID, MCRCategoryImpl>();
        fillIDMap(oldMap, oldCategory);
        final MCRCategoryImpl copyDeepImpl = copyDeep(newCategory, -1);
        MCRCategoryImpl newCategoryImpl = MCRCategoryImpl.wrapCategory(copyDeepImpl, oldCategory.getParent(),
            oldCategory.getRoot());
        // new Map with all Categories referenced by ID
        Map<MCRCategoryID, MCRCategoryID> oldParents = new HashMap<MCRCategoryID, MCRCategoryID>();
        for (MCRCategory category : oldMap.values()) {
            if (category.isCategory()) {
                oldParents.put(category.getId(), category.getParent().getId());
            }
        }
        Map<MCRCategoryID, MCRCategoryImpl> newMap = new HashMap<MCRCategoryID, MCRCategoryImpl>();
        fillIDMap(newMap, newCategoryImpl);
        /*
         * copy elements from the new tree to the old tree. fixes bug #1848710
         * (Classification save fails after shifting categories)
         * 
         * set internalID for the new rootCategory
         */
        session.evict(oldCategory);
        for (MCRCategoryImpl category : newMap.values()) {
            MCRCategoryImpl oldValue = oldMap.get(category.getId());
            if (oldValue != null) {
                copyCategoryToNewTree(newCategoryImpl, category, oldValue);
            }
        }
        // calculate left, right and level values
        int diffNodes = newMap.size() - oldMap.size();
        LOGGER.debug("Update changes classification node size by: " + diffNodes);
        int increment = diffNodes * 2;
        if (increment != 0 && oldCategory.isCategory()) {
            final int left = oldCategory.getRightSiblingOrOfAncestor().getLeft();
            final int maxLeftRight = ((MCRCategoryImpl) oldCategory.getRoot()).getRight();
            final int right = oldCategory.getRightSiblingOrParent().getRight();
            updateLeftRightValueMax(connection, newCategory.getId().getRootID(), left, maxLeftRight, right,
                maxLeftRight, increment);
        }
        newCategoryImpl.calculateLeftRightAndLevel(oldCategory.getLevel(), oldCategory.getLeft());
        if (oldCategory.isCategory()) {
            MCRCategoryImpl parent = (MCRCategoryImpl) oldCategory.getParent();
            parent.getChildren().set(oldCategory.getPositionInParent(), newCategoryImpl);
        }
        // delete removed categories
        boolean flushBeforeUpdate = false;
        for (MCRCategoryImpl category : oldMap.values()) {
            if (!newMap.containsKey(category.getId())) {
                LOGGER.info("Deleting category :" + category.getId());
                category.detachFromParent();
                session.delete(category);
                flushBeforeUpdate = true;
            }
        }
        // update shifted categories (JUnit testCase testReplaceCategoryShiftCase)
        fillIDMap(newMap, newCategoryImpl);
        for (MCRCategoryImpl category : newMap.values()) {
            if (category.isCategory() && oldParents.containsKey(category.getId())) {
                if (!oldParents.get(category.getId()).equals(category.getParent().getId())) {
                    LOGGER.info("updating new parent for " + category.getId());
                    session.update(category);
                    flushBeforeUpdate = true;
                }
            }
        }
        // important to flush here as positionInParent could collide with deleted categories
        if (flushBeforeUpdate) {
            //update add new categories before flush
            for (MCRCategoryImpl category : newMap.values()) {
                if (!oldMap.containsKey(category.getId())) {
                    LOGGER.info("adding new parent category :" + category.getId());
                    session.save(category);
                }
            }
            session.flush();
        }
        session.saveOrUpdate(newCategoryImpl);
        updateTimeStamp();
        updateLastModified(newCategory.getRoot().getId().toString());
    }

    public void setLabel(MCRCategoryID id, MCRLabel label) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        MCRLabel oldLabel = category.getLabel(label.getLang());
        if (oldLabel != null) {
            category.getLabels().remove(oldLabel);
        }
        category.getLabels().add(label);
        session.update(category);
        updateTimeStamp();
        updateLastModified(category.getRootID());
    }

    @Override
    public void setLabels(MCRCategoryID id, Set<MCRLabel> labels) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        category.setLabels(labels);
        session.update(category);
        updateTimeStamp();
        updateLastModified(category.getRootID());
    }

    public void setURI(MCRCategoryID id, URI uri) {
        Session session = getHibConnection().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        category.setURI(uri);
        session.update(category);
        updateTimeStamp();
        updateLastModified(category.getRootID());
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

    private static Criterion getCategoryCriterion(MCRCategoryID id) {
        return Restrictions.naturalId().set("rootID", id.getRootID()).set("categID", id.getID());
    }

    private static MCRCategoryImpl buildCategoryFromPrefetchedList(List<MCRCategoryImpl> list) {
        MCRCategoryImpl baseCat = list.iterator().next();
        int size = list.size();
        for (int i = size - 1; i >= 0; i--) {
            MCRCategoryImpl currentCat = list.get(i);
            currentCat.getChildren();
        }
        return baseCat;
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
     * every change to the returned MCRCategory is refelected in the database.
     * 
     * @param session
     * @param id
     */
    public static MCRCategoryImpl getByNaturalID(Session session, MCRCategoryID id) {
        return (MCRCategoryImpl) session.byNaturalId(CATEGRORY_CLASS).setSynchronizationEnabled(false)
            .using("rootID", id.getRootID()).using("categID", id.getID()).load();
    }

    /**
     * copies <code>previousVersion</code> into the place of
     * <code>category</code> in <code>newTree</code>. All changes of
     * <code>category</code> are applied to <code>previousVersion</code>.
     * 
     * @param newTree
     *            new root of MCRCategory tree
     * @param category
     *            new version containing all changes
     * @param previousVersion
     *            oldVersion allready stored in database
     */
    private static void copyCategoryToNewTree(MCRCategoryImpl newTree, MCRCategoryImpl category,
        MCRCategoryImpl previousVersion) {
        category.setInternalID(previousVersion.getInternalID());
        MCRCategoryImpl parent = (MCRCategoryImpl) category.getParent();
        if (parent == null || category.getId().equals(newTree.getId())) {
            category.setInternalID(previousVersion.getInternalID());
        } else {
            // get new parent of modified category tree here (by parent category id)
            parent = (MCRCategoryImpl) findCategory(newTree, category.getParent().getId());
            int pos = category.getPositionInParent();
            parent.getChildren().remove(pos);
            previousVersion.detachFromParent();
            parent.getChildren().add(pos, previousVersion);
            // set parent here
            previousVersion.parent = parent;
            previousVersion.getChildren().clear();
            previousVersion.getChildren().addAll(
                MCRCategoryImpl.wrapCategories(detachCategories(category.getChildren()), previousVersion,
                    previousVersion.getRoot()));
            for (MCRLabel newLabel : category.getLabels()) {
                MCRLabel oldLabel = previousVersion.getLabel(newLabel.getLang());
                if (oldLabel == null) {
                    // copy new label
                    previousVersion.getLabels().add(newLabel);
                } else {
                    if (!oldLabel.getText().equals(newLabel.getText())) {
                        oldLabel.setText(newLabel.getText());
                    }
                    if (!oldLabel.getDescription().equals(newLabel.getDescription())) {
                        oldLabel.setDescription(newLabel.getDescription());
                    }
                }
            }
            Iterator<MCRLabel> labels = previousVersion.getLabels().iterator();
            while (labels.hasNext()) {
                // remove labels that are not present in new version
                if (category.getLabel(labels.next().getLang()) == null) {
                    labels.remove();
                }
            }
            previousVersion.setURI(category.getURI());
        }
    }

    /**
     * finds a MCRCategory with <code>id</code> somewhere below
     * <code>root</code>.
     */
    private static MCRCategory findCategory(MCRCategory root, MCRCategoryID id) {
        if (root.getId().equals(id)) {
            return root;
        }
        for (MCRCategory child : root.getChildren()) {
            MCRCategory rv = findCategory(child, id);
            if (rv != null) {
                return rv;
            }
        }
        return null;
    }

    /**
     * detaches all elements in list from their parent.
     */
    private static List<MCRCategory> detachCategories(List<MCRCategory> list) {
        ArrayList<MCRCategory> categories = new ArrayList<MCRCategory>(list.size());
        categories.addAll(list);
        for (MCRCategory category : categories) {
            if (!(category instanceof MCRCategoryImpl)) {
                throw new IllegalArgumentException("This method just works for MCRCategoryImpl");
            }
            ((MCRAbstractCategoryImpl) category).detachFromParent();
        }
        return categories;
    }

    private static void fillIDMap(Map<MCRCategoryID, MCRCategoryImpl> map, MCRCategoryImpl category) {
        map.put(category.getId(), category);
        for (MCRCategory subCategory : category.getChildren()) {
            fillIDMap(map, (MCRCategoryImpl) subCategory);
        }
    }

    private static Integer[] getLeftRightValues(MCRCategoryID id) {
        Session session = getHibConnection().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(
            Projections.projectionList().add(Projections.property("left")).add(Projections.property("right")));
        c.add(getCategoryCriterion(id));
        Object[] result = (Object[]) c.uniqueResult();
        Integer[] iResult = new Integer[] { (Integer) result[0], (Integer) result[1] };
        return iResult;
    }

    private static int getNumberOfChildren(MCRCategoryID id) {
        Session session = getHibConnection().getSession();
        FlushMode fm = session.getFlushMode();
        session.setFlushMode(FlushMode.MANUAL);
        try {
            Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.rowCount());
            c.add(Subqueries.propertyEq(
                "parent",
                DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID"))
                    .add(getCategoryCriterion(id))));
            return ((Number) c.uniqueResult()).intValue();
        } finally {
            session.setFlushMode(fm);
        }
    }

    private static void updateLeftRightValue(MCRHIBConnection connection, String classID, int left, final int increment) {
        LOGGER.debug("LEFT AND RIGHT values need updates. Left=" + left + ", increment by: " + increment);
        Query leftQuery = getHibConnection().getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeft");
        leftQuery.setInteger("left", left);
        leftQuery.setInteger("increment", increment);
        leftQuery.setString("classID", classID);
        int leftChanges = leftQuery.executeUpdate();
        Query rightQuery = getHibConnection().getNamedQuery(CATEGRORY_CLASS.getName() + ".updateRight");
        rightQuery.setInteger("left", left);
        rightQuery.setInteger("increment", increment);
        rightQuery.setString("classID", classID);
        int rightChanges = rightQuery.executeUpdate();
        LOGGER.debug("Updated " + leftChanges + " left and " + rightChanges + " right values.");
    }

    private static void updateLeftRightValueMax(MCRHIBConnection connection, String classID, int left, int maxLeft,
        int right, int maxRight, final int increment) {
        LOGGER.debug("LEFT AND RIGHT values need updates. Left=" + left + ", MaxLeft=" + maxLeft + ", Right=" + right
            + ", MaxRight=" + maxRight + " increment by: " + increment);
        Query leftQuery = getHibConnection().getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeftWithMax");
        leftQuery.setInteger("left", left);
        leftQuery.setInteger("max", maxLeft);
        leftQuery.setInteger("increment", increment);
        leftQuery.setString("classID", classID);
        int leftChanges = leftQuery.executeUpdate();
        Query rightQuery = getHibConnection().getNamedQuery(CATEGRORY_CLASS.getName() + ".updateRightWithMax");
        rightQuery.setInteger("right", right);
        rightQuery.setInteger("max", maxRight);
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
    synchronized private void updateLastModified(String root) {
        LAST_MODIFIED_MAP.put(root, System.currentTimeMillis());
    }

    private int getNodesCount(MCRCategory category) {
        int nodes = 1;
        for (MCRCategory child : category.getChildren()) {
            nodes += getNodesCount(child);
        }
        return nodes;
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
