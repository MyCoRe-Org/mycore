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

import java.util.Date;

import org.jdom.Document;

/**
 * Provides information about a stored version of metadata 
 * and allows to retrieve that version from SVN
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataVersion 
{
  /**
   * The store this version comes from
   */
  private MCRVersioningMetadataStore store;  
    
  /**
   * The ID of the metadata document this version belongs to 
   */
  private int id;
    
  /** 
   * The revision number of this version
   */
  private int revision;
  
  /**
   * The user that created this version
   */
  private String user;
  
  /**
   * The date this version was created
   */
  private Date date;
  
  /**
   * If true, this version is a deletion of the metadata 
   */
  private boolean deleted;
    
  /**
   * Creates a new metadata version info object
   * 
   * @param id the ID of the metadata document this version belongs to
   */
  MCRMetadataVersion( int id, MCRVersioningMetadataStore store )
  {
    this.id = id;
    this.store = store;
  }
  
  /**
   * Returns the ID of the metadata object this version belongs to
   * 
   * @return the ID of the metadata object this version belongs to
   */
  public int getID()
  { return id; }
  
  /**
   * Returns the store this version comes from
   */
  public MCRVersioningMetadataStore getStore()
  { return store; }
  
  /**
   * Returns the SVN revision number of this version
   * 
   * @return the SVN revision number of this version
   */
  public int getRevision()
  { return revision; }
  
  /**
   * Returns the user that created this version
   * 
   * @return the user that created this version
   */
  public String getUser()
  { return user; }
  
  /**
   * Returns the date and time this version was created
   * 
   * @return the date and time this version was created
   */
  public Date getDate()
  { return date; }
  
  /**
   * If true, this version is a deletion of the metadata
   * 
   * @return true, if this version is a deletion
   */
  public boolean isDeleted()
  { return deleted; }
  
  /**
   * Retrieves this version of the metadata
   * 
   * @return the metadata document as it was in this version
   */
  public Document retrieve()
  {
    if( deleted ) return null;
    return new Document(); 
  }
  
  /**
   * Replaces the current version of the metadata object with this version,
   * which means that a new version is created that is identical to this old version.
   * The stored metadata document is updated to the old version of the metadata. 
   */
  public void replaceCurrentVersion() throws Exception
  {
    Document xml = retrieve();
    store.update( xml, id );  
  }
}
