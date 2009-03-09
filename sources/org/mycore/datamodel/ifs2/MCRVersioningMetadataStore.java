/*
 * $Revision: 14836 $ 
 * $Date: 2009-03-09 10:34:44 +0100 (Mo, 09 Mrz 2009) $
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.jdom.Document;

/**
 * Additionally commits each change of metadata in the store to a subversion repository.
 * Allows external changes of metadata in the repository, and provides a method to
 * update to the HEAD revision afterwards. Provides method to retrieve all versions stored
 * for a given ID. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVersioningMetadataStore extends MCRMetadataStore 
{
  /**
   * Creates a new versioning metadata store instance. 
   * 
   * @param type the document type that is stored in this store
   * @param baseDir the base directory in the local filesystem storing the data
   * @param slotLayout the layout of slot subdirectories
   * @param the cache size, to disable cache use a size of 0. 
   */
  protected MCRVersioningMetadataStore( String type, String baseDir, String slotLayout, int cacheSize )
  { 
    super( type, baseDir, slotLayout, cacheSize ); 
  }
    
  public void create( Document xml, int id ) throws Exception
  {
    super.create( xml, id );
    // TODO: create in SVN
  }
  
  public void update( Document xml, int id ) throws Exception
  {
    super.update( xml, id );
    // TODO: update in SVN
  }
  
  public void delete( int id ) throws Exception
  {
    super.delete( id );
    // TODO: delete in SVN
  }
  
  /**
   * Updates the data stored for this ID to the latest version from SVN.
   * 
   * @param id the ID of the metadata document that should be updated
   */
  public void update( int id ) throws Exception
  {
    // TODO: update to head from SVN  
  }
  
  /**
   * Updates all data stored to the latest version from SVN. 
   */
  public void updateAll() throws Exception
  {
    Enumeration<Integer> ids = listIDs( MCRStore.ASCENDING );
    while( ids.hasMoreElements() ) update( ids.nextElement() );
  }
  
  /**
   * Retrieves a list of all stored versions for the given metadata document ID 
   *   
   * @return a list of all versions stored for the given ID
   */
  public List<MCRMetadataVersion> getVersions( int id ) throws Exception
  {
    List<MCRMetadataVersion> list = new ArrayList<MCRMetadataVersion>(); 
    // TODO: read and create from SVN  
    return list;  
  }
}
