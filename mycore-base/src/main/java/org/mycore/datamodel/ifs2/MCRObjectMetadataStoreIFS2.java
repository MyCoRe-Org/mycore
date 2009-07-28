/*
 * $Revision$ 
 * $Date$
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

package org.mycore.datamodel.ifs2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLTableInterface;
import org.mycore.datamodel.ifs2.MCRContent;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRStoredMetadata;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Stores XML metadata of MCRObject in IFS2
 */
public class MCRObjectMetadataStoreIFS2 
  implements MCRXMLTableInterface 
{
  private MCRMetadataStore store;

  private String project;
  
  public MCRObjectMetadataStoreIFS2(){}

 /**
  * Reads the configuration and initializes the store.
  * 
  * MCR.IFS2.Store.{type}.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
  * MCR.IFS2.Store.{type}.BaseDir={/path/to/store/directory/}
  * MCR.IFS2.Store.{type}.SlotLayout=4-2-2
  * MCR.IFS2.Store.{type}.SVNRepositoryURL=file:///{where/the/svn/store/should/be/created/}
  * 
  * @param type the MCRObjectID type
  */
  public void init( String type )
  {
    if( ( type == null ) || ( ( type = type.trim() ).length() == 0 ) ) 
      throw new MCRConfigurationException( "Trying to init object metadata store with empty type ID" );

    MCRConfiguration config = MCRConfiguration.instance();

    boolean defined = config.getBoolean( "MCR.Metadata.Type." + type, false );
    if( ! defined ) 
      throw new MCRConfigurationException( "The configuration property MCR.Metadata.Type." + type + " is not defined!" );

    this.store = MCRMetadataStore.getStore(type);
    this.project = config.getString("MCR.SWF.Project.ID", "MyCoRe");
    this.project = config.getString("MCR.SWF.Project.ID." + type, this.project );
  }

  public synchronized void create( String mcrid, byte[] xml, Date lastModified )
  {
    int ID = new MCRObjectID( mcrid ).getNumberAsInteger();
    try
    {
      MCRStoredMetadata sm = store.create( MCRContent.readFrom( xml ), ID );
      sm.setLastModified( lastModified );
    }
    catch( Exception ex )
    { throw new MCRPersistenceException( "Exception storing XML of " + mcrid, ex ); }
  }

  public synchronized void delete( String mcrid )
  {
    int ID = new MCRObjectID( mcrid ).getNumberAsInteger();
    try
    {
      store.delete( ID );
    }
    catch( Exception ex )
    { throw new MCRPersistenceException( "Exception deleting stored XML of " + mcrid, ex ); }
  }

  public synchronized void update( String mcrid, byte[] xml, Date lastModified ) 
  {
    int ID = new MCRObjectID( mcrid ).getNumberAsInteger();
    try
    {
      MCRStoredMetadata sm = store.retrieve( ID );
      sm.update( MCRContent.readFrom( xml ) );
    }
    catch( Exception ex )
    { throw new MCRPersistenceException( "Exception updating XML of " + mcrid, ex ); }
  }

  public InputStream retrieve( String mcrid ) 
  {
    int ID = new MCRObjectID( mcrid ).getNumberAsInteger();
    try
    {
      MCRStoredMetadata sm = store.retrieve( ID );
      return sm.getMetadata().getInputStream();
    }
    catch( Exception ex )
    { throw new MCRPersistenceException( "Exception retrieving XML of " + mcrid, ex ); }
  }

  public synchronized int getHighestStoredID()
  {
    return store.getHighestStoredID();
  }

  public boolean exists( String mcrid ) 
  {
    int ID = new MCRObjectID( mcrid ).getNumberAsInteger();
    try
    {
      return store.exists( ID );
    }
    catch( Exception ex )
    { throw new MCRPersistenceException( "Exception checking existence of " + mcrid, ex ); }
  }

  public List<String> retrieveAllIDs() 
  { 
    Iterator<Integer> IDs = store.listIDs( MCRMetadataStore.ASCENDING );
    List<String> list = new ArrayList<String>();
    MCRObjectID oid = new MCRObjectID( project + "_" + store.getID() + "_1" );
    while( IDs.hasNext() )
    {
      oid.setNumber( IDs.next() );
      list.add( oid.getId() );
    }
    return list;
  }

  public List<MCRObjectIDDate> listObjectDates( String type ) 
  {
    Iterator<Integer> IDs = store.listIDs( MCRMetadataStore.ASCENDING );
    List<MCRObjectIDDate> list = new ArrayList<MCRObjectIDDate>();
    MCRObjectID oid = new MCRObjectID( project + "_" + store.getID() + "_1" );
    while( IDs.hasNext() )
    {
      int ID = IDs.next();
      oid.setNumber( ID );
      try
      {
        MCRStoredMetadata sm = store.retrieve( ID );
        list.add( new MyMCRObjectIDDate( oid.getId(), sm.getLastModified() ) );
      }
      catch( Exception ex )
      { throw new MCRPersistenceException( "Exception retrieving data of " + ID, ex ); }
    }

    return list;
  }
   
  class MyMCRObjectIDDate implements MCRObjectIDDate
  {
    String oid;
    Date date;
    
    MyMCRObjectIDDate( String oid, Date lastModified )
    {
      this.oid = oid;
      this.date = lastModified;
    }
    
    public String getId()
    { return oid; }
    
    public Date getLastModified()
    { return date; }
  }
}
