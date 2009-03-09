/*
 * $Revision: 13085 $ 
 * $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb 2008) $
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

import org.apache.commons.vfs.FileObject;

/**
 * Represents a set of files and directories belonging together, that are
 * stored in a persistent MCRFileStore. A FileCollection has a unique ID within
 * the store, it is the root folder of all files and directories in the collection. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileCollection extends MCRDirectory 
{
  /**
   * The store this file collection is stored in.
   */
  private MCRStore store;
  
  /**
   * The ID of this file collection
   */
  private int id;

  /**
   * Creates a new file collection in the given store, or retrieves an 
   * existing one.
   *
   * @see MCRFileStore 
   * 
   * @param store the store this file collection is stored in
   * @param id the ID of this file collection
   * @param fo the directory in the local filesystem storing this file collection
   * @param create if true, a new folder for the collection is created, otherwise the collection is assumed existing 
   */
  protected MCRFileCollection( MCRStore store, int id, FileObject fo, boolean create ) throws Exception
  {
    super( null, fo );
    this.store = store;
    this.id = id;
    
    if( create )
    {
      fo.createFolder();
      readMetadata();
      writeMetadata();
    }
    else readMetadata();
  }

  /**
   * Does nothing, because a file collection's name is always the empty string and 
   * therefore can not be renamed.
   */
  public void renameTo( String name )
  {}

  /**
   * Returns the store this file collection is stored in.
   * 
   * @return the store this file collection is stored in.
   */
  public MCRStore getStore()
  { return store; }
  
  /**
   * Returns the ID of this file collection
   * 
   * @return the ID of this file collection
   */
  public int getID()
  { return id; }
  
  /**
   * Returns the empty String
   * 
   * @return the empty String
   */
  public String getPath()
  { return ""; }
  
  /**
   * Returns this object, because the FileCollection instance is
   * the root of all files and directories contained in the collection.
   * 
   * @return this
   */
  public MCRFileCollection getRoot()
  { return this; }
}
