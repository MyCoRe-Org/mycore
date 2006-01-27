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

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.jdom.Element;
import org.mycore.backend.query.MCRQueryIndexer;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.fieldquery.MCRFieldValue;

/**
 * 
 * @author Arne Seifert
 * 
 */
public class MCRSQLIndexer extends MCRQueryIndexer {
    /**
     * method loads all searchfield values into database after clearing old
     * values. needs to be done after changes in the fielddefinition
     */
    public void initialLoad() {
        StringTokenizer tokenizer = new StringTokenizer(querytypes, ",");
        createSQLQueryTable();

        while (tokenizer.hasMoreTokens())
            queryManager.loadType(tokenizer.nextToken());
    }

    /**
     * method to update entries of given objectid
     * 
     * @param objectid
     *            as MCRObjectID
     */
    public void updateObject(MCRBase object) {
        try {
            deleteObject(object.getId());
            queryManager.create(object);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * method to delete all entries of given objectid
     * 
     * @param objectid
     *            as MCRObjectID
     */
    public void deleteObject(MCRObjectID objectid) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLStatement query = new MCRSQLStatement(SQLQueryTable).setCondition("MCRID", objectid.getId());

        try {
            c.doUpdate(query.toDeleteStatement());
        } catch (Exception e) {
            logger.error(e);
        } finally {
            c.release();
        }
    }

    /**
     * internal helper method
     * 
     * @param mcrid
     *            identifier for object
     * @param values
     *            list of values to be indexed vor search. list contains values
     *            for each field
     */
    public final void insertInQuery(String mcrid, List values) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLStatement query = new MCRSQLStatement(SQLQueryTable);

        for (int i = 0; i < values.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue)( values.get(i));
            String field = fv.getField().getName();
            String value = fv.getValue();
            String type  = fv.getField().getDataType();

            if (value != "") {
                if (type.equals("text") || type.equals("name") || type.equals("identifier")) {
                    query.setValue(new MCRSQLColumn(field, value.replaceAll("\'", "''"), "string"));
                } else if (type.equals("date")) {
                    query.setValue(new MCRSQLColumn(field, value.split("[|]")[0].replaceAll("\'", "''"), "string"));
                } else if (type.equals("integer")) {
                    query.setValue(new MCRSQLColumn(field, "" + Integer.parseInt(value.split("[|]")[0]), "integer"));
                } else if (type.equals("decimal")) {
                    query.setValue(new MCRSQLColumn(field, "" + value.split("[|]")[0], "decimal"));
                } else if (type.equals("boolean")) {
                    query.setValue(new MCRSQLColumn(field, "" + value.split("[|]")[0], "boolean"));
                }
            }
        }

        try {
            c.doUpdate(query.toTypedInsertStatement());
        } catch (Exception e) {
            logger.error("e: " + e);
            e.printStackTrace();
        } finally {
            c.release();
        }
    }

    /**
     * This method creates the table named SQLQueryTable.
     */
    private void createSQLQueryTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            MCRSQLStatement query = new MCRSQLStatement(SQLQueryTable);
            query.addColumn("MCRID VARCHAR(64) NOT NULL");

            Iterator it = queryManager.getQueryFields().keySet().iterator();

            while (it.hasNext()) {
                query.addColumn(addcolumn((Element) queryManager.getQueryFields().get(it.next())));
            }

            if (MCRSQLConnection.doesTableExist(SQLQueryTable)) {
                System.out.println("table exists -> will be dropped");
                dropSQLQueryTable();
            }

            c.doUpdate(query.toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLQueryTable).addColumn("MCRID").toIndexStatement());
        } catch (Exception e) {
            logger.error("Fehler", e);
        } finally {
            c.release();
        }
    }

    /**
     * internal helper method
     * 
     * @param el
     *            jdom-element with definition of searchfield
     * @return formated sql-string for table generation
     */
    private String addcolumn(Element el) {
        StringBuffer sbRet = new StringBuffer().append("`" + el.getAttributeValue("name").toUpperCase() + "` ");
        String type = el.getAttributeValue("type").toLowerCase();

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
            sbRet.append("smallint");
        }

        return sbRet.toString();
    }

    /**
     * This method drops the table named SQLQueryTable.
     */
    private final void dropSQLQueryTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            if (MCRSQLConnection.doesTableExist(SQLQueryTable)) {
                c.doUpdate("drop table " + SQLQueryTable);
            }
        } finally {
            c.release();
        }
    }

    public void updateConfiguration() {
        // TODO Auto-generated method stub
        
    }
    
}
