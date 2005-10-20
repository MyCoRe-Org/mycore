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

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.mycore.access.MCRAccessStore;
import org.mycore.access.MCRRuleMapping;
import org.mycore.common.MCRException;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.userstore_class_name</code>
 * from mycore.properties.
 * 
 * @author Arne Seifert
 * @version $Revision$ $Date$
 */
public class MCRSQLAccessStore extends MCRAccessStore {
    public void createTables() {
        // create tables
        if (!AccessPools.equals("")) {
            if (!MCRSQLConnection.doesTableExist(SQLAccessCtrlRule)) {
                logger.info("Create table " + SQLAccessCtrlRule);
                createAccessRuleTable();
                logger.info("Done.");
            }

            if (!MCRSQLConnection.doesTableExist(SQLAccessCtrlMapping)) {
                logger.info("Create table " + SQLAccessCtrlMapping);
                createAccessMappingTable();
                logger.info("Done.");
            }
        } else {
            logger.info("Access Control System disabled");
        }
    }

    /**
     * This method creates the table named SQLRule.
     */
    private void createAccessRuleTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlRule).addColumn("RID VARCHAR(64) NOT NULL").addColumn("CREATOR VARCHAR(64) NOT NULL").addColumn("CREATIONDATE TIMESTAMP").addColumn("RULE Text").addColumn("DESCRIPTION VARCHAR(255)").addColumn("PRIMARY KEY (RID)").toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlRule).addColumn("RID").toIndexStatement());
        } finally {
            c.release();
        }
    }

    /**
     * This method creates the table named SQLRuleMapping.
     */
    private void createAccessMappingTable() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlMapping).addColumn("RID VARCHAR(64) NOT NULL").addColumn("ACPOOL VARCHAR(64)").addColumn("OBJID VARCHAR(64) NOT NULL").addColumn("CREATOR VARCHAR(64) NOT NULL").addColumn("CREATIONDATE TIMESTAMP").addColumn("PRIMARY KEY (RID,ACPOOL,OBJID)").toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlMapping).addColumn("RID").addColumn("ACPOOL").addColumn("OBJID").toIndexStatement());
        } finally {
            c.release();
        }
    }

    public String getRuleID(String objID, String ACPool) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        String strRuleID = "";

        try {
            String select = "SELECT RID FROM " + SQLAccessCtrlMapping + " WHERE OBJID = '" + objID + "' AND ACPOOL = '" + ACPool + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (rs.next()) {
                strRuleID = rs.getString(1);
            }
        } catch (Exception ex) {
            throw new MCRException("No rule defined for object " + objID, ex);
        } finally {
            c.release();
        }

        return strRuleID;
    }

    /**
     * method creates new access definition in db
     * 
     * @param MCRAccessDefinition
     *            data-object
     */
    public void createAccessDefinition(MCRRuleMapping rulemapping) {
        if (!existAccessDefinition(rulemapping.getRuleId(), rulemapping.getPool(), rulemapping.getObjId())) {
            DateFormat df = new SimpleDateFormat(sqlDateformat);
            MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
            MCRSQLStatement query = new MCRSQLStatement(SQLAccessCtrlMapping);

            query.setValue(new MCRSQLColumn("RID", rulemapping.getRuleId(), "string"));
            query.setValue(new MCRSQLColumn("ACPOOL", rulemapping.getPool(), "string"));
            query.setValue(new MCRSQLColumn("OBJID", rulemapping.getObjId(), "string"));
            query.setValue(new MCRSQLColumn("CREATOR", rulemapping.getCreator(), "string"));
            query.setValue(new MCRSQLColumn("CREATIONDATE", df.format(rulemapping.getCreationdate()), "date"));

            try {
                c.doUpdate(query.toTypedInsertStatement());
            } catch (Exception e) {
                logger.error(e);
            } finally {
                c.release();
            }
        }
    }

    /**
     * internal helper method to check existance of object
     * 
     * @param ruleid
     * @param pool
     * @param objid
     * @return boolean value
     */
    private boolean existAccessDefinition(String ruleid, String pool, String objid) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();

        try {
            String select = "SELECT RID FROM " + SQLAccessCtrlMapping + " WHERE OBJID = '" + objid + "' AND ACPOOL = '" + pool + "' AND RID = '" + ruleid + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error(e);

            return true;
        } finally {
            c.release();
        }
    }

    public void deleteAccessDefinition(MCRRuleMapping rulemapping) {
        try {
            MCRSQLConnection.justDoUpdate("DELETE FROM " + SQLAccessCtrlMapping + " WHERE ACPOOL = '" + rulemapping.getPool() + "'" + " AND OBJID = '" + rulemapping.getObjId() + "'");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void updateAccessDefinition(MCRRuleMapping rulemapping) {
        deleteAccessDefinition(rulemapping);
        createAccessDefinition(rulemapping);
    }

    public MCRRuleMapping getAccessDefinition(String ruleid, String pool, String objid) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRRuleMapping rulemapping = new MCRRuleMapping();

        try {
            String select = "SELECT RID FROM " + SQLAccessCtrlMapping + " WHERE OBJID = '" + objid + "' AND ACPOOL = '" + pool + "' AND RID = '" + ruleid + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (rs.next()) {
                rulemapping.setCreationdate(rs.getDate(5));
                rulemapping.setCreator(rs.getString(4));
                rulemapping.setObjId(rs.getString(3));
                rulemapping.setPool(rs.getString(2));
                rulemapping.setRuleId(rs.getString(1));
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            c.release();
        }

        return rulemapping;
    }

    public ArrayList getMappedObjectId(String pool) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        ArrayList ret = new ArrayList();
        
        try {
            String select = "SELECT OBJID FROM " + SQLAccessCtrlMapping + " WHERE ACPOOL = '" + pool + "'";
            logger.debug(" SQL: " + select);
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);
            while (rs.next()) {
                ret.add(rs.getString(1));
            }
            rs.close();
        } catch(Exception e) {
            logger.error(e);
        } finally {
            c.release();            
        }
        
        return ret;
    }

    public ArrayList getPoolsForObject(String objid) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        ArrayList ret = new ArrayList();
        
        try {
            String select = "SELECT ACPOOL FROM " + SQLAccessCtrlMapping + " WHERE OBJID = '" + objid + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (rs.next()) {
                ret.add(rs.getString(1));
            }
        } catch(Exception e) {
            logger.error(e);
        } finally {
            c.release();            
        }
        
        return ret;
    }

    public ArrayList getDatabasePools() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        ArrayList ret = new ArrayList();
        
        try {
            String select = "SELECT distinct(ACPOOL) FROM " + SQLAccessCtrlMapping ;
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (rs.next()) {
                ret.add(rs.getString(1));
            }
        } catch(Exception e) {
            logger.error(e);
        } finally {
            c.release();            
        }
        
        return ret;
    }
}
