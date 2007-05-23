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

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRException;

/**
 * Hibernate implementation for RuleStore, storing access rules
 * 
 * @author Arne Seifert
 * 
 */
public class MCRHIBRuleStore extends MCRRuleStore {
	
	public MCRHIBRuleStore() {
		init();
	}
	
    /**
     * Method creates new rule in database by given rule-object
     * 
     * @param rule
     *            as MCRAccessRule
     */
    public void createRule(MCRAccessRule rule) {

        if (!existsRule(rule.getId())) {
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();
            MCRACCESSRULE hibrule = new MCRACCESSRULE();

            try {
                DateFormat df = new SimpleDateFormat(sqlDateformat);
                hibrule.setCreationdate(Timestamp.valueOf(df.format(rule.getCreationTime())));
                hibrule.setCreator(rule.getCreator());
                hibrule.setRid(rule.getId());
                hibrule.setRule(rule.getRuleString());
                hibrule.setDescription(rule.getDescription());
                session.saveOrUpdate(hibrule);
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                logger.error("createRule: catched error", e);
            } finally {
                 if ( session != null ) session.close();
            }
        } else {
            logger.error("rule with id '" + rule.getId() + "' can't be created, rule still exists.");
        }
    }

    /**
     * Method retrieves the ruleIDs of rules, whose string-representation starts with given data
     * 
     */
    public ArrayList retrieveRuleIDs(String ruleExpression, String description) {
    	ArrayList<String> ret = new ArrayList<String>();
    	Session session = MCRHIBConnection.instance().getSession();
        try{
        	List l = session.createCriteria(MCRACCESSRULE.class)
        		.add(Restrictions.like("rule", ruleExpression))
        		.add(Restrictions.like("description", description)).list();
            for (int i = 0; i < l.size(); i++) {
                ret.add(((MCRACCESSRULE) l.get(i)).getRid());
            }        	
        }catch (Exception e) {
            logger.error("retrieveRuleIDs: catched error", e);
        } finally {
             if ( session != null ) session.close();
        }
    	return ret;
    }
    
    /**
     * Method updates accessrule by given rule. internal: rule will be deleted
     * first and recreated
     */
    public void updateRule(MCRAccessRule rule) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            session.createQuery("delete MCRACCESSRULE where RID = '" + rule.getId() + "'").executeUpdate();
            MCRACCESSRULE hibrule = new MCRACCESSRULE();

            DateFormat df = new SimpleDateFormat(sqlDateformat);
            hibrule.setCreationdate(Timestamp.valueOf(df.format(rule.getCreationTime())));
            hibrule.setCreator(rule.getCreator());
            hibrule.setRid(rule.getId());
            hibrule.setRule(rule.getRuleString());
            hibrule.setDescription(rule.getDescription());
            session.saveOrUpdate(hibrule);
          
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error("updateRule: catched error",e);
        } finally {
             if ( session != null ) session.close();
        }
    }

    /**
     * Method deletes accessrule for given ruleid
     */
    public void deleteRule(String ruleid) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();

        try {
            session.createQuery("delete MCRACCESSRULE where RID = '" + ruleid + "'").executeUpdate();          
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error("catched error",e);
        } finally {
             if ( session != null ) session.close();
        }
    }

    /**
     * Method returns accessrule for given ruleid
     * 
     * @param ruleid
     *            as string
     * @return MCRAccessRule object with database values or null
     */
    public MCRAccessRule retrieveRule(String ruleid) {
    	return getRule(ruleid);
    }

    /**
     * update hibernate configuration and add mappings for acccesstable
     */
    private void init() {
        try {
            new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);
        } catch (Exception e) {
            logger.error("catched error", e);
        }
    }

    /**
     * Method returns MCRAccessRule by given id
     * 
     * @param ruleid
     *            as string
     * @return MCRAccessRule
     */
    public MCRAccessRule getRule(String ruleid) {
        init();

        Session session = MCRHIBConnection.instance().getSession();
        MCRAccessRule rule = null;
        try {
            logger.debug("Getting MCRACCESSRULE");
        	MCRACCESSRULE hibrule = (MCRACCESSRULE)session.createCriteria(MCRACCESSRULE.class).add(Restrictions.eq("rid", ruleid)).uniqueResult();
            logger.debug("Getting MCRACCESSRULE done");

            if (hibrule != null) {
                try {
                    logger.debug("new MCRAccessRule");
                    rule = new MCRAccessRule(ruleid, hibrule.getCreator(), hibrule.getCreationdate(), hibrule.getRule(), hibrule.getDescription());
                    logger.debug("new MCRAccessRule done");
                } catch (Exception e) {
                    throw new MCRException("Rule " + ruleid + " can't be parsed", e);
                }
            }

        } catch (Exception e) {
            logger.error("getRule: catched error: ",e);
        } finally {
             if ( session != null ) session.close();
        }

        return rule;
    }

    public ArrayList retrieveAllIDs() {
        init();

        Session session = MCRHIBConnection.instance().getSession();
        ArrayList<String> ret = new ArrayList<String>();

        try {
            List l = session.createCriteria(MCRACCESSRULE.class).list();
            for (int i = 0; i < l.size(); i++) {
                ret.add(((MCRACCESSRULE) l.get(i)).getRid());
            }
        } catch (Exception e) {
            logger.error("retrieveAllIDs:catched error: ",e);
        } finally {
             if ( session != null ) session.close();
        }

        return ret;
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
            Session session = MCRHIBConnection.instance().getSession();
            List l = session.createCriteria(MCRACCESSRULE.class).add(Restrictions.eq("rid", ruleid)).list();
            session.close();
            if (l.size() == 1) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new MCRException("existsRule: catched error", ex);
        }
    }

	public int getNextFreeRuleID(String prefix) {
    	int ret = 1;
    	Session session = MCRHIBConnection.instance().getSession();
        try{
        	List l = session.createQuery("select max(rid) from MCRACCESSRULE where rid like '"+prefix+"%'").list();
            if (l.size() > 0) {
              	String max = (String) l.get(0);
              	if (max == null){
              		ret = 1;
              	}else {
                  	int lastNumber = Integer.parseInt(max.substring(prefix.length()));
                  	ret = lastNumber + 1;              		
              	}
            } else
            	return 1;
        }catch (Exception e) {
            logger.error("getNextFreeRuleID: catched error", e);
        } finally {
             if ( session != null ) session.close();
        }
        return ret;
	}    
}
