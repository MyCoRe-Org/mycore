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
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;

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
        if (!MCRSQLConnection.doesTableExist(table)) {
            createSQLQueryTable();
        }
    }

    /**
     * This method creates the SQL table that stores the field values to query.
     */
    private void createSQLQueryTable() {
        MCRSQLStatement sQuery = new MCRSQLStatement(table);
        sQuery.addColumn("MCRID VARCHAR(64) NOT NULL");
        sQuery.addColumn("RETURNID VARCHAR(64) NOT NULL");

        List fds = MCRFieldDef.getFieldDefs(getIndex());
        for (int i = 0; i < fds.size(); i++) {
            MCRFieldDef fd = (MCRFieldDef) (fds.get(i));
            if (!MCRFieldDef.SEARCHER_HIT_METADATA.equals(fd.getSource()))
                sQuery.addColumn(buildColumn(fd));
        }

        MCRSQLConnection.justDoUpdate(sQuery.toCreateTableStatement());

        MCRSQLStatement sIndex = new MCRSQLStatement(table).addColumn("MCRID");
        MCRSQLConnection.justDoUpdate(sIndex.toIndexStatement());
    }

    /**
     * Builds SQL column definition from MCRFieldDef object when SQL table is
     * created.
     * 
     * @param fd
     *            the search field definition
     * @return formatted SQL string for table generation
     */
    private String buildColumn(MCRFieldDef fd) {
        StringBuffer sbRet = new StringBuffer().append(fd.getName().toUpperCase());
        String type = fd.getDataType().toLowerCase();

        if (type.equals("text") || type.equals("name") || type.equals("identifier")) {
            sbRet.append("TEXT");
        } else if (type.equals("date")) {
            sbRet.append("DATE");
        } else if (type.equals("time")) {
            sbRet.append("TIME");
        } else if (type.equals("timestamp")) {
            sbRet.append("TIMESTAMP");
        } else if (type.equals("integer")) {
            sbRet.append("INT");
        } else if (type.equals("decimal")) {
            sbRet.append("DOUBLE");
        } else if (type.equals("boolean")) {
            sbRet.append("SMALLINT");
        }

        return sbRet.toString();
    }

    // TODO: store all values, not just the first for each repeated field
    protected void addToIndex(String entryID, String returnID, List fields) {
        MCRSQLStatement query = new MCRSQLStatement(table);
        query.setValue(new MCRSQLColumn("MCRID", entryID, "string"));
        query.setValue(new MCRSQLColumn("RETURNID", returnID, "string"));

        Hashtable used = new Hashtable();

        for (int i = 0; i < fields.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue) (fields.get(i));

            String value = fv.getValue();
            String name = fv.getField().getName();
            String type = fv.getField().getDataType();

            // Store only first occurrence of field
            if (used.containsKey(name)) {
                continue;
            }
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

    public MCRResults search(MCRQuery query) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRResults result = new MCRResults();
        MCRCondition cond = query.getCondition();
        List sortBy = query.getSortBy();
        int maxResults = query.getMaxResults();

        try {
            String sql = new MCRSQLQuery(table, cond, sortBy, maxResults).getSQLQuery();
            LOGGER.debug(sql);
            MCRSQLRowReader reader = c.doQuery(sql);

            while (reader.next() && (maxResults <= 0 ? true : result.getNumHits() < maxResults)) {
                String id = reader.getString("RETURNID");

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
