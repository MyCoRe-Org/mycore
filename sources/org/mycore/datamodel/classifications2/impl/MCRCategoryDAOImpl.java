/**
 * $RCSfile$
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
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
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategoryDAOImpl implements MCRCategoryDAO {

    private static final Logger LOGGER = Logger.getLogger(MCRCategoryDAOImpl.class);

    private static final Class<MCRCategoryImpl> CATEGRORY_CLASS = MCRCategoryImpl.class;

    public void addCategory(MCRCategoryID parentID, MCRCategory category) {
        if (exist(category.getId())) {
            throw new MCRException("Cannot add category. A category with ID " + category.getId() + " allready exists");
        }
        int leftStart = 0;
        int levelStart = 0;
        final MCRHIBConnection connection = MCRHIBConnection.instance();
        Session session = connection.getSession();
        MCRCategoryImpl parent = null;
        if (parentID != null) {
            parent = getByNaturalID(session, parentID);
            levelStart = parent.getLevel() + 1;
            leftStart = parent.getLeft() + 1;
        }
        LOGGER.debug("Calculating LEFT,RIGHT and LEVEL attributes...");
        final MCRCategoryImpl wrapCategory = MCRCategoryImpl.wrapCategory(category, parent, (parent == null) ? category.getRoot() : parent.getRoot());
        calculateLeftRightAndLevel(wrapCategory, leftStart, levelStart);
        // always add +1 for the current node
        int nodes = 1 + (wrapCategory.getRight() - wrapCategory.getLeft()) / 2;
        LOGGER.debug("Calculating LEFT,RIGHT and LEVEL attributes. Done! Nodes: " + nodes);
        if (parentID != null) {
            final int increment = nodes * 2;
            updateLeftRightValue(connection, leftStart, increment);
            parent.getChildren().add(category);
        }
        session.save(category);
        LOGGER.info("Categorie saved.");
    }

    public void deleteCategory(MCRCategoryID id) {
        final MCRHIBConnection connection = MCRHIBConnection.instance();
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
            updateLeftRightValue(connection, category.getLeft(), increment);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRCategoryDAO#exist(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public boolean exist(MCRCategoryID id) {
        Criteria criteria = MCRHIBConnection.instance().getSession().createCriteria(CATEGRORY_CLASS);
        criteria.setProjection(Projections.rowCount()).add(MCRCategoryExpression.eq(id));
        Number result = (Number) criteria.uniqueResult();
        if (result == null) {
            return false;
        }
        return result.intValue() > 0;
    }

    @SuppressWarnings("unchecked")
    public List<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        Session session = MCRHIBConnection.instance().getSession();
        Integer[] leftRight = getLeftRightValues(baseID);
        Query q = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".byLabel");
        q.setString("rootID", baseID.getRootID());
        q.setInteger("left", leftRight[0]);
        q.setInteger("right", leftRight[1]);
        q.setString("lang", lang);
        q.setString("text", text);
        return (List<MCRCategory>) q.list();
    }

    @SuppressWarnings("unchecked")
    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        return copyDeep(category, childLevel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @SuppressWarnings("unchecked")
    public List<MCRCategory> getChildren(MCRCategoryID cid) {
        LOGGER.debug("Get children of category: " + cid);
        if (!exist(cid)) {
            return new MCRCategoryImpl.ChildList(null, null);
        }
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).add(
                Subqueries.propertyEq("parent", DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID")).add(
                        MCRCategoryExpression.eq(cid))));
        return (List<MCRCategory>) c.list();
    }

    public List<MCRCategory> getParents(MCRCategoryID id) {
        // TODO: Make use of left and right value here
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRCategory> parents = new ArrayList<MCRCategory>();
        MCRCategory category = getByNaturalID(session, id);
        while (category.getParent() != null) {
            category = category.getParent();
            parents.add(category);
        }
        return parents;
    }

    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        if (baseID.getID() == null) {
            return getCategory(baseID, childLevel);
        }
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRCategory> parents = getParents(baseID);
        List<MCRCategoryImpl> parentsCopy = new ArrayList<MCRCategoryImpl>(parents.size());
        for (int i = parents.size() - 1; i >= 0; i--) {
            parentsCopy.add(copyDeep((MCRCategoryImpl) parents.get(i), 0));
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

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        final MCRHIBConnection connection = MCRHIBConnection.instance();
        Session session = connection.getSession();
        MCRCategoryImpl subTree = getByNaturalID(session, id);
        int left = subTree.getLeft();
        int right = subTree.getRight();
        int oldIndex = subTree.getPositionInParent();
        MCRCategoryImpl oldParent = (MCRCategoryImpl) subTree.getParent();
        MCRCategoryImpl newParent = getByNaturalID(session, newParentID);
        subTree.detachFromParent();
        LOGGER.debug("Add subtree to new Parent at index: " + index);
        newParent.getChildren().add(index, subTree);
        subTree.parent = newParent;
        // update needed for old and newParent;
        // Update Left, Right values of other categories
        boolean movedToRight = isCategoryMovedRight(oldParent, newParent, index, oldIndex);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("OldParent left: " + oldParent.getLeft() + " Right: " + oldParent.getRight() + " Level: " + oldParent.getLevel());
            LOGGER.debug("NewParent left: " + newParent.getLeft() + " Right: " + newParent.getRight() + " Level: " + newParent.getLevel());
            LOGGER.debug("SubTree   left: " + subTree.getLeft() + " Right: " + subTree.getRight() + " Level: " + subTree.getLevel());
            if (movedToRight) {
                LOGGER.debug("Category '" + id + "' is moved right.");
            } else {
                LOGGER.debug("Category '" + id + "' is moved left.");
            }
        }
        if (movedToRight)
            updateMoveRight(connection, oldParent, newParent, left, right, index, oldIndex);
        else
            updateMoveLeft(connection, oldParent, newParent, left, right, index, oldIndex);
        // use newParent.left+1 if no left sibling else leftSibling.right+1
        int leftStart = (index == 0) ? (getLeftRightValues(newParent.getId())[0] + 1)
                : (getLeftRightValues(newParent.getChildren().get(index - 1).getId())[1] + 1);
        // update Left, Right and Level values
        calculateLeftRightAndLevel(subTree, leftStart, newParent.getLevel() + 1);
        // only update oldParent if newParent is not its ancestor
        boolean updateOldParent = (oldParent.getLeft() < newParent.getLeft() || oldParent.getRight() > newParent.getRight()) ? true : false;
        // only update newParent if newParent!=oldParent and
        // oldParent is not its ancestor
        boolean updateNewParent = (!oldParent.getId().equals(newParent.getId()) && (newParent.getLeft() < oldParent.getLeft() || newParent.getRight() > oldParent
                .getRight())) ? true : false;
        if (updateOldParent) {
            LOGGER.debug("Updating old parent " + oldParent.getId());
            session.update(oldParent);
        }
        if (updateNewParent) {
            LOGGER.debug("Updating new parent " + newParent.getId());
            session.update(newParent);
        }
    }

    public void removeLabel(MCRCategoryID id, String lang) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        Iterator<MCRLabel> it = category.labels.iterator();
        while (it.hasNext()) {
            if (lang.equals(it.next().getLang())) {
                it.remove();
            }
        }
        session.update(category);
    }

    public void replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        if (!exist(newCategory.getId())) {
            throw new IllegalArgumentException("MCRCategory can not be replaced. MCRCategoryID '" + newCategory.getId() + "' is unknown.");
        }
        final MCRHIBConnection connection = MCRHIBConnection.instance();
        Session session = connection.getSession();
        MCRCategoryImpl oldCategory = getByNaturalID(session, newCategory.getId());
        // old Map with all Categories referenced by ID
        Map<MCRCategoryID, MCRCategoryImpl> oldMap = new HashMap<MCRCategoryID, MCRCategoryImpl>();
        fillIDMap(oldMap, oldCategory);
        // detatch from session (all sub categories are fetched)
        session.evict(oldCategory);
        MCRCategoryImpl newCategoryImpl = MCRCategoryImpl.wrapCategory(newCategory, oldCategory.getParent(), oldCategory.getRoot());
        Map<MCRCategoryID, MCRCategoryImpl> newMap = new HashMap<MCRCategoryID, MCRCategoryImpl>();
        fillIDMap(newMap, newCategoryImpl);
        // set internalID of known categories
        for (MCRCategoryImpl category : newMap.values()) {
            MCRCategoryImpl oldValue = oldMap.get(category.getId());
            if (oldValue != null) {
                category.setInternalID(oldValue.getInternalID());
            }
        }
        // calculate left, right and level values
        int diffNodes = newMap.size() - oldMap.size();
        LOGGER.debug("Update changes classification node size by: " + diffNodes);
        int increment = diffNodes * 2;
        if (increment != 0 && oldCategory.isCategory()) {
            final int left = getRightSiblingOrOfAncestor((MCRCategoryImpl) oldCategory.getParent(), oldCategory.getPositionInParent() + 1).getLeft();
            final int maxLeft = ((MCRCategoryImpl) oldCategory.getRoot()).getRight();
            final int right = getRightSiblingOrParent((MCRCategoryImpl) oldCategory.getParent(), oldCategory.getPositionInParent() + 1).getRight();
            final int maxRight = maxLeft;
            updateLeftRightValueMax(connection, left, maxLeft, right, maxRight, increment);
        }
        calculateLeftRightAndLevel(newCategoryImpl, oldCategory.getLevel(), oldCategory.getLeft());
        if (oldCategory.isCategory()) {
            MCRCategoryImpl parent = (MCRCategoryImpl) oldCategory.getParent();
            parent.getChildren().set(oldCategory.getPositionInParent(), newCategoryImpl);
        }
        // delete removed categories
        for (MCRCategoryImpl category : oldMap.values()) {
            if (!newMap.containsKey(category.getId())) {
                LOGGER.info("Deleting category :" + category.getId());
                session.delete(category);
            }
        }
        session.saveOrUpdate(newCategoryImpl);
    }

    public void setLabel(MCRCategoryID id, MCRLabel label) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        Iterator<MCRLabel> it = category.labels.iterator();
        while (it.hasNext()) {
            if (label.getLang().equals(it.next().getLang())) {
                it.remove();
            }
        }
        category.labels.add(label);
        session.update(category);
    }

    /**
     * calculates left and right value throug the subtree rooted at
     * <code>co</code>.
     * 
     * @param co
     *            root node of subtree
     * @param leftStart
     *            co.left will be set to this value
     * @param levelStart
     *            co.getLevel() will return this value
     * @return co.right
     */
    static int calculateLeftRightAndLevel(MCRCategoryImpl co, int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        co.setLeft(leftStart);
        co.setLevel(levelStart);
        for (MCRCategory child : co.getChildren()) {
            LOGGER.debug(child.getId());
            curValue = calculateLeftRightAndLevel((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        co.setRight(++curValue);
        return curValue;
    }

    static MCRCategoryImpl getLeftSiblingOrOfAncestor(MCRCategoryImpl newParent, int index) {
        if (index > 0) {
            // has left sibling
            return (MCRCategoryImpl) newParent.getChildren().get(index - 1);
        }
        if (newParent.getParent() != null) {
            // recursive call to get left sibling of parent
            int positionInParent = newParent.getPositionInParent();
            return getLeftSiblingOrOfAncestor((MCRCategoryImpl) newParent.getParent(), positionInParent);
        }
        return newParent;// is root
    }

    static MCRCategoryImpl getLeftSiblingOrParent(MCRCategoryImpl newParent, int index) {
        if (index == 0) {
            return newParent;
        }
        return (MCRCategoryImpl) newParent.getChildren().get(index - 1);
    }

    static MCRCategoryImpl getRightSiblingOrOfAncestor(MCRCategoryImpl newParent, int index) {
        if (index + 1 < newParent.getChildren().size()) {
            // has right sibling
            return (MCRCategoryImpl) newParent.getChildren().get(index + 1);
        }
        if (newParent.getParent() != null) {
            // recursive call to get right sibling of parent
            int positionInParent = newParent.getPositionInParent();
            return getRightSiblingOrOfAncestor((MCRCategoryImpl) newParent.getParent(), positionInParent);
        }
        return newParent;// is root
    }

    static MCRCategoryImpl getRightSiblingOrParent(MCRCategoryImpl newParent, int index) {
        if (index + 1 == newParent.getChildren().size()) {
            return newParent;
        }
        // get Element at index that would be at index+1 after insert
        return (MCRCategoryImpl) newParent.getChildren().get(index + 1);
    }

    /**
     * return true if a node moved in Category tree is moved to the right.
     * 
     * If the <code>newParent</code> is not an ancestor node of
     * <code>oldParent</code>, a simple comparison of their <code>left</code>
     * values is done to determine if a node is moved to the right.
     * 
     * If the <code>newParent</code> is an ancestor node of
     * <code>oldParent</code>, the ancestor axis is walked up to the direct
     * child of <code>newParent</code>. The position of the direct child in
     * the childrenList of <code>newParent</code> is compared to
     * <code>newIndex</code> to determine if a node is moved to the right. If
     * <code>oldParent</code> is an ancestor <code>newParent</code> the
     * solution is analog.
     * 
     * @param oldParent
     * @param newParent
     * @param newIndex
     * @param oldIndex
     * @return
     */
    static boolean isCategoryMovedRight(MCRCategoryImpl oldParent, MCRCategoryImpl newParent, int newIndex, int oldIndex) {
        int newParentLevel = newParent.getLevel();
        int oldParentLevel = oldParent.getLevel();
        if (newParent == oldParent) {
            LOGGER.debug("oldParent is newParent");
            return (newIndex > oldIndex);
        }
        if (newParentLevel < oldParentLevel && !((oldParent.getLeft() < newParent.getLeft() || oldParent.getRight() > newParent.getRight()))) {
            // newParent is ancestor of oldParent
            MCRCategory node = oldParent;
            while (!node.getParent().getId().equals(newParent.getId())) {
                // walk ancestor axis up while node not direct child of
                // newParent
                node = node.getParent();
            }
            if (((MCRCategoryImpl) node).getPositionInParent() < newIndex) {
                // old ancestor axis is left from new index
                return true;
            }
            // old ancestor axis is right from new index
            return false;
        } else if (newParentLevel > oldParentLevel && !((newParent.getLeft() < oldParent.getLeft() || newParent.getRight() > oldParent.getRight()))) {
            MCRCategory node = newParent;
            while (!node.getParent().getId().equals(oldParent.getId())) {
                // walk ancestor axis up while node not direct child of
                // newParent
                node = node.getParent();
            }
            if (((MCRCategoryImpl) node).getPositionInParent() > oldIndex) {
                // new ancestor axis is left from old index
                return true;
            }
            return false;
        }
        /*
         * oldParent and newParent are on the same level in the tree OR neither
         * newParent is ancestor of oldParent nor oldParent is ancestor of
         * newParent.
         */
        LOGGER.debug("oldParent and newParent are on the same level in the tree");
        return (newParent.getLeft() > oldParent.getLeft());
    }

    private static MCRCategoryImpl copyDeep(MCRCategoryImpl category, int level) {
        MCRCategoryImpl newCateg = new MCRCategoryImpl();
        int childAmount = (level != 0) ? category.getChildren().size() : 0;
        newCateg.setChildren(new ArrayList<MCRCategory>(childAmount));
        newCateg.setId(category.getId());
        newCateg.setLabels(category.getLabels());
        newCateg.setRoot(category.root);
        newCateg.setURI(category.getURI());
        if (childAmount > 0) {
            for (MCRCategory child : category.getChildren()) {
                newCateg.getChildren().add(copyDeep((MCRCategoryImpl) child, level - 1));
            }
        }
        return newCateg;
    }

    private static MCRCategoryImpl getByNaturalID(Session session, MCRCategoryID id) {
        final Criteria criteria = session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.id());
        Integer internalID = (Integer) criteria.setCacheable(true).add(MCRCategoryExpression.eq(id)).uniqueResult();
        return (MCRCategoryImpl) session.get(CATEGRORY_CLASS, internalID);
    }

    private static void fillIDMap(Map<MCRCategoryID, MCRCategoryImpl> map, MCRCategoryImpl category) {
        map.put(category.getId(), category);
        for (MCRCategory subCategory : category.getChildren()) {
            fillIDMap(map, (MCRCategoryImpl) subCategory);
        }
    }

    private static Integer[] getLeftRightValues(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(
                Projections.projectionList().add(Projections.property("left")).add(Projections.property("right")));
        c.add(MCRCategoryExpression.eq(id));
        Object[] result = (Object[]) c.uniqueResult();
        Integer[] iResult = new Integer[] { (Integer) result[0], (Integer) result[1] };
        return iResult;
    }

    private static int getNumberOfChildren(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.rowCount());
        c.add(Subqueries.propertyEq("parent", DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID")).add(
                MCRCategoryExpression.eq(id))));
        return ((Number) c.uniqueResult()).intValue();
    }

    /**
     * @param session
     * @param left
     * @param increment
     */
    private static void updateLeftRightValue(MCRHIBConnection connection, int left, final int increment) {
        Session session = connection.getSession();
        connection.flushSession();
        LOGGER.debug("LEFT AND RIGHT values need updates. Left=" + left + ", increment by: " + increment);
        Query leftQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeft");
        leftQuery.setInteger("left", left);
        leftQuery.setInteger("increment", increment);
        int leftChanges = leftQuery.executeUpdate();
        Query rightQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateRight");
        rightQuery.setInteger("left", left);
        rightQuery.setInteger("increment", increment);
        int rightChanges = rightQuery.executeUpdate();
        connection.flushSession();
        LOGGER.debug("Updated " + leftChanges + " left and " + rightChanges + " right values.");
    }

    private static void updateLeftRightValueMax(MCRHIBConnection connection, int left, int maxLeft, int right, int maxRight, final int increment) {
        Session session = connection.getSession();
        connection.flushSession();
        LOGGER.debug("LEFT AND RIGHT values need updates. Left=" + left + ", MaxLeft=" + maxLeft + ", Right=" + right + ", MaxRight=" + maxRight
                + " increment by: " + increment);
        Query leftQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeftWithMax");
        leftQuery.setInteger("left", left);
        leftQuery.setInteger("max", maxLeft);
        leftQuery.setInteger("increment", increment);
        int leftChanges = leftQuery.executeUpdate();
        Query rightQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateRightWithMax");
        rightQuery.setInteger("right", right);
        rightQuery.setInteger("max", maxRight);
        rightQuery.setInteger("increment", increment);
        int rightChanges = rightQuery.executeUpdate();
        connection.flushSession();
        LOGGER.debug("Updated " + leftChanges + " left and " + rightChanges + " right values.");
    }

    private static void updateMoveLeft(MCRHIBConnection connection, MCRCategoryImpl oldParent, MCRCategoryImpl newParent, int left, int right, int index,
            int oldIndex) {
        int nodes = 1 + (right - left) / 2;
        int increment = 2 * nodes;
        if (newParent == oldParent) {
            MCRCategoryImpl leftSiblingOrParent = getLeftSiblingOrParent(newParent, index);
            int newLeft = leftSiblingOrParent.getLeft();
            int newRight = (leftSiblingOrParent == newParent) ? newLeft + 1 : leftSiblingOrParent.getRight();
            updateLeftRightValueMax(connection, newLeft, left, newRight, right, increment);
            connection.getSession().refresh(newParent);
        } else {
            MCRCategoryImpl rightSiblingOrOfAncestor = getRightSiblingOrOfAncestor(newParent, index);
            int maxLeft = rightSiblingOrOfAncestor.getLeft();
            MCRCategoryImpl rightSiblingOrParent = getRightSiblingOrParent(newParent, index);
            int maxRight = rightSiblingOrParent.getRight();
            int minRight = right;
            if (maxRight <= right) {
                // Update at least newParent
                minRight = maxRight - 1;
            }
            updateLeftRightValueMax(connection, left, maxLeft, minRight, maxRight, increment);
            connection.getSession().refresh(rightSiblingOrOfAncestor);
        }
    }

    private static void updateMoveRight(MCRHIBConnection connection, MCRCategoryImpl oldParent, MCRCategoryImpl newParent, int left, int right, int index,
            int oldIndex) {
        int nodes = 1 + (right - left) / 2;
        int increment = -2 * nodes;
        if (newParent == oldParent) {
            MCRCategoryImpl leftSibling = getLeftSiblingOrParent(newParent, index);
            int maxLeft=leftSibling.getLeft();
            int maxRight=leftSibling.getRight();
            updateLeftRightValueMax(connection, right, maxLeft, left, maxRight, increment);
            connection.getSession().refresh(newParent);
        } else {
            int maxLeft = getLeftSiblingOrParent(newParent, index).getLeft();
            int minLeft = left;
            if (maxLeft <= left) {
                // update at least newParent
                minLeft = maxLeft - 1;
            }
            MCRCategoryImpl leftSiblingOrOfAncestor = getLeftSiblingOrOfAncestor(newParent, index);
            int maxRight = leftSiblingOrOfAncestor.getRight();
            updateLeftRightValueMax(connection, minLeft, maxLeft, right, maxRight, increment);
            connection.getSession().refresh(leftSiblingOrOfAncestor);
        }
    }
}
