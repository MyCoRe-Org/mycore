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
 * Stores the content of MCRFiles in a persistent datastore. This can be
 * a filesystem, IBM Content Manager, video streaming servers like 
 * IBM VideoCharger or Real Server, depending on the class that implements
 * this interface. The MCRContentStore provides methods to store, delete
 * and retrieve content. It uses a storage ID and the store ID to identify 
 * the place where the content of a file is stored.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public abstract class MCRContentStore
{
  /** The unique store ID for this MCRContentStore implementation */
  protected String storeID;

  /** The prefix of all properties in mycore.properties for this store */
  protected String prefix;

  public MCRContentStore()
  {
    formatter = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss_SSS" );
    lastID    = null;
  }
  
  /** 
   * Initializes the store and sets its unique store ID. MCRFiles must remember 
   * this ID to indentify the store that holds their file content. The store 
   * ID is set by MCRContentStoreFactory when a new store instance is built.
   * Subclasses should override this method.
   *
   * @param storeID the non-null unique store ID for this store instance
   **/
  public void init( String storeID )
  { 
    this.storeID = storeID; 
    this.prefix = "MCR.IFS.ContentStore." + storeID + ".";
  }
  
  /** 
   * Returns the unique store ID that was set for this store instance
   *
   * @return the unique store ID that was set for this store instance
   **/
  public String getID()
  { return storeID; }

  /**
   * Stores the content of an MCRFile by reading from an MCRContentInputStream.
   * Returns a StorageID to indentify the place where the content was stored.
   *
   * @param file the MCRFile thats content is to be stored
   * @param source the ContentInputStream where the file content is read from
   * @return an ID that indentifies the place where the content was stored
   **/
  public abstract String storeContent( MCRFileReader file, MCRContentInputStream source )
    throws MCRPersistenceException;

  /**
   * Deletes the content of an MCRFile object that is stored under the given
   * Storage ID in this store instance.
   *
   * @param storageID the storage ID of the MCRFile object
   */
  public abstract void deleteContent( String storageID )
    throws MCRPersistenceException;

  /**
   * Retrieves the content of an MCRFile to an OutputStream. 
   * Uses the StorageID to indentify the place where the file content was 
   * stored in this store instance.
   *
   * @param file the MCRFile thats content should be retrieved
   * @param target the OutputStream to write the file content to
   */
  public abstract void retrieveContent( MCRFileReader file, OutputStream target )
    throws MCRPersistenceException;
  
  /** DateFormat used to construct new unique IDs based on timecode */
  protected DateFormat formatter;

  /** The last ID that was constructed */
  protected String lastID;
  
  /**
   * Constructs a new unique ID based on timecode for storing content
   */
  protected synchronized String buildNextID()
  {
    String ID = null;
    do{ ID = formatter.format( new Date() ); }
    while( ID.equals( lastID ) );
    return ( lastID = ID );
  }
  
  protected String[] buildSlotPath()
  {
    Random random = new Random();
    int na = random.nextInt( 100 );
    int nb = random.nextInt( 100 );
    String sa = String.valueOf( na );
    String sb = String.valueOf( nb );
    if( na < 10 ) sa = "0" + sa;
    if( nb < 10 ) sb = "0" + sb;
    String[] slots = new String[ 2 ];
    slots[ 0 ] = sa;
    slots[ 1 ] = sb;
    return slots;
  }
}
