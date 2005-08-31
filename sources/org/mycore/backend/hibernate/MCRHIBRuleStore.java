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
 **/

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.mycore.access.MCRAccessRule;
import org.mycore.access.MCRRuleStore;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRException;

/**
 * Hibernate implementation for RuleStore, storing access rules
 * @author Arne Seifert
 *
 */
public class MCRHIBRuleStore extends MCRRuleStore{

    /**
     * Method creates new rule in database by given rule-object
     * @param rule as MCRAccessRule
     */
    public void createRule(MCRAccessRule rule) {
        init();
        if (! existsRule(rule.getId())){
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();
            MCRACCESSRULE hibrule = new MCRACCESSRULE();
            try{
                DateFormat df = new SimpleDateFormat(sqlDateformat);
                hibrule.setCreationdate(Timestamp.valueOf(df.format(rule.getCreationTime())));
                hibrule.setCreator(rule.getCreator());
                hibrule.setRid(rule.getId());
                hibrule.setRule(rule.getRuleString());
                hibrule.setDescription(rule.getDescription());
                session.saveOrUpdate(hibrule);          
                tx.commit();         
            }catch(Exception e){
                tx.rollback();
                logger.error(e);
            }finally{
                session.close();
            }
        }else{
            logger.error("rule with id '" + rule.getId() + "' can't be created, rule still exists.");
        }
        
    }
    
    
    /**
     * Method checks existance of rule in db
     * @param ruleid id as string
     * @return boolean value
     * @throws MCRException
     */
    private boolean existsRule(String ruleid) throws MCRException{
        init();
        try{
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();
            List l = session.createQuery("from MCRACCESSRULE where RID = '" + ruleid + "'").list();
            tx.commit();
            session.close();
            if (l.size() == 1)
                return true;
            else
                return false;
        } catch (Exception ex) {
            throw new MCRException("Error in access-rule-store.", ex);
        }
    }

    
    /**
     * Method updates accessrule by given rule.
     * internal: rule will be deleted first and recreated
     */
    public void updateRule(MCRAccessRule rule) {
        deleteRule(rule.getId());
        createRule(rule);
    }

    
    /**
     * Method deletes accessrule for given ruleid
     */
    public void deleteRule(String ruleid) {
        init();
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            session.createQuery("delete MCRACCESSRULE where RID = '" + ruleid + "'") 
                 .executeUpdate(); 
            tx.commit(); 
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
            e.printStackTrace();
        }finally{
            session.close();
        }
    }

    
    /**
     * Method returns accessrule for given ruleid
     * @param ruleid as string
     * @return MCRAccessRule object with database values or null
     */
    public MCRAccessRule retrieveRule(String ruleid) {
        init();
        MCRAccessRule rule = null;
        try{
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();
            MCRACCESSRULE hibrule = ((MCRACCESSRULE) session.createQuery("from MCRACCESSRULE where RID = '" + ruleid + "'").list().get(0));
            tx.commit();
            session.close();
            if (hibrule != null){
                rule = new MCRAccessRule(hibrule.getRid(), hibrule.getCreator(), hibrule.getCreationdate(), hibrule.getRule(), hibrule.getDescription());
            }
        }catch(Exception e){
            logger.error(e);
        }
        return rule;
    }

    
    /**
     * update hibernate configuration and add mappings for acccesstable
     */
    private void init() {
        try {
            // update schema -> first time create table
            Configuration cfg = MCRHIBConnection.instance().getConfiguration();
            if (! MCRHIBConnection.instance().containsMapping(ruletablename)){
                MCRTableGenerator map = new MCRTableGenerator(ruletablename, "org.mycore.backend.hibernate.tables.MCRACCESSRULE", "", 1);
                map.addIDColumn("rid", "RID", new StringType(), 64, "assigned", false);
                map.addColumn("creator", "CREATOR", new StringType(), 64, true, false, false);
                map.addColumn("creationdate", "CREATIONDATE", new TimestampType(), 64, true, false, false);
                map.addColumn("rule", "RULE", new StringType(), 2147483647, false, false, false);
                map.addColumn("description", "DESCRIPTION", new StringType(), 255, false, false, false);
                cfg.addXML(map.getTableXML());
                cfg.createMappings();

                MCRHIBConnection.instance().buildSessionFactory(cfg);
                new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);    
            }
        }catch(Exception e){
            logger.error(e);
        }
    }

    /**
     * Method returns MCRAccessRule by given id
     * @param ruleid as string
     * @return MCRAccessRule
     */
    public MCRAccessRule getRule(String ruleid) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRAccessRule rule = null;
        try {
            MCRACCESSRULE hibrule = ((MCRACCESSRULE) session.createQuery("from MCRACCESSRULE where RID = '"+ruleid+"'").list().get(0));
            if (hibrule != null){
                try {
                    rule = new MCRAccessRule(ruleid, hibrule.getCreator(), hibrule.getCreationdate(), hibrule.getRule(), hibrule.getDescription());
                } catch(Exception e) {
                    throw new MCRException("Rule "+ruleid+" can't be parsed", e);
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

}
