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
 
package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.GregorianCalendar;

public class MCRFileMetadataManager
{
  private static Logger logger = Logger.getLogger( MCRFileMetadataManager.class.getName() );

  private static MCRFileMetadataManager manager;
  
  public static synchronized MCRFileMetadataManager instance()
  {
    if( manager == null ) manager = new MCRFileMetadataManager();
    return manager;
  }
  
  private MCRCache cache;
  
  private MCRFileMetadataStore store;

  private MCRFileMetadataManager()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    Object object = config.getInstanceOf( "MCR.IFS.FileMetadataStore.Class" );  
    store = (MCRFileMetadataStore)object;
    
    int size = config.getInt( "MCR.IFS.FileMetadataStore.CacheSize", 500 );
    cache = new MCRCache( size );
  }
  
  private long lastID = System.currentTimeMillis();
  
  synchronized String createNodeID()
  {
    lastID++;
    String value = "0000000000000000" + Long.toString( lastID, 36 );
    return value.substring( value.length() - 16 );
  }
  
  void storeNode( MCRFilesystemNode node )
    throws MCRPersistenceException
  {
    logger.debug( "IFS StoreNode " + node.getName() );
    store.storeNode( node );
    cache.put( node.getID(), node );
  }
  
  MCRFilesystemNode retrieveNode( String ID )
    throws MCRPersistenceException
  {
    logger.debug( "IFS RetrieveNode " + ID );
    MCRFilesystemNode n = (MCRFilesystemNode)( cache.get( ID ) );
    if( n != null ) 
      return n;  
    else
      return store.retrieveNode( ID );
  }
  
  MCRFilesystemNode[] retrieveRootNodes( String ownerID )
    throws MCRPersistenceException
  {
    Vector v = store.retrieveRootNodes( ownerID );
    MCRFilesystemNode[] nodes = new MCRFilesystemNode[ v.size() ];
    v.copyInto( nodes );
    return nodes;
  }
  
  MCRFilesystemNode retrieveChild( String parentID, String name )
    throws MCRPersistenceException
  { return store.retrieveChild( parentID, name ); }
  
  public MCRFilesystemNode buildNode( String type, String ID, String parentID, String ownerID, String name, long size, GregorianCalendar date, String storeID, String storageID, String fctID, String md5 )
    throws MCRPersistenceException
  {
    MCRFilesystemNode n = (MCRFilesystemNode)( cache.get( ID ) );
    if( n != null ) return n;
    
    if( type.equals( "D" ) )
      n = new MCRDirectory( ID, parentID, ownerID, name, size, date );
    else
      n = new MCRFile( ID, parentID, ownerID, name, size, date, storeID, storageID, fctID, md5 );
    
    cache.put( ID, n ); 
    return n;
  }
  
  Vector retrieveChildrenIDs( String ID )
    throws MCRPersistenceException
  { return store.retrieveChildrenIDs( ID ); }
  
  int retrieveNumberOfChildren( String parentID )
    throws MCRPersistenceException
  { return store.retrieveNumberOfChildren( parentID ); }
  
  void deleteNode( String ID )
    throws MCRPersistenceException
  {
    logger.debug( "IFS DeleteNode " + ID );
    cache.remove( ID );
    store.deleteNode( ID );
  }
}


