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

package org.mycore.backend.sql;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.datamodel.classifications.*;

import org.hibernate.*;
import org.hibernate.cfg.Configuration;

import java.util.Vector;
import java.util.GregorianCalendar;

/** 
 * This class implements the MCRLinkTableInterface.
 *
 **/
public class MCRHIBLinkTableStore implements MCRLinkTableInterface
{
    protected String table;
    
    // logger
    static Logger logger=Logger.getLogger(MCRHIBLinkTableStore.class.getName());

    // internal data
    private String mtype;
    private int lengthClassID  = MCRMetaClassification.MAX_CLASSID_LENGTH;
    private int lengthCategID  = MCRMetaClassification.MAX_CATEGID_LENGTH;
    private int lengthObjectID = MCRObjectID.MAX_LENGTH;

    private String classname;
    
    private SessionFactory sessionFactory;
    
    private Session getSession() {
        return sessionFactory.openSession();
    }

    public MCRHIBFileMetadataStore() throws MCRPersistenceException
    { 
    }
    
    public void storeNode(MCRFilesystemNode node) throws MCRPersistenceException
    {
	deleteNode( node.getID() );
	
	String ID    = node.getID();
	String PID   = node.getParentID();
	String OWNER = node.getOwnerID();
	String NAME  = node.getName();
	String LABEL = node.getLabel();
	long   SIZE  = node.getSize();

	GregorianCalendar DATE = node.getLastModified();  
     
	String TYPE       = null;
	String STOREID    = null;
	String STORAGEID  = null;
	String FCTID      = null;
	String MD5        = null;
	
	int NUMCHDD = 0, NUMCHDF = 0, NUMCHTD = 0, NUMCHTF = 0;

	if( node instanceof MCRFile )
	{
	    MCRFile file = (MCRFile)node;

	    TYPE      = "F";
	    STOREID   = file.getStoreID();
	    STORAGEID = file.getStorageID();
	    FCTID     = file.getContentTypeID();
	    MD5       = file.getMD5();
	}
	else
	{
	    MCRDirectory dir = (MCRDirectory)node;
	    
	    TYPE    = "D";
	    NUMCHDD = dir.getNumChildren( MCRDirectory.DIRECTORIES, MCRDirectory.HERE  );
	    NUMCHDF = dir.getNumChildren( MCRDirectory.FILES,       MCRDirectory.HERE  );
	    NUMCHTD = dir.getNumChildren( MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL );
	    NUMCHTF = dir.getNumChildren( MCRDirectory.FILES,       MCRDirectory.TOTAL );
	}
      
	MCRHIBConnection connection = MCRHIBConnectionPool.instance().getConnection();

	Session session = getSession();
	Transaction tx = session.beginTransaction();

	MCRFSNODES fs = new MCRFSNODES();
	fs.setId(ID);
	fs.setPid(PID);
	fs.setType(TYPE);
	fs.setOwner(OWNER);
	fs.setName(NAME);
	fs.setLabel(LABEL);
	fs.setSize(SIZE);
	fs.setDate(new Timestamp(DATE.getTime().getTime()));
	fs.setStoreID(STOREID);
	fs.setStorageID(STORAGEID);
	fs.setFctid(FCTID);
	fs.setMd5(MD5);
	fs.setNumchdd(NUMCHDD); 
	fs.setNumchdf(NUMCHDD); 
	fs.setNumchtd(NUMCHDD); 
	fs.setNumchtf(NUMCHDD); 

	tx.commit();
	session.close();
    }
    
    public String retrieveRootNodeID(String ownerID)
      throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("SELECT FROM MCRFSNODES WHERE OWNER = " + ownerID + " AND PID=NULL").list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no node with ID = "
                    + userID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
        return buildNode((MCRFSNODE)l.get(0));
    }
    public MCRFilesystemNode retrieveChild(String parentID, String name)
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("SELECT FROM MCRFSNODES WHERE PARENT = " + parentID  + " AND NAME = "+name).list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no node with ID = "
                    + userID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
        return buildNode((MCRFSNODE)l.get(0));
    }
    
    public Vector retrieveChildrenIDs(String parentID)
      throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("SELECT FROM MCRFSNODES WHERE PARENT = " + parentID  + " AND NAME = "+name).list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no node with ID = "
                    + userID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
        return buildNode((MCRFSNODE)l.get(0));
    }
    
    public void deleteNode( String ID )
      throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
	session.delete("SELECT FROM MCRFSNODES WHERE ID="+ID);
        tx.commit();
        session.close();
        return buildNode((MCRFSNODE)l.get(0));
       
    }
    
    public MCRFilesystemNode retrieveNode(String ID) throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("SELECT FROM MCRFSNODES WHERE MCRID = " + ID).list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no user with ID = "
                    + userID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
        return buildNode((MCRFSNODE)l.get(0));

    }

    public MCRFilesystemNode buildNode(MCRFSNODE node)
    {
	return MCRFileMetadataManager.instance().buildNode( 
		node.getObjectType(),
		node.getID(),
		node.getParentID(),
		node.getOwnerID(),
		node.getName(),
		node.getLabel(),
		node.getSize(),
		node.getDate(),
		node.getStoreid(),
		node.getStorageid(),
		node.getFctid(),
		node.getMd5(),
		node.getNumchdd(),
		node.getNumchdf(),
		node.getNumchtd(),
		node.getNumchtf());
    }
    
}
