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

import java.util.List;

import java.sql.Timestamp;

import org.apache.log4j.Logger;

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import org.mycore.backend.hibernate.tables.*;

import org.hibernate.*;

import java.util.Vector;
import java.util.GregorianCalendar;

/** 
 * This class implements the MCRFileMetadataStore.
 *
 **/
public class MCRHIBFileMetadataStore implements MCRFileMetadataStore
{
    protected String table;
    
    // logger
    static Logger logger=Logger.getLogger(MCRHIBLinkTableStore.class.getName());

    private Session getSession() {
	return MCRHIBConnection.instance().getSession();
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
	else if(node instanceof MCRDirectory)
	{
	    MCRDirectory dir = (MCRDirectory)node;
	    
	    TYPE    = "D";
	    NUMCHDD = dir.getNumChildren( MCRDirectory.DIRECTORIES, MCRDirectory.HERE  );
	    NUMCHDF = dir.getNumChildren( MCRDirectory.FILES,       MCRDirectory.HERE  );
	    NUMCHTD = dir.getNumChildren( MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL );
	    NUMCHTF = dir.getNumChildren( MCRDirectory.FILES,       MCRDirectory.TOTAL );
	}
        else throw new MCRPersistenceException("MCRFilesystemNode must be either MCRFile or MCRDirectory");

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
	fs.setStoreid(STOREID);
	fs.setStorageid(STORAGEID);
	fs.setFctid(FCTID);
	fs.setMd5(MD5);
	fs.setNumchdd(NUMCHDD); 
	fs.setNumchdf(NUMCHDF);
	fs.setNumchtd(NUMCHTD);
	fs.setNumchtf(NUMCHTF);

	session.saveOrUpdate(fs);

	tx.commit();
	session.close();
    }
    
    public String retrieveRootNodeID(String ownerID) throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("from MCRFSNODES where OWNER = '" + ownerID + "' and PID=NULL").list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no node with ID = " + ownerID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
	return ((MCRFSNODES)(l.get(0))).getId();
    }
    public MCRFilesystemNode retrieveChild(String parentID, String name)
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("from MCRFSNODES where PID = '" + parentID  + "' and NAME = '"+name+"'").list();
        if (l.size() < 1) {
	    return null;
        }
        tx.commit();
        session.close();
        return buildNode((MCRFSNODES)l.get(0));
    }
    
    public Vector retrieveChildrenIDs(String parentID)
      throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("from MCRFSNODES where PID = '" + parentID + "'").list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no node with PID = " + parentID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();

	int t;
	Vector v = new Vector(l.size());
	for(t=0;t<l.size();t++) {
	    v.add(t, ((MCRFSNODES)l.get(0)).getId());
	}
	return v;
    }
    
    public void deleteNode( String ID )
      throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        List l = session.createQuery("from MCRFSNODES where ID = '" + ID + "'").list();
        int t;
        for(t=0;t<l.size();t++) {
            MCRFSNODES node = (MCRFSNODES)l.get(t);
	    session.delete(node);
        }
        tx.commit();
        session.close();
       
    }
    
    public MCRFilesystemNode retrieveNode(String ID) throws MCRPersistenceException
    {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
       
        List l = session.createQuery("from MCRFSNODES where ID = '" + ID + "'").list();
        if (l.size() < 1) {
            String msg = "MCRSQLUserStore.retrieveUser(): There is no user with ID = " + ID;
            throw new MCRException(msg);
        }
        tx.commit();
        session.close();
        return buildNode((MCRFSNODES)l.get(0));

    }

    public MCRFilesystemNode buildNode(MCRFSNODES node)
    {
	GregorianCalendar greg = new GregorianCalendar();
	greg.setTime(node.getDate());

	return MCRFileMetadataManager.instance().buildNode( 
		node.getType(),
		node.getId(),
		node.getPid(),
		node.getOwner(),
		node.getName(),
		node.getLabel(),
		node.getSize(),
		greg,
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
