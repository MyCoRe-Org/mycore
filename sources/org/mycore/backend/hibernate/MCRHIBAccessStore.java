/**
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
package org.mycore.backend.hibernate;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.mycore.access.MCRAccessRule;
import org.mycore.access.MCRAccessStore;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.services.fieldquery.MCRParseException;

public class MCRHIBAccessStore extends MCRAccessStore{

    /** the logger */
    static Logger logger = Logger.getLogger(MCRHIBAccessStore.class.getName());
    
    protected static MCRHIBConnection hibconnection = MCRHIBConnection.instance();

    /** name of the sql table containing rule information */
    private String SQLAccessCtrlRule;

    /** name of the sql table containing mapping information */
    private String SQLAccessCtrlMapping;
    
    public MCRHIBAccessStore(){
        MCRConfiguration config = MCRConfiguration.instance();
        SQLAccessCtrlRule = config.getString("MCR.access_store_sql_table_rule",
                "MCRACCESSRULE");
        SQLAccessCtrlMapping = config.getString(
                "MCR.access_store_sql_table_map", "MCRACCESS");
    }

    public String getRuleID(String objID, String ACPool) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        String strRuleID = "";
        try {
            List l = session.createQuery("from MCRACCESS where OBJID = '" + objID + "' and ACPOOL = '" + ACPool + "'").list();
            if (l.size()==1){
                strRuleID = ((MCRACCESS) l.get(0)).getRid();
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
        return strRuleID;
    }

    public MCRAccessRule getRule(String ruleID) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRAccessRule rule = null;
        try {
            List l = session.createQuery("from MCRACCESSRULE where RID = '"+ruleID+"'").list();
            if (l.size()==1){
                //strRule = ((MCRACCESSRULE) l.get(0)).getRule();
                MCRACCESSRULE hibrule = (MCRACCESSRULE) l.get(0);
                try {
                    rule = new MCRAccessRule(ruleID, hibrule.getCreator(), hibrule.getCreationdate(), hibrule.getRule(), hibrule.getDescription());
                } catch(MCRParseException e) {
                    throw new MCRException("Rule "+ruleID+" can't be parsed", e);
                }
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
        return rule;
    }

    
    public void createTables() {
        try {
            // update schema -> first time create table
            Configuration cfg = hibconnection.getConfiguration();
            MCRTableGenerator map = new MCRTableGenerator(SQLAccessCtrlMapping, "org.mycore.backend.hibernate.tables.MCRACCESS", "", 1);
            map.addIDColumn("rid", "RID", new StringType(), 64, "assigned", false);
            map.addColumn("creator", "CREATOR", new StringType(), 64, true, false, false);
            map.addColumn("creationdate", "CREATIONDATE", new TimestampType(), 64, true, false, false);
            map.addColumn("objid", "OBJID", new StringType(), 64, true, false, false);
            map.addColumn("acpool", "ACPOOL", new StringType(), 64, false, false, false);
            cfg.addXML(map.getTableXML());

            map = new MCRTableGenerator(SQLAccessCtrlRule, "org.mycore.backend.hibernate.tables.MCRACCESSRULE", "", 1);
            map.addIDColumn("rid", "RID", new StringType(), 64, "assigned", false);
            map.addColumn("creator", "CREATOR", new StringType(), 64, true, false, false);
            map.addColumn("creationdate", "CREATIONDATE", new TimestampType(), 64, true, false, false);
            map.addColumn("rule", "RULE", new StringType(), 2147483647, false, false, false);
            map.addColumn("description", "DESCRIPTION", new StringType(), 255, false, false, false);
            cfg.addXML(map.getTableXML());
            cfg.createMappings();

            hibconnection.buildSessionFactory(cfg);
            new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);

        }catch(Exception e){
            e.printStackTrace();
            logger.error(e);
        }
    }

}
