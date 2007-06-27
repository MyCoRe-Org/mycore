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

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.TypedValue;

import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Hibernate Criterion used to query for a MCRCategory.
 * 
 * Sample: You get a <code>MCRCategoryImpl</code> instance with
 * <code>id</code> instance of MCRCategoryID:
 * 
 * <pre>
 *  Criteria c = session.createCriteria(MCRCategoryImpl.class);
 *  MCRCategoryImpl category = c.add(MCRCategoryExpression.eq(id)).uniqueResult();
 * </pre>
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @since 2.0
 * @see Criteria
 * @see Session
 */
class MCRCategoryExpression implements Criterion {
    private static final long serialVersionUID = 2518173920912008890L;

    public static MCRCategoryExpression eq(MCRCategoryID id) {
        return new MCRCategoryExpression(id);
    }

    MCRCategoryID id;

    Criterion derived;

    private MCRCategoryExpression(MCRCategoryID id) {
        this.id = id;
        if (id.getID() == null) {
            // handle Classification
            derived = Restrictions.and(Restrictions.eq("id.rootID", id.getRootID()), Restrictions.isNull("id.ID"));
        } else {
            // handle Category
            derived = Restrictions.eq("id", id);
        }

    }

    public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
        return derived.getTypedValues(criteria, criteriaQuery);
    }

    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
        return derived.toSqlString(criteria, criteriaQuery);
    }

}