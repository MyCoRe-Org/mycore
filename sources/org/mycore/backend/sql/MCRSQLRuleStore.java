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

import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.common.MCRException;

/**
 * SQL implementation for RuleStore, storing access rules
 * 
 * @author Arne Seifert
 * 
 */
public class MCRSQLRuleStore extends MCRRuleStore {
    /**
     * Method creates new rule in database by given rule-object
     * 
     * @param rule
     *            as MCRAccessRule
     */
    public void createRule(MCRAccessRule rule) {
        if (!existsRule(rule.getId())) {
            MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
            MCRSQLStatement query = new MCRSQLStatement(ruletablename);
            query.setValue(new MCRSQLColumn("RID", rule.getId(), "string"));
            query.setValue(new MCRSQLColumn("CREATOR", rule.getCreator(), "string"));

            DateFormat df = new SimpleDateFormat(sqlDateformat);
            query.setValue(new MCRSQLColumn("CREATIONDATE", df.format(rule.getCreationTime()), "date"));
            query.setValue(new MCRSQLColumn("RULE", rule.getRuleString(), "string"));
            query.setValue(new MCRSQLColumn("DESCRIPTION", rule.getDescription(), "string"));

            try {
                c.doUpdate(query.toTypedInsertStatement());
            } catch (Exception e) {
                logger.error(e);
            } finally {
                c.release();
            }
        } else {
            logger.error("rule with id '" + rule.getId() + "' can't be created, rule still exists.");
        }
    }

    /**
     * Method checks existance of rule in db
     * 
     * @param ruleid
     *            id as string
     * @return boolean value
     * @throws MCRException
     */
    public boolean existsRule(String ruleid) throws MCRException {
        try {
            return MCRSQLConnection.justCheckExists(new MCRSQLStatement(ruletablename).setCondition("RID", ruleid).toRowSelector());
        } catch (Exception ex) {
            throw new MCRException("Error in access-rule-store.", ex);
        }
    }

    /**
     * method updates rule by given value. If rule still exists it will be
     * deleted and inserted
     * 
     * @param rule
     *            rule object filled with values to be saved
     */
    public void updateRule(MCRAccessRule rule) {
        deleteRule(rule.getId());
        createRule(rule);
    }

    /**
     * method deletes rule with given id
     */
    public void deleteRule(String ruleid) {
        try {
            MCRSQLConnection.justDoUpdate("DELETE FROM " + ruletablename + " WHERE RID = '" + ruleid + "'");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * method returns rule by given id
     * 
     * @param ruleid
     *            String with ruleid
     * @return MCRAccessRule object
     */
    public MCRAccessRule retrieveRule(String ruleid) {
        MCRAccessRule rule = null;
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            String select = "SELECT * FROM " + ruletablename + " WHERE RID = '" + ruleid + "'";
            Statement statement = connection.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (!rs.next()) {
                String msg = "There is no rule with ID = " + ruleid;
                throw new MCRException(msg);
            }

            DateFormat df = new SimpleDateFormat(sqlDateformat);

            // df.parse(rs.getString(3));
            rule = new MCRAccessRule(rs.getString(1), rs.getString(2), df.parse(rs.getString(3)), rs.getString(4), rs.getString(5));
            rs.close();
        } catch (Exception e) {
            logger.error(e);
        } finally {
            connection.release();
        }

        return rule;
    }

    /**
     * Method returns MCRAccessRule by given id
     * 
     * @param ruleid
     *            as string
     * @return MCRAccessRule
     */
    public MCRAccessRule getRule(String ruleid) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRAccessRule rule = null;

        try {
            String select = "SELECT CREATOR, CREATIONDATE, RULE, DESCRIPTION FROM " + ruletablename + " WHERE RID = '" + ruleid + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (rs.next()) {
                try {
                    return new MCRAccessRule(ruleid, rs.getString(1), rs.getTimestamp(2), rs.getString(3), rs.getString(4));
                } catch (Exception e) {
                    throw new MCRException("Rule " + ruleid + " can't be parsed", e);
                }
            }
            return rule;
        } catch (Exception e) {
            logger.error(e);
        } finally {
            c.release();
        }

        return rule;
    }

    public ArrayList retrieveAllIDs() {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        ArrayList ret = new ArrayList();

        try {
            String select = "SELECT RID FROM " + ruletablename;
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            while (rs.next()) {
                ret.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            c.release();
        }

        return ret;
    }

	public ArrayList retrieveRuleIDs(String ruleExpression, String description) {
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        ArrayList ret = new ArrayList();

        try {
            String select = "SELECT RID FROM " + ruletablename + " WHERE " + 
            	"RULE LIKE '" + ruleExpression + "' OR" +
            	"DESCRIPTION LIKE '" + description + "'";
            Statement statement = c.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            while (rs.next()) {
                ret.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            c.release();
        }

        return ret;
	}

	public int getNextFreeRuleID(String prefix) {
    	String query = new StringBuffer().append("SELECT MAX(RID) FROM MCRACCESSRULE WHERE (RID LIKE \'").append(prefix).append("%\' )").toString();
    	try {
            return Integer.parseInt(MCRSQLConnection.justGetSingleValue(query).substring(prefix.length())) + 1;
        } catch (Exception e) {
        	logger.debug("catched error", e);
        }
        return 1;    	
	}


}
