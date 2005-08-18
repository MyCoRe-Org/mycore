/**
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
 *
 **/

package org.mycore.backend.hibernate;

import java.util.*;

import org.apache.log4j.Logger;

import org.hibernate.*;
import org.hibernate.criterion.Restrictions;

import org.mycore.backend.hibernate.tables.MCRXMLTABLE;
import org.mycore.backend.hibernate.tables.MCRXMLTABLEPK;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;

/**
 * This class implements the MCRXMLInterface.
 **/
public class MCRHIBXMLStore implements MCRXMLTableInterface
{
    // logger
    static Logger logger=Logger.getLogger(MCRHIBXMLStore.class.getName());

    private String type;

    /**
     * The constructor for the class MCRHIBXMLStore.
     **/
    public MCRHIBXMLStore(){
    }

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /**
     * The initializer for the class MCRHIBXMLStore. It reads
     * the configuration and checks the table names and create the
     * table if they does'n exist..
     *
     * @param type the type String of the MCRObjectID
     * @exception MCRPersistenceException if the type is not correct
     **/
    public final void init(String type) throws MCRPersistenceException{
        MCRConfiguration config = MCRConfiguration.instance();
        // Check the parameter
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type of the constructor"+ " is null or empty.");
        }
        boolean test = config.getBoolean("MCR.type_"+type,false);
        if (!test) {
            throw new MCRPersistenceException("The type "+type+" of the constructor"+
            " is false.");
        }
        this.type = type;
    }

    /**
     * The method create a new item in the datastore.
     *
     * @param mcrid a MCRObjectID
     * @param xml a byte array with the XML file
     * @param version the version of the XML Blob as integer
     * @exception MCRPersistenceException the method arguments are not correct
     **/
    public synchronized final void create(MCRObjectID mcrid, byte[] xml, int version) throws MCRPersistenceException{
        if (mcrid == null) {
            throw new MCRPersistenceException("The MCRObjectID is null."); }
        if ((xml == null) || (xml.length ==0)) {
            throw new MCRPersistenceException("The XML array is null or empty."); }
        
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        try{
            MCRXMLTABLE tab = new MCRXMLTABLE();
            tab.setId(mcrid.getId());
            tab.setVersion(version);
            tab.setType(this.type);
            tab.setXmlByteArray(xml);
            
            System.out.println("Inserting "+mcrid.getId()+"/"+version+"/"+this.type+" into database");
            
            session.saveOrUpdate(tab);
            
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
        }finally{
            session.close();
        }
        
    }

    /**
     * The method remove a item for the MCRObjectID from the datastore.
     *
     * @param mcrid a MCRObjectID
     * @param version the version of the XML Blob as integer
     * @exception MCRPersistenceException the method argument is not correct
     **/
    public synchronized final void delete( MCRObjectID mcrid, int version ) throws MCRPersistenceException{
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        try{
            MCRXMLTABLE tab = new MCRXMLTABLE(mcrid.getId(), version, this.type, null);
            session.delete(tab); //"from MCRXMLTABLE where id='"+mcrid+"' and version='"+version+"' and type='"+type+"'");
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
        }finally{
            session.close();
        }
        
        
    }

    /**
     * The method update an item in the datastore.
     *
     * @param mcrid a MCRObjectID
     * @param xml a byte array with the XML file
     * @param version the version of the XML Blob as integer
     * @exception MCRPersistenceException the method arguments are not correct
     **/
    public synchronized final void update(MCRObjectID mcrid, byte[] xml, int version) throws MCRPersistenceException{
        create(mcrid, xml, version);
    }

    /**
     * The method retrieve a dataset for the given MCRObjectID and returns
     * the corresponding XML file as byte array.
     *
     * @param mcrid a MCRObjectID
     * @param version the version of the XML Blob as integer
     * @return the XML-File as byte array or null
     * @exception MCRPersistenceException the method arguments are not correct
     **/
    public final byte[] retrieve(MCRObjectID mcrid, int version) throws MCRPersistenceException{
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = new LinkedList();
        try{
            MCRXMLTABLEPK pk = new MCRXMLTABLEPK(mcrid.getId(), version, this.type);
            l = session.createCriteria(MCRXMLTABLE.class).add(Restrictions.eq("key", pk)).list();
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
	    return null;
        }finally{
            session.close();
        }

        if (l.size() > 0)
            return ((MCRXMLTABLE)l.get(0)).getXmlByteArray();
        return null;
    }

    /**
      * This method returns the next free ID number for a given
      * MCRObjectID base. This method ensures that any invocation
      * returns a new, exclusive ID by remembering the highest ID
      * ever returned and comparing it with the highest ID stored
      * in the related index class.
      *
      * @param project   the project ID part of the MCRObjectID base
      * @param type      the type ID part of the MCRObjectID base
      *
      * @exception MCRPersistenceException if a persistence problem is occured
      *
      * @return the next free ID number as a String
      **/
    public final int getNextFreeIdInt( String project, String type )
    throws MCRPersistenceException
    {
        /*Session session = getSession();
         Transaction tx = session.beginTransaction();
         List l = session.createQuery("from MCRXMLTABLE where MCRID like '"+project+"_"+type+"%'").list();
         String max = "";
         int t;
         for(t=0;t<l.size();t++) {
         MCRXMLTABLE tab = ((MCRXMLTABLE)l.get(t));
         if(tab.getId().compareTo(max) > 0)
         max = tab.getId();
         }
         tx.commit();
         session.close();
         return new MCRObjectID(max).getNumberAsInteger() + 1;*/
        
        return (int)MCRHIBConnection.instance().getID();
    }

    /**
     * This method check that the MCRObjectID exist in this store.
     *
     * @param mcrid a MCRObjectID
     * @param version the version of the XML Blob as integer
     * @return true if the MCRObjectID exist, else return false
     **/
    public final boolean exist(MCRObjectID mcrid, int version)
    {
        return retrieve(mcrid, version) != null;
    }

    /**
     * The method return a Array list with all stored MCRObjectID's of the
     * XML table of a MCRObjectID type.
     *
     * @param type a MCRObjectID type string
     * @return a ArrayList of MCRObjectID's
     **/
    public ArrayList retrieveAllIDs(String type){
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = new LinkedList();
        ArrayList a = null;
        try{
            l = session.createQuery("from MCRXMLTABLE where MCRTYPE = '"+type+"'").list();
            a = new ArrayList(l.size());
            Set s = new HashSet();
            int t;
            for(t=0;t<l.size();t++) {
                MCRXMLTABLE tab = ((MCRXMLTABLE)l.get(t));
                if(!s.contains(tab.getId())) {
                    a.add(t, tab.getId());
                    s.add(tab.getId());
                }
            }
            tx.commit();
        }catch(Exception e){
            tx.rollback();
            logger.error(e);
	    throw new MCRException("Error during retrieveAllIDs("+type+")", e);
        }finally{
            session.close();
        }
        return a;
    }
    
    
    public static void test()
    {
        MCRHIBXMLStore store = new MCRHIBXMLStore();
        List l = store.retrieveAllIDs(null);
        int t;
        for(t=0;t<l.size();t++) {
            System.out.println(l.get(0).toString());
        }
    }
}

