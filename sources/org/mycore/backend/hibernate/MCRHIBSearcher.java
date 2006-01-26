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

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.query.MCRQuerySearcher;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearchField;

/**
 * Hibernate implementation of the searcher
 * 
 * @author Arne Seifert
 * 
 */
public class MCRHIBSearcher extends MCRQuerySearcher {

    public MCRResults search(MCRCondition condition, List order, int maxResults) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRResults result = new MCRResults();

        try {
            MCRHIBQuery hibquery = new MCRHIBQuery(condition, order);
            List l = session.createQuery(hibquery.getHIBQuery()).list();

            for (int i = 0; i < l.size(); i++) {
                MCRHIBQuery tmpquery = new MCRHIBQuery(l.get(i));

                MCRHit hit = new MCRHit((String) tmpquery.getValue("getmcrid"));

                // fill hit meta
                for (int j = 0; j < order.size(); j++) {
                    String key = ((MCRSearchField) order.get(j)).getName();
                    Object valueObj = tmpquery.getValue("get" + ((MCRSearchField) order.get(j)).getName());
                    String value = "";
                    if (valueObj instanceof java.sql.Date) {
                    	value = ((java.sql.Date) valueObj).toString() ;
                    }else{
                    	value = (String) valueObj ;
                    }
                    if (value == null) value = "";
                    hit.addSortData(key, value);
                }

                if ((maxResults > 0) && (result.getNumHits() <= maxResults)) {
                    result.addHit(hit);
                }else{
                    break;
                }
            }
            tx.commit();

            if (order.size() > 0) {
                result.setSorted(true);
            }
        } catch (Exception e) {
            tx.rollback();
            logger.error("error in MCRHibSearcher", e);
        } finally {
            session.close();
        }

        return result;
    }
}
