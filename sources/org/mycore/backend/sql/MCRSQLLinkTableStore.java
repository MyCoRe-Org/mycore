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
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRLinkTableInterface;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class implements the MCRLinkTableInterface as a presistence layer for
 * the store of a table with link connections under the SQL database.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRSQLLinkTableStore implements MCRLinkTableInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRSQLLinkTableStore.class.getName());

    // internal data
    private String tableName;

    private String mytype;

    private int lengthClassID = MCRMetaClassification.MAX_CLASSID_LENGTH;

    private int lengthCategID = MCRMetaClassification.MAX_CATEGID_LENGTH;

    private int lengthObjectID = MCRObjectID.MAX_LENGTH;

    /**
     * The constructor for the class MCRSQLLinkTableStore.
     */
    public MCRSQLLinkTableStore() {
    }

    /**
     * The initializer for the class MCRSQLLinkTableStore. It reads the
     * classification configuration and checks the table names.
     * 
     * @exception throws
     *                if the type is not correct
     */
    public final void init(String type) throws MCRPersistenceException {
        MCRConfiguration config = MCRConfiguration.instance();

        // Check the parameter
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type of the constructor" + " is null or empty.");
        }

        boolean test = false;

        for (int i = 0; i < MCRLinkTableManager.LINK_TABLE_TYPES.length; i++) {
            if (type.equals(MCRLinkTableManager.LINK_TABLE_TYPES[i])) {
                test = true;

                break;
            }
        }

        if (!test) {
            throw new MCRPersistenceException("The type of the constructor" + " is false.");
        }

        mytype = type;

        // set configuration
        tableName = config.getString("MCR.linktable_store_sql_table_" + type, "MCRLINKTABLE");

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
            if (mytype.equals("class")) {
                c.doUpdate(new MCRSQLStatement(tableName).addColumn("MCRFROM VARCHAR(" + Integer.toString(lengthObjectID) + ") NOT NULL").addColumn("MCRTO VARCHAR(" + Integer.toString(lengthClassID + lengthCategID + 2) + ") NOT NULL").addColumn("PRIMARY KEY (MCRFROM,MCRTO)").toCreateTableStatement());
                c.doUpdate(new MCRSQLStatement(tableName).addColumn("MCRFROM").addColumn("MCRTO").toIndexStatement());
            } else {
                c.doUpdate(new MCRSQLStatement(tableName).addColumn("MCRFROM VARCHAR(" + Integer.toString(lengthObjectID) + ") NOT NULL").addColumn("MCRTO VARCHAR(" + Integer.toString(lengthObjectID) + ") NOT NULL").addColumn("PRIMARY KEY (MCRFROM,MCRTO)").toCreateTableStatement());
                c.doUpdate(new MCRSQLStatement(tableName).addColumn("MCRFROM").addColumn("MCRTO").toIndexStatement());
            }
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
     */
    public final void create(String from, String to) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }

        try {
            MCRSQLConnection.justDoUpdate(new MCRSQLStatement(tableName).setValue("MCRFROM", from).setValue("MCRTO", to).toInsertStatement());
        } catch (Exception e) {
            logger.debug("SQL Exception while store link table with message : " + e.getMessage());
        }
    }

    /**
     * The method remove a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     */
    public final void delete(String from) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        MCRSQLConnection.justDoUpdate(new MCRSQLStatement(tableName).setCondition("MCRFROM", from).toDeleteStatement());
    }

    /**
     * The method count the number of references to the 'to' value of the table.
     * 
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     */
    public final int countTo(String to) {
        String sql = new MCRSQLStatement(tableName).setCondition("MCRTO", to).toCountStatement("MCRFROM");
        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        int num = 0;

        try {
            MCRSQLRowReader reader = conn.doQuery(sql);
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
     * The method count the number of references to the 'to' value of the table.
     * 
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     * 
     */
    public final int countTo(String to, String doctype, String restriction) {
        if (((doctype == null) || (doctype.trim().length() == 0)) && ((restriction == null) || (restriction.trim().length() == 0))) {
            return countTo(to);
        }

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

        if (doctype != null) {
            select.append(" AND A.MCRFROM like '%_" + doctype + "_%'");
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
     * @param mcrtoPrefix
     * @return 
     * 
     * the result-map of (key,value)-pairs can be visualized as <br />
     * select count(mcrfrom) as value, mcrto as key from
     * mcrlinkclass|mcrlinkhref 
     * where mcrto like mcrtoPrefix + '%'
     * group by mcrto;
     *  
     */    
	public Map getCountedMapOfMCRTO(String mcrtoPrefix) {
		Map map = new HashMap();
        StringBuffer select = new StringBuffer();

        select.append("SELECT COUNT( A.MCRFROM ) AS NUMBER, A.MCRTO AS KEY FROM ")
        	.append(tableName).append(" A where MCRTO like '")
        	.append(mcrtoPrefix).append("%' group by MCRTO");

        logger.info("STATEMENT:    " + select);

        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        int num = 0;
        String key = "";

        try {
            MCRSQLRowReader reader = conn.doQuery(select.toString());
            while (reader.next()) {
                num = reader.getInt("NUMBER");
                key = reader.getString("KEY");
                map.put(key,new Integer(num));
            }
            reader.close();
        } catch (Exception e) {
            throw new MCRException("SQL counter error", e);
        } finally {
            conn.release();
        }
        return map;
	}	
	
}
