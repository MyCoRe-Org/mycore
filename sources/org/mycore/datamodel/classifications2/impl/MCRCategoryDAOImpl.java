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
import java.util.Collection;
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
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl parent = null;
        if (parentID != null) {
            parent = (MCRCategoryImpl) session.createCriteria(CATEGRORY_CLASS).add(CategoryExpression.eq(parentID)).uniqueResult();
            levelStart = parent.getLevel() + 1;
            leftStart = parent.getLeft() + 1;
            parent.getChildren().add(category);
        }
        LOGGER.info("Calculating LEFT,RIGHT and LEVEL attributes...");
        int nodes = calculateLeftRightAndLevel(MCRCategoryImpl.wrapCategory(category, parent, (parent == null) ? category.getRoot() : parent.getRoot()),
                leftStart, levelStart) / 2;
        LOGGER.info("Calculating LEFT,RIGHT and LEVEL attributes. Done! Nodes: " + nodes);
        if (parentID != null) {
            LOGGER.info("LEFT AND RIGHT values need updates");
            Query leftQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeft");
            leftQuery.setInteger("left", leftStart);
            leftQuery.setInteger("increment", nodes * 2);
            int leftChanges = leftQuery.executeUpdate();
            Query rightQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateRight");
            rightQuery.setInteger("left", leftStart);
            rightQuery.setInteger("increment", nodes * 2);
            int rightChanges = rightQuery.executeUpdate();
            LOGGER.info("Updated " + leftChanges + " left and " + rightChanges + " right values.");
        }
        session.save(category);
        if (parent == null) {
            session.evict(category);
        } else {
            session.evict(parent);
        }
        LOGGER.info("Categorie saved.");
    }

    public void deleteCategory(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        LOGGER.info("Will get: " + id);
        MCRCategory category = (MCRCategory) session.createCriteria(CATEGRORY_CLASS).add(CategoryExpression.eq(id)).uniqueResult();
        if (category == null) {
            throw new MCRPersistenceException("Category " + id + " was not found. Delete aborted.");
        }
        LOGGER.info("Will delete: " + category.getId());
        session.delete(category);
    }

    public Collection<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS);
        Integer[] leftRight = getLeftRightValues(baseID);
        c.add(Restrictions.eq("id.rootID", baseID.getRootID()));
        c.add(Restrictions.between("left", leftRight[0], leftRight[1]));
        // TODO: add lang and text to query
        return null;
    }

    @SuppressWarnings("unchecked")
    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        // TODO Auto-generated method stub
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS);
        c.setCacheable(true);
        c.add(CategoryExpression.eq(id));
        MCRCategoryImpl category = (MCRCategoryImpl) c.uniqueResult();
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
        // TODO Auto-generated method stub
        return null;
    }

    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        return (MCRCategory) MCRHIBConnection.instance().getSession().get(CATEGRORY_CLASS, MCRCategoryID.rootID(baseID.getRootID()));
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
        int index = getNumberOfChildren(newParentID);
        moveCategory(id, newParentID, index);
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        // TODO Auto-generated method stub

    }

    public void removeLabel(MCRCategoryID id, String lang) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl category = (MCRCategoryImpl) session.createCriteria(CATEGRORY_CLASS).add(CategoryExpression.eq(id)).uniqueResult();
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
        MCRCategoryImpl category = (MCRCategoryImpl) session.createCriteria(CATEGRORY_CLASS).add(CategoryExpression.eq(id)).uniqueResult();
        Iterator<MCRLabel> it = category.labels.iterator();
        while (it.hasNext()) {
            if (label.getLang().equals(it.next().getLang())) {
                it.remove();
            }
        }
        category.labels.add(label);
        session.update(category);
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
