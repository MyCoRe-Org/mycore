/**
 * $RCSfile$
 * $Revision$ $Date$
 *
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

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Builds an Index for the content of MCRFiles. This can be
 * a lucene, IBM Content Manager, xml:db, depending on the class that implements
 * this interface. The MCRContentIndexer provides methods to build, delete
 * and serach the index. It uses a storage ID and the indexer ID to identify 
 * the place where the content of a file is indexed.
 *
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public abstract class MCRContentIndexer
{
  /** The unique indexer ID for this MCRContentIndexer implementation */
  protected String indexerID;

  /** The prefix of all properties in mycore.properties for this indexer */
  protected String prefix;

  /** Default constructor **/
  public MCRContentIndexer(){}
  
  /** 
   * Initializes the indexer and sets its unique indexer ID. MCRFiles must remember 
   * this ID to indentify the indexer that holds their file content. The indexer 
   * ID is set by MCRContentIndexerFactory when a new indexer instance is built.
   * Subclasses should override this method.
   *
   * @param indexerID the non-null unique indexer ID for this indexer instance
   **/
  public void init( String indexerID, Hashtable attribute )
  { 
    this.indexerID = indexerID; 
    this.prefix = "MCR.IFS.ContentIndexer." + indexerID + ".";
  }
  
  /** 
   * Returns the unique indexer ID that was set for this indexer instance
   *
   * @return the unique indexer ID that was set for this indexer instance
   **/
  public String getID()
  { return indexerID; }

  /**
   * Builds an index of the content of an MCRFile by reading from an MCRContentInputStream.
   *
   * @param file the MCRFile thats content is to be indexed
   * @param source the ContentInputStream where the file content is read from
   **/
  public void indexContent( MCRFile file )
    throws MCRPersistenceException
  {
    try
    { doIndexContent( file ); }
    catch( Exception exc )
    {
      if( ! ( exc instanceof MCRException ) )
      {
        StringBuffer msg = new StringBuffer();
        msg.append( "Could not index content of file [" );
        msg.append( file.getPath() ).append( "] in indexer [" );
        msg.append( indexerID ).append( "]" );
        throw new MCRPersistenceException( msg.toString(), exc );
      }
      else throw (MCRException)exc;
    }
  }
  
  /**
   * Builds an index of the content of an MCRFile by reading from an MCRContentInputStream.
   *
   * @param file the MCRFile thats content is to be indexed
   * @param source the ContentInputStream where the file content is read from
   **/
  protected abstract void doIndexContent( MCRFile file )
    throws Exception;

  /**
   * Deletes the index of an MCRFile object that is indexed under the given
   * Storage ID in this indexer instance.
   *
   * @param the MCRFile object to delete
   */
  public void deleteIndex( MCRFile file )
    throws MCRException
  {
    try
    { doDeleteIndex( file ); }
    catch( Exception exc )
    {
      if( ! ( exc instanceof MCRException ) )
      {
        StringBuffer msg = new StringBuffer();
        String storageID = file.getStoreID( );
        msg.append( "Could not delete index of file with storage ID [" );
        msg.append( storageID ).append( "] in indexer [" );
        msg.append( indexerID ).append( "]" );
        throw new MCRPersistenceException( msg.toString(), exc );
      }
      else throw (MCRException)exc;
    }
  }
    
  /**
   * Deletes the index of an MCRFile object that is indexed under the given
   * Storage ID in this indexer instance.
   *
   * @param the MCRFile object to delete
   */
  protected abstract void doDeleteIndex( MCRFile file )
    throws Exception;

  /**
   * Search in Index with query
   *
   * @param query
   *
   * @return the hits of the query (ifs IDs)
   *
   */
  public abstract String[] doSearchIndex( String query )
    throws Exception;

  /**
   * Search in Index with query
   *
   * @param query the query for the serch
   *
   * @return the hits of the query (ifs IDs)
   *
   */
  public String[] searchIndex( String query )
    throws MCRException
  {
    try
    { return doSearchIndex( query ); }
    catch( Exception exc )
    {
      if( ! ( exc instanceof MCRException ) )
      {
        StringBuffer msg = new StringBuffer();
        msg.append( "Could not build search with indexer [" );
        msg.append( indexerID ).append( "]" );
        throw new MCRPersistenceException( msg.toString(), exc );
      }
      else throw (MCRException)exc;
    }
  }
    
}
