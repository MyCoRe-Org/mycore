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
package org.mycore.access;

import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.backend.sql.MCRSQLConnectionPool;
import org.mycore.backend.sql.MCRSQLStatement;
import org.mycore.backend.sql.MCRSQLUserStore;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.parsers.bool.MCRParseException;

//import org.mycore.access.MCRAccessCtrlStore;

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
class MCRSQLAccessStore extends MCRAccessStore {

    /** the logger */
    static Logger logger = Logger.getLogger(MCRSQLUserStore.class.getName());

    /** name of the sql table containing rule information */
    private String SQLAccessCtrlRule;

    /** name of the sql table containing mapping information */
    private String SQLAccessCtrlMapping;

    /** access pool names, comma separated* */
    private String AccessPools;

    /**
     * constructor with initialisation of variables
     */
    public MCRSQLAccessStore() {
        MCRConfiguration config = MCRConfiguration.instance();
        SQLAccessCtrlRule = config.getString("MCR.access_store_sql_table_rule",
                "MCRACCESSRULE");
        SQLAccessCtrlMapping = config.getString(
                "MCR.access_store_sql_table_map", "MCRACCESS");
        AccessPools = config.getString("MCR.AccessPools", "");
    }

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
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlRule).addColumn(
                    "RID VARCHAR(64) NOT NULL").addColumn(
                    "CREATOR VARCHAR(64) NOT NULL").addColumn(
                    "CREATIONDATE TIMESTAMP").addColumn("RULE Text").addColumn(
                    "DESCRIPTION VARCHAR(255)").toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlRule).addColumn("RID")
                    .toIndexStatement());
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
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlMapping).addColumn(
                    "RID VARCHAR(64) NOT NULL").addColumn(
                    "CREATOR VARCHAR(64) NOT NULL").addColumn(
                    "CREATIONDATE TIMESTAMP").addColumn(
                    "OBJID VARCHAR(64) NOT NULL").addColumn(
                    "ACPOOL VARCHAR(64)").toCreateTableStatement());
            c.doUpdate(new MCRSQLStatement(SQLAccessCtrlMapping).addColumn(
                    "RID").toIndexStatement());
        } finally {
            c.release();
        }
    }

    public MCRAccessRule getRule(String ruleID) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRAccessRule rule;
        try {
            String select = "SELECT CREATOR,CREATIONDATE,DESCRIPTION FROM " + SQLAccessCtrlRule
                    + " WHERE RID = '" + ruleID + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);
            if (rs.next()) {
                try {
                    rule = new MCRAccessRule(ruleID, rs.getString(1), rs.getTimestamp(2), rs.getString(3), "");
                } catch(MCRParseException e) {
                    throw new MCRException("Rule "+ruleID+" can't be parsed", e);
                }
            } else {
                throw new Exception("No such row: RID="+ruleID);
            }
        } catch (Exception ex) {
            throw new MCRException("RuleID " + ruleID + " not found"
                    + ex.getMessage(), ex);
        } finally {
            c.release();
        }
        return rule;
    }

    public String getRuleID(String objID, String ACPool) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        String strRuleID = "";

        try {
            String select = "SELECT RID FROM " + SQLAccessCtrlMapping
                    + " WHERE OBJID = '" + objID + "' AND ACPOOL = '" + ACPool
                    + "'";
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

}
