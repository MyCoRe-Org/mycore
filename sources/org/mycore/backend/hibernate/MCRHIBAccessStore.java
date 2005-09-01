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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.mycore.access.MCRRuleMapping;
import org.mycore.access.MCRAccessStore;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSPK;

/**
 * Hibernate implementation of acceess store to manage access rights
 * @author Arne Seifert
 *
 */
public class MCRHIBAccessStore extends MCRAccessStore{

    final protected static MCRHIBConnection hibconnection = MCRHIBConnection.instance();
    
    public String getRuleID(String objID, String ACPool) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        String strRuleID = "";
        try {
            List l = session.createQuery("from MCRACCESS where OBJID = '" + objID + "' and ACPOOL = '" + ACPool + "'").list();
            if (l.size()==1){
                strRuleID = ((MCRACCESS) l.get(0)).getKey().getRid();
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


    public void createTables() {
        try {
            // update schema -> first time create table
            Configuration cfg = hibconnection.getConfiguration();
            MCRTableGenerator map = null;
            if (! hibconnection.containsMapping(SQLAccessCtrlMapping)){
                map = new MCRTableGenerator(SQLAccessCtrlMapping, "org.mycore.backend.hibernate.tables.MCRACCESS", "", 3);
                map.addIDColumn("rid", "RID", new StringType(), 64, "assigned", false);
                map.addIDColumn("acpool", "ACPOOL", new StringType(), 64, "assigned", false);
                map.addIDColumn("objid", "OBJID", new StringType(), 64, "assigned", false);
                map.addColumn("creator", "CREATOR", new StringType(), 64, true, false, false);
                map.addColumn("creationdate", "CREATIONDATE", new TimestampType(), 64, true, false, false);
                cfg.addXML(map.getTableXML());
            }
            
            if (! hibconnection.containsMapping(SQLAccessCtrlRule)){
                map = new MCRTableGenerator(SQLAccessCtrlRule, "org.mycore.backend.hibernate.tables.MCRACCESSRULE", "", 1);
                map.addIDColumn("rid", "RID", new StringType(), 64, "assigned", false);
                map.addColumn("creator", "CREATOR", new StringType(), 64, true, false, false);
                map.addColumn("creationdate", "CREATIONDATE", new TimestampType(), 64, true, false, false);
                map.addColumn("rule", "RULE", new StringType(), 2147483647, false, false, false);
                map.addColumn("description", "DESCRIPTION", new StringType(), 255, false, false, false);
                cfg.addXML(map.getTableXML());
                cfg.createMappings();
            }
            hibconnection.buildSessionFactory(cfg);
            new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);
        }catch(Exception e){
            e.printStackTrace();
            logger.error(e);
        }
    }


    /**
     * method creates a new AccessDefinition in db
     * @param MCRAccessData with values
     */
    public void createAccessDefinition(MCRRuleMapping rulemapping) {
        createTables();
        if (! existAccessDefinition(rulemapping.getRuleId(), rulemapping.getPool(), rulemapping.getObjId())){
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();
            MCRACCESS accdef = new MCRACCESS();
            try{
                DateFormat df = new SimpleDateFormat(sqlDateformat);
                accdef.setKey(new MCRACCESSPK(rulemapping.getRuleId(), rulemapping.getPool(), rulemapping.getObjId()));
                accdef.setCreator(rulemapping.getCreator());
                accdef.setCreationdate(Timestamp.valueOf(df.format(rulemapping.getCreationdate())));
                session.saveOrUpdate(accdef);          
                tx.commit();         
            }catch(Exception e){
                tx.rollback();
                logger.error(e);
            }finally{
                session.close();
            }
        }
    }
    
    /**
     * internal helper method to check existance of object
     * @param ruleid
     * @param pool
     * @param objid
     * @return boolean value
     */
    private boolean existAccessDefinition(String ruleid, String pool, String objid){
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            MCRACCESSPK key = new MCRACCESSPK(ruleid,pool,objid);
            List l = session.createCriteria(MCRACCESS.class).add(Restrictions.eq("key", key)).list();
            tx.commit();
            if (l.size()==1)
                return true;
            else
                return false;
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
            return true;
        }finally{
            session.close();
        }
    }


    /**
     * delete given definition in db
     * @param accessdata MCRAccessData containing key;
     */
    public void deleteAccessDefinition(MCRRuleMapping rulemapping) {
        createTables();
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            session.createQuery("delete MCRACCESS " +
                    "where RID = '" + rulemapping.getRuleId() + "'" +
                    " AND ACPOOL = '" + rulemapping.getPool() + "'" +
                    " AND OBJID = '" + rulemapping.getObjId() + "'") 
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
     * update AccessDefinition in db for given MCRAccessData
     */
    public void updateAccessDefinition(MCRRuleMapping rulemapping) {
        deleteAccessDefinition(rulemapping);
        createAccessDefinition(rulemapping);
    }

    /**
     * method returns AccessDefinition for given key values
     * @param  ruleid name of rule
     * @param  pool name of accesspool
     * @param  objid objectid of MCRObject
     * @return MCRAccessData
     */
    public MCRRuleMapping getAccessDefinition(String ruleid, String pool, String objid) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        MCRRuleMapping rulemapping = new MCRRuleMapping();
        try{
            MCRACCESS data = ((MCRACCESS) session.createCriteria(MCRACCESS.class).add(Restrictions.eq("key", new MCRACCESSPK(ruleid, pool, objid))).list().get(0));
            tx.commit();
            if (data != null){
                rulemapping.setCreationdate(data.getCreationdate());
                rulemapping.setCreator(data.getCreator());
                rulemapping.setObjId(data.getKey().getObjid());
                rulemapping.setPool(data.getKey().getAcpool());
                rulemapping.setRuleId(data.getKey().getRid());
            }
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
        }finally{
            session.close();
        }
        return rulemapping;
    }

}
