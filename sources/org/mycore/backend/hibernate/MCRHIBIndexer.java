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

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.type.StringType;
import org.jdom.Element;
import org.mycore.backend.query.MCRQueryIndexer;
import org.mycore.backend.hibernate.MCRTableGenerator;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRHIBIndexer extends MCRQueryIndexer {

    protected static MCRHIBMapping genTable = new MCRHIBMapping();
    protected static MCRHIBConnection hibconnection = MCRHIBConnection.instance();

    /**
     * method loads all searchfield values into database after clearing old values.
     * needs to be done after changes in the fielddefinition 
     */
    public void initialLoad() {
        StringTokenizer tokenizer = new StringTokenizer(querytypes,",");
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            session.createQuery("delete from MCRQuery").executeUpdate(); 
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
        }finally{
            session.close();
        }
        
        while ( tokenizer.hasMoreTokens() )
            queryManager.loadType(tokenizer.nextToken());
    }

    
    /**
     * method updates all search values for a single object
     * @param objectid identifier for single object
     */
    public void updateObject(MCRBase object) {
        try{
            queryManager.create(object);
        }catch(Exception e){
            logger.error(e);
        }

    }

    /**
     * method deletes all values for given objectid
     * @param objectid identifier for single object
     */
    public void deleteObject(MCRObjectID objectid) {
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try{
            session.createQuery("delete MCRQuery where MCRID =\'" + objectid.getId() + "\'") 
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
     * internal helper method
     * @param mcrid identifier for object
     * @param values list of values to be indexed vor search. list contains values for each field
     */
    public final void insertInQuery(String mcrid, List values){
        
        MCRHIBQuery query = new MCRHIBQuery();

        query.setValue("setmcrid", mcrid);
        query.setValue("setmcrtype",new MCRObjectID(mcrid).getTypeId());
        
        Iterator it = queryManager.getQueryFields().keySet().iterator();
        for(int i=0; i< values.size(); i++){
            Element el = queryManager.getField((String) it.next());
            try{
                if(values.get(i) != null){
                   query.setValue("set" + el.getAttributeValue("name"), (String) values.get(i) );                    
                }
            }catch(Exception e){
                logger.error(e);
            }
        }
        
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try {
            session.saveOrUpdate( query.getQueryObject());
            tx.commit();   
        }catch(Exception e){
            logger.error(e);
            tx.rollback();
        } finally {
            session.close();
        }

    }
    
    /**
     * method updates the hibernate configuration and introduces the new table for searching
     */
    public void updateConfiguration() {
        try {
            // update schema -> first time create table
            Configuration cfg = hibconnection.getConfiguration();
            MCRTableGenerator map = new MCRTableGenerator(SQLQueryTable, "org.mycore.backend.query.MCRQuery", "", 1);
            
            Iterator it = queryManager.getQueryFields().keySet().iterator();
            map.addIDColumn("mcrid", "MCRID", new StringType(), 64, "assigned", false);
            map.addColumn("mcrtype", "MCRTYPE", new StringType(), 64, true, false, false);
            
            while (it.hasNext()){
                Element el = (Element) queryManager.getQueryFields().get((String) it.next());              
                map.addColumn(el.getAttributeValue("name"),
                        el.getAttributeValue("name"),
                        hibconnection.getHibType(el.getAttributeValue("type")),
                        2147483647, false, false, false);
            }
            
            cfg.addXML(map.getTableXML());
            cfg.createMappings();
            hibconnection.buildSessionFactory(cfg);

            new SchemaUpdate(MCRHIBConnection.instance().getConfiguration()).execute(true, true);
            
        }catch(Exception e){
            logger.error(e);
        }
    }

}
