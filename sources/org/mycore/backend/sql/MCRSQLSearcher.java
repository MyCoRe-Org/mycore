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

import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;

// TODO: build table
// TODO: load entries

/**
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 */
public class MCRSQLSearcher extends MCRSearcher {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRSQLSearcher.class.getName());

    private String table;

    public void init(String ID) {
        super.init(ID);
        this.table = MCRConfiguration.instance().getString(prefix + "TableName");
    }

    // TODO: store all values, not just the first for each repeated field
    protected void addToIndex(String entryID, List fields) {
        MCRSQLStatement query = new MCRSQLStatement(table);
        Hashtable used = new Hashtable();

        for (int i = 0; i < fields.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue) (fields.get(i));

            String value = fv.getValue();
            String name = fv.getField().getName();
            String type = fv.getField().getDataType();

            // Store only first occurrence of field
            if (used.containsKey(name))
                continue;
            else
                used.put(name, name);

            MCRSQLColumn col = null;
            if (type.equals("text") || type.equals("name") || type.equals("identifier")) {
                col = new MCRSQLColumn(name, value.replaceAll("\'", "''"), "string");
            } else if (type.equals("date")) {
                col = new MCRSQLColumn(name, value.replaceAll("\'", "''"), "string");
            } else if (type.equals("integer")) {
                col = new MCRSQLColumn(name, value, "integer");
            } else if (type.equals("decimal")) {
                col = new MCRSQLColumn(name, value, "decimal");
            } else if (type.equals("boolean")) {
                col = new MCRSQLColumn(name, value, "boolean");
            }

            if (col != null)
                query.setValue(col);
        }

        String sql = query.toTypedInsertStatement();
        LOGGER.debug(sql);
        MCRSQLConnection.justDoUpdate(sql);
    }

    protected void removeFromIndex(String entryID) {
        MCRSQLStatement query = new MCRSQLStatement(table).setCondition("MCRID", entryID);
        String sql = query.toDeleteStatement();
        LOGGER.debug(sql);
        MCRSQLConnection.justDoUpdate(sql);
    }

    public MCRResults search(MCRCondition query, List sortBy, int maxResults) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRResults result = new MCRResults();

        try {
            String sql = new MCRSQLQuery(table, query, sortBy, maxResults).getSQLQuery();
            LOGGER.debug(sql);
            MCRSQLRowReader reader = c.doQuery(sql);

            while (reader.next() && (maxResults > 0) && (result.getNumHits() < maxResults)) {
                String id = reader.getString("MCRID");

                MCRHit hit = new MCRHit(id);

                // Add hit sort data
                if (sortBy != null)
                    for (int j = 0; j < sortBy.size(); j++) {
                        MCRFieldDef fd = ((MCRSortBy) sortBy.get(j)).getField();
                        String value = reader.getString(fd.getName());
                        hit.addSortData(new MCRFieldValue(fd, value));
                    }
                result.addHit(hit);
            }

            if ((sortBy != null) && (sortBy.size() > 0)) {
                result.setSorted(true);
            }
        } finally {
            c.release();
        }
        return result;
    }
}
