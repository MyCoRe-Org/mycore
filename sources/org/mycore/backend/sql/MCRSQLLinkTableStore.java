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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.common.MCRLinkTableInterface;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class implements the MCRLinkTableInterface as a presistence layer for
 * the store of a table with link connections under the SQL database.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @deprecated
 */
public class MCRSQLLinkTableStore implements MCRLinkTableInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRSQLLinkTableStore.class.getName());

    // internal data
    private String tableName;

    private int lengthObjectID = MCRObjectID.MAX_LENGTH;

    /**
     * The constructor for the class MCRSQLLinkTableStore.
     */
    public MCRSQLLinkTableStore() {
        MCRConfiguration config = MCRConfiguration.instance();
        // set configuration
        tableName = config.getString("MCR.linktable_store_sql_table_href","MCRLINKTABLE");

        if (!MCRSQLConnection.doesTableExist(tableName)) {
            logger.info("Create table " + tableName);
            createLinkTable();
            logger.info("Done.");
        }
    }

    /**
     * The method drop the table.
     */
    public final void dropTables() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate("DROP TABLE " + tableName);
        } finally {
            c.release();
        }
    }

    /**
     * The method create a table for classification.
     */
    private final void createLinkTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
               c.doUpdate(new MCRSQLStatement(tableName).addColumn("MCRFROM VARCHAR(" + Integer.toString(lengthObjectID) + ") NOT NULL").addColumn("MCRTO VARCHAR(" + Integer.toString(lengthObjectID) + ") NOT NULL").addColumn("MCRTYPE VARCHAR(64) NOT NULL").addColumn("MCRATTR VARCHAR(64)").addColumn("PRIMARY KEY (MCRFROM,MCRTO,MCRTYPE)").toCreateTableStatement());
                c.doUpdate(new MCRSQLStatement(tableName).addColumn("MCRFROM").addColumn("MCRTO").addColumn("MCRTYPE").toIndexStatement());
        } finally {
            c.release();
        }
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID TO
     * @param type
     *            a string with the link ID MCRTYPE
      * @param attr
     *            a string with the link ID MCRATTR
    */
    public final void create(String from, String to, String type, String attr) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }
        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }
        if ((attr == null) || ((attr = attr.trim()).length() == 0)) {
            throw new MCRPersistenceException("The attr value is null or empty.");
        }

        try {
            MCRSQLConnection.justDoUpdate(new MCRSQLStatement(tableName).setValue("MCRFROM", from).setValue("MCRTO", to).setValue("MCRTYPE", type).setValue("MCRATTR", attr).toInsertStatement());
        } catch (Exception e) {
            logger.debug("SQL Exception while store link table with message : " + e.getMessage());
        }
    }

    /**
     * The method remove a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     */
    public final void delete(String from, String to, String type) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }

        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }

        MCRSQLConnection.justDoUpdate(new MCRSQLStatement(tableName).setCondition("MCRFROM", from).setCondition("MCRTO", to).setCondition("MCRTYPE", type).toDeleteStatement());
    }

    /**
     * The method count the number of references with '%from%' and 'to' and
     * optional 'type' and optional 'restriction%' values of the table.
     * 
     * @param fromtype
     *            a substing in the from ID as String, it can be null
     * @param to
     *            the object ID as String, which is referenced
     * @param type
     *            the refernce type, it can be null
     * @param restriction
     *            a first part of the to ID as String, it can be null
     * @return the number of references
     */
    public final int countTo(String fromtype, String to, String type, String restriction) {
        StringBuffer select = new StringBuffer();

        select.append("SELECT COUNT( DISTINCT A.MCRFROM ) AS NUMBER FROM ");
        select.append(tableName).append(" A ");

        if (restriction != null) {
            select.append(", ");
            select.append(tableName).append(" B ");
        }

        select.append(" WHERE ");
        select.append("A.MCRTO like '" + to + "'");

        if (restriction != null) {
            select.append(" AND ");
            select.append("B.MCRTO like '" + restriction + "%'");
            select.append(" AND ( A.MCRFROM = B.MCRFROM )");
        }

        if (type != null) {
            select.append(" AND A.MCRFROM like '%_" + type + "_%'");
        }

        logger.info("STATEMENT:    " + select);

        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        int num = 0;

        try {
            MCRSQLRowReader reader = conn.doQuery(select.toString());
            if (reader.next()) {
                num = reader.getInt("NUMBER");
            }
            reader.close();
            return num;
        } catch (Exception e) {
            throw new MCRException("SQL counter error", e);
        } finally {
            conn.release();
        }
    }

    /**
     * The method returns a Map of all counted distinct references
     * 
     * @param mcrtoPrefix
     * @return
     * 
     * the result-map of (key,value)-pairs can be visualized as <br />
     * select count(mcrfrom) as value, mcrto as key from
     * mcrlinkclass|mcrlinkhref where mcrto like mcrtoPrefix + '%' group by
     * mcrto;
     * 
     */
    public Map getCountedMapOfMCRTO(String mcrtoPrefix) {
        Map map = new HashMap();
        StringBuffer select = new StringBuffer();

        select.append("SELECT COUNT( A.MCRFROM ) AS NUMBER, A.MCRTO AS MYKEY FROM ").append(tableName).append(" A where MCRTO like '").append(mcrtoPrefix).append("%' group by MCRTO");

        logger.info("STATEMENT:    " + select);

        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        int num = 0;
        String key = "";

        try {
            MCRSQLRowReader reader = conn.doQuery(select.toString());
            while (reader.next()) {
                num = reader.getInt("NUMBER");
                key = reader.getString("MYKEY");
                map.put(key, new Integer(num));
            }
            reader.close();
        } catch (Exception e) {
            throw new MCRException("SQL counter error", e);
        } finally {
            conn.release();
        }
        return map;
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     * 
     * @param to
     *            Destination-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            child, classid, parent, reference and derivate.
     * @return List of Strings (Source-IDs)
     */
    public List getSourcesOf(String to, String type) {
        StringBuffer select = new StringBuffer();
        select.append("SELECT A.MCRFROM FROM TABLE ").append(tableName).append(" A WHERE A.MCRTO='").append(to).append("'");
        if ((type != null) && (type.trim().length() != 0)) {
        	select.append(" AND A.MCRTYPE='").append(type).append("'");
        }
        logger.debug("STATEMENT: " + select);
        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        List returns = new LinkedList<String>();

        try {
            MCRSQLRowReader reader = conn.doQuery(select.toString());
            while (reader.next()) {
                returns.add(reader.getString(1));
            }
            reader.close();
        } catch (Exception e) {
            throw new MCRException("SQL counter error", e);
        } finally {
            conn.release();
        }
        return returns;
    }    

    /**
     * Returns a List of all link destination of <code>source</code> and a
     * special <code>type</code>
     * 
     * @param source
     *            Source-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            child, classid, parent, reference and derivate.
     * @return List of Strings (Destination-IDs)
     */
    public List getDestinationsOf(String source, String type) {
        StringBuffer select = new StringBuffer();
        select.append("SELECT A.MCRTO FROM TABLE ").append(tableName).append(" A WHERE A.MCRFROM='").append(source).append("'");
        if ((type != null) && (type.trim().length() != 0)) {
            select.append(" AND A.MCRTYPE='").append(type).append("'");
        }
        logger.debug("STATEMENT: " + select);
        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        List returns = new LinkedList<String>();

        try {
            MCRSQLRowReader reader = conn.doQuery(select.toString());
            while (reader.next()) {
                returns.add(reader.getString(1));
            }
            reader.close();
        } catch (Exception e) {
            throw new MCRException("SQL counter error", e);
        } finally {
            conn.release();
        }
        return returns;
    }    

    private static final String getSQLArray(String[] values) {
        StringBuffer returns = new StringBuffer();
        returns.append("( ");
        for (int i = 0; i < values.length; i++) {
            returns.append('\'').append(values[i]).append('\'');
            if (i < (values.length - 1)) {
                returns.append(", ");
            }
        }
        returns.append(" )");
        return returns.toString();
    }

}
