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

/**
 * Stores the content of MCROldFiles in a persistent datastore. This can be
 * a filesystem, IBM Content Manager, video streaming servers like 
 * IBM VideoCharger or Real Server, depending on the class that implements
 * this interface. The MCROldContentStore provides methods to store, delete
 * and retrieve content. It uses a storage ID and the store ID to identify 
 * the place where the content of a file is stored.
 *
 * @author Frank Lützenkirchen 
 * @deprecated USED BY MILESS 1.3 ONLY!
 * @version $Revision$ $Date$
 */
public interface MCROldContentStore
{
  /** 
   * Initializes the store and sets its unique store ID. MCROldFiles must remember 
   * this ID to indentify the store that holds their file content. The store 
   * ID is set by MCROldContentStoreFactorywhen a new store instance is built.
   *
   * @param storeID the non-null unique store ID for this store instance
   **/
  public void init( String storeID );
  
  /** 
   * Returns the unique store ID that was set for this store instance
   *
   * @return the unique store ID that was set for this store instance
   **/
  public String getID();
    
  /**
   * Stores the content of an MCROldFile by reading from an MCRContentInputStream.
   * Returns a StorageID to indentify the place where the content was stored.
   *
   * @param file the MCROldFile thats content is to be stored
   * @param source the ContentInputStream where the file content is read from
   * @return an ID that indentifies the place where the content was stored
   **/
  public String storeContent( MCROldFile file, MCRContentInputStream source )
    throws MCRPersistenceException;

  /**
   * Deletes the content of the given MCROldFile. Uses the StorageID set in the
   * MCROldFile object to indentify the place where the file content was stored
   * in this store instance.
   *
   * @param file the MCROldFile thats content is to be deleted
   */
  public void deleteContent( MCROldFile file )
    throws MCRPersistenceException;

  /**
   * Retrieves the content of the given MCROldFile to an OutputStream. 
   * Uses the StorageID set in the MCROldFile object to indentify the place 
   * where the file content was stored in this store instance.
   *
   * @param file the MCROldFile thats content is to be retrieved
   * @param target the OutputStream to write the file content to
   */
  public void retrieveContent( MCROldFile file, OutputStream target )
    throws MCRPersistenceException;
}
