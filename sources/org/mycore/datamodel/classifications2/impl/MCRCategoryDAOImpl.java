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

import java.util.Collection;
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
        return ((Number) criteria.uniqueResult()).intValue() > 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.classifications2.MCRClassificationService#hasChildren(org.mycore.datamodel.classifications2.MCRCategoryID)
     */
    public boolean hasChildren(MCRCategoryID cid) {
        // SELECT * FROM MCRCATEGORY WHERE PARENTID=(SELECT INTERNALID FROM
        // MCRCATEGORY WHERE rootID=cid.getRootID() and ID...);
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(CATEGRORY_CLASS).setProjection(Projections.rowCount());
        c.add(Subqueries.propertyEq("parent", DetachedCriteria.forClass(CATEGRORY_CLASS).setProjection(Projections.property("internalID")).add(
                CategoryExpression.eq(cid))));
        return ((Number)c.uniqueResult()).intValue() > 0;
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
        // Criteria criteria =
        // MCRHIBConnection.instance().getSession().createCriteria(CATEGRORY_CLASS);
        // criteria.add(Restrictions.eq("parent", cid));
        Query q = MCRHIBConnection.instance().getSession().getNamedQuery(CATEGRORY_CLASS.getName() + ".getChildren");
        q.setString("parentID", cid.getID());
        q.setString("rootID", cid.getRootID());
        return (List<MCRCategory>) q.list();
    }

    public void addCategory(MCRCategoryID parentID, MCRCategory category) {
        int leftStart = 0;
        int levelStart = 0;
        Session session = MCRHIBConnection.instance().getSession();
        MCRCategoryImpl parent = null;
        if (parentID != null) {
            parent = (MCRCategoryImpl) session.get(CATEGRORY_CLASS, parentID);
            levelStart = parent.getLevel() + 1;
            leftStart = parent.getLeft() + 1;
        }
        LOGGER.info("Calculating LEFT,RIGHT and LEVEL attributes...");
        int nodes = calculateLeftRightAndLevel(MCRCategoryImpl.wrapCategory(category, parent, (parent == null) ? category.getRoot() : parent.getRoot()),
                leftStart, levelStart) / 2;
        LOGGER.info("Calculating LEFT,RIGHT and LEVEL attributes. Done! Nodes: " + nodes);
        if (parentID != null) {
            LOGGER.info("LEFT AND RIGHT values need updates");
            Query leftQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeft");
            leftQuery.setInteger(":left", leftStart);
            leftQuery.setInteger(":increment", nodes * 2);
            int leftChanges = leftQuery.executeUpdate();
            Query rightQuery = session.getNamedQuery(CATEGRORY_CLASS.getName() + ".updateLeft");
            rightQuery.setInteger(":left", leftStart);
            rightQuery.setInteger(":increment", nodes * 2);
            int rightChanges = rightQuery.executeUpdate();
            LOGGER.info("Updated " + leftChanges + " left and " + rightChanges + " right values.");
        }
        session.save(category);
        session.evict(category);
        LOGGER.info("Categorie saved.");
    }

    public void deleteCategory(MCRCategoryID id) {
        Session session = MCRHIBConnection.instance().getSession();
        LOGGER.info("Will get: " + id);
        MCRCategory category = (MCRCategory) session.createCriteria(CATEGRORY_CLASS).add(CategoryExpression.eq(id)).uniqueResult();
        LOGGER.info("Will delete: " + category.getId());
        session.delete(category);
    }

    public Collection<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        // TODO Auto-generated method stub
        return null;
    }

    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<MCRCategory> getParents(MCRCategoryID id) {
        // TODO Auto-generated method stub
        return null;
    }

    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        return (MCRCategory) MCRHIBConnection.instance().getSession().get(CATEGRORY_CLASS, MCRCategoryID.rootID(baseID.getRootID()));
    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
        // TODO Auto-generated method stub

    }

    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        // TODO Auto-generated method stub

    }

    public void removeLabel(MCRCategoryID id, String lang) {
        // TODO Auto-generated method stub

    }

    public void setLabel(MCRCategoryID id, MCRLabel label) {
        // TODO Auto-generated method stub

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
