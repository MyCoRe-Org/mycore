/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate;

import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.sql.MCRSQLSearcher;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Hibernate implementation of MCRSearcher
 * 
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 */
public class MCRHIBSearcher extends MCRSearcher {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRSQLSearcher.class.getName());

    protected void addToIndex(String entryID, List fields) {
        MCRHIBQuery query = new MCRHIBQuery();
        Hashtable used = new Hashtable();

        query.setValue("setmcrid", entryID);

        for (int i = 0; i < fields.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue) (fields.get(i));

            // Store only first occurrence of field
            if (used.containsKey(fv.getField()))
                continue;
            else
                used.put(fv.getField(), fv.getField());

            query.setValue("set" + fv.getField().getName(), fv.getValue());
        }

        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            session.saveOrUpdate(query.getQueryObject());
            tx.commit();
        } catch (Exception ex) {
            LOGGER.error(ex);
            tx.rollback();
        } finally {
            session.close();
        }
    }

    protected void removeFromIndex(String entryID) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            session.createQuery("delete MCRQuery where MCRID =\'" + entryID + "\'").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            LOGGER.error(e);
        } finally {
            session.close();
        }

    }

    public MCRResults search(MCRCondition condition, List order, int maxResults) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRResults results = new MCRResults();

        try {
            MCRHIBQuery hibquery = new MCRHIBQuery(condition, order);
            List l = session.createQuery(hibquery.getHIBQuery()).list();

            for (int i = 0; (i < l.size()) && (maxResults > 0) && (results.getNumHits() < maxResults); i++) {
                MCRHIBQuery tmpquery = new MCRHIBQuery(l.get(i)); // ?
                MCRHit hit = new MCRHit((String) (tmpquery.getValue("getmcrid")));

                // Add hit sort data
                if (order != null)
                    for (int j = 0; j < order.size(); j++) {
                        MCRSortBy by = (MCRSortBy) (order.get(j));
                        Object valueObj = tmpquery.getValue("get" + by.getField().getName());
                        String value;
                        if (valueObj instanceof java.sql.Date) {
                            value = ((java.sql.Date) valueObj).toString();
                        } else {
                            value = (String) valueObj;
                        }
                        hit.addSortData(new MCRFieldValue(by.getField(), value));
                    }
                results.addHit(hit);
            }
            tx.commit();

            if ((order != null) && (order.size() > 0))
                results.setSorted(true);
        } catch (Exception ex) {
            tx.rollback();
            LOGGER.error("Exception in MCRHibSearcher", ex);
        } finally {
            session.close();
        }

        return results;
    }
}
