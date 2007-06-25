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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.engine.TypedValue;

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

    private static final Class CATEGRORY_CLASS = MCRCategoryImpl.class;

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
    protected static int calculateLeftRightAndLevel(MCRCategoryImpl co, int leftStart, int levelStart) {
        int curValue = leftStart;
        final int nextLevel = levelStart + 1;
        co.setLeft(leftStart);
        co.setLevel(levelStart);
        for (MCRCategory child : co.getChildren()) {
            LOGGER.info(child.getId());
            curValue = calculateLeftRightAndLevel((MCRCategoryImpl) child, ++curValue, nextLevel);
        }
        co.setRight(++curValue);
        return curValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRCategoryDAO#exist(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public boolean exist(MCRCategoryID id) {
        Criteria criteria = MCRHIBConnection.instance().getSession().createCriteria(CATEGRORY_CLASS);
        criteria.setProjection(Projections.rowCount()).add(CategoryExpression.eq(id));
        Number result = (Number) criteria.uniqueResult();
        if (result == null) {
            return false;
        }
        return result.intValue() > 0;
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#getChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    @SuppressWarnings("unchecked")
    public List<MCRCategory> getChildren(MCRCategoryID cid) {
        if (!exist(cid)) {
            return new MCRCategoryImpl.ChildList(null, null);
        }
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).add(
                Subqueries.propertyEq("parent", DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID")).add(
                        CategoryExpression.eq(cid))));
        return (List<MCRCategory>) c.list();
    }

    private static int getNumberOfChildren(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.rowCount());
        c.add(Subqueries.propertyEq("parent", DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID")).add(
                CategoryExpression.eq(id))));
        return ((Number) c.uniqueResult()).intValue();
    }

    private static Integer[] getLeftRightValues(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(
                Projections.projectionList().add(Projections.property("left")).add(Projections.property("right")));
        c.add(CategoryExpression.eq(id));
        Object[] result = (Object[]) c.uniqueResult();
        Integer[] iResult = new Integer[] { (Integer) result[0], (Integer) result[1] };
        return iResult;
    }

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
            parent.getChildren().add(category);
        }
        LOGGER.info("Calculating LEFT,RIGHT and LEVEL attributes...");
        final MCRCategoryImpl wrapCategory = MCRCategoryImpl.wrapCategory(category, parent, (parent == null) ? category.getRoot() : parent.getRoot());
        calculateLeftRightAndLevel(wrapCategory, leftStart, levelStart);
        // alway add +1 for the current node
        int nodes = 1 + (wrapCategory.getRight() - wrapCategory.getLeft()) / 2;
        LOGGER.info("Calculating LEFT,RIGHT and LEVEL attributes. Done! Nodes: " + nodes);
        if (parentID != null) {
            final int increment = nodes * 2;
            updateLeftRightValue(connection, leftStart, increment);
        }
        session.save(category);
        LOGGER.info("Categorie saved.");
    }

    /**
     * @param session
     * @param left
     * @param increment
     */
    private static void updateLeftRightValue(MCRHIBConnection connection, int left, final int increment) {
        Session session = connection.getSession();
        connection.flushSession();
        LOGGER.info("LEFT AND RIGHT values need updates. Left=" + left + ", increment by: " + increment);
        Query leftQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeft");
        leftQuery.setInteger("left", left);
        leftQuery.setInteger("increment", increment);
        int leftChanges = leftQuery.executeUpdate();
        Query rightQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateRight");
        rightQuery.setInteger("left", left);
        rightQuery.setInteger("increment", increment);
        int rightChanges = rightQuery.executeUpdate();
        connection.flushSession();
        LOGGER.info("Updated " + leftChanges + " left and " + rightChanges + " right values.");
    }

    public void deleteCategory(MCRCategoryID id) {
        final MCRHIBConnection connection = MCRHIBConnection.instance();
        Session session = connection.getSession();
        LOGGER.info("Will get: " + id);
        MCRCategoryImpl category = getByNaturalID(session, id);
        if (category == null) {
            throw new MCRPersistenceException("Category " + id + " was not found. Delete aborted.");
        }
        LOGGER.info("Will delete: " + category.getId());
        MCRCategory parent = category.parent;
        category.detachFromParent();
        session.delete(category);
        if (parent != null) {
            LOGGER.info("Left: " + category.getLeft() + " Right: " + category.getRight());
            // always add +1 for the currentNode
            int nodes = 1 + (category.getRight() - category.getLeft()) / 2;
            final int increment = nodes * -2;
            // decrement left and right values by nodes
            updateLeftRightValue(connection, category.getLeft(), increment);
        }
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
        // TODO Auto-generated method stub
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl category = getByNaturalID(session, id);
        return copyDeep(category, childLevel);
    }

    public static MCRCategoryImpl copyDeep(MCRCategoryImpl category, int level) {
        MCRCategoryImpl newCateg = new MCRCategoryImpl();
        int childAmount = (level > 0) ? category.getChildren().size() : 0;
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
        return (MCRCategory) MCRHIBConnection.instance().getSession().get(CATEGRORY_CLASS, MCRCategoryID.rootID(baseID.getRootID()));
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
        int index = getNumberOfChildren(newParentID);
        moveCategory(id, newParentID, index);
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl subTree = getByNaturalID(session, id);
        MCRCategoryImpl oldParent = (MCRCategoryImpl) subTree.getParent();
        MCRCategoryImpl newParent = getByNaturalID(session, newParentID);
        subTree.detachFromParent();
        newParent.getChildren().add(index, subTree);
        // update needed for old and newParent;
        // TODO: Update Left, Right and Level values
        boolean updateOldParent = (oldParent.getLeft() < newParent.getLeft() || oldParent.getRight() > newParent.getRight()) ? true : false;
        boolean updateNewParent = (!oldParent.getId().equals(newParent.getId())) ? true : false;
        if (updateOldParent) {
            LOGGER.info("Updating old parent " + oldParent.getId());
            session.update(oldParent);
        }
        if (updateNewParent) {
            LOGGER.info("Updating new parent " + newParent.getId());
            // session.update(newParent);
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

    private static MCRCategoryImpl getByNaturalID(Session session, MCRCategoryID id) {
        Integer internalID = (Integer) session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.id()).setCacheable(true)
                .add(CategoryExpression.eq(id)).uniqueResult();
        return (MCRCategoryImpl) session.get(CATEGRORY_CLASS, internalID);
    }

    private static class CategoryExpression implements Criterion {
        private static final long serialVersionUID = 2518173920912008890L;

        MCRCategoryID id;

        Criterion derived;

        private CategoryExpression(MCRCategoryID id) {
            this.id = id;
            if (id.getID() == null) {
                // handle Classification
                derived = Restrictions.and(Restrictions.eq("id.rootID", id.getRootID()), Restrictions.isNull("id.ID"));
            } else {
                // handle Category
                derived = Restrictions.eq("id", id);
            }

        }

        public static CategoryExpression eq(MCRCategoryID id) {
            return new CategoryExpression(id);
        }

        public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            return derived.getTypedValues(criteria, criteriaQuery);
        }

        public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            return derived.toSqlString(criteria, criteriaQuery);
        }

    }

}
