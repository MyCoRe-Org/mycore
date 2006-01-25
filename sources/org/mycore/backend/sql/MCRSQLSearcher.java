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

package org.mycore.backend.sql;

import java.util.List;

import org.mycore.access.MCRAccessManager;
import org.mycore.backend.query.MCRQuerySearcher;
import org.mycore.common.MCRSessionMgr;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearchField;

/**
 * SQL implementation of the searcher
 * 
 * @author Arne Seifert
 * 
 */
public class MCRSQLSearcher extends MCRQuerySearcher {
    /**
     * method runs given query-string access-control included: id of object will
     * testet for rules of the "READ"-pool
     * 
     * @param query
     *            xml-query string as jdom document
     * @return MCRResults with MCRHit-objects
     */
    public MCRResults search(MCRCondition condition, List order, int maxResults) {

        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRResults result = new MCRResults();

        try {
            MCRSQLRowReader reader;
            reader = c.doQuery(new MCRSQLQuery(condition, order, maxResults).getSQLQuery());

            while (reader.next()) {
                String id = reader.getString("MCRID");

                MCRHit hit = new MCRHit(id);

                /* fill hit meta */
                for (int j = 0; j < order.size(); j++) {
                    String key = ((MCRSearchField) order.get(j)).getName();
                    String value = (String) reader.getString(((MCRSearchField) order.get(j)).getName());
                    hit.addSortData(key, value);
                }
                
                if ((maxResults > 0) && (result.getNumHits() <= maxResults)) {
                    result.addHit(hit);
                }else{
                    break;
                }
            }

            if (order.size() > 0) {
                result.setSorted(true);
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            c.release();
        }
        return result;
    }
}
