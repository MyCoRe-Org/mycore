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

import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Decides which MCRContentStore implementation should be used to store the
 * content of a given file, based on the content type of the file.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRSimpleContentStoreSelector implements MCRContentStoreSelector
{
  /** the default content store to use if no other rule matches */
  protected String defaultID;
  
  /** store lookup table where keys are file content type IDs, values are content store IDs */
  protected Hashtable table;
  
  /** list of all storeIDs **/
  protected String[] storeIDs;
  
  public MCRSimpleContentStoreSelector()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    String file = config.getString( "MCR.IFS.ContentStoreSelector.ConfigFile" );
    Element xml = MCRURIResolver.instance().resolve( "resource:" + file );

    table = new Hashtable();
      
    List stores = xml.getChildren( "store" );
    storeIDs= new String[stores.size()+1];

    for( int i = 0; i < stores.size(); i++ )
    {
      Element store = (Element)( stores.get( i ) );
      String storeID = store.getAttributeValue( "ID" );
      storeIDs[i]=storeID;
        
      List types = store.getChildren();
      for( int j = 0; j < types.size(); j++ )
      {
        Element type = (Element)( types.get( j ) );
        String typeID = type.getTextTrim();

        table.put( typeID, storeID );
      }
    }
      
    defaultID = xml.getAttributeValue( "default" );
    //NOTE: if defaultID is listed as a <store> it's inserted twice here
    storeIDs[storeIDs.length-1]=defaultID;
  }
  
  public String selectStore( MCRFile file ) 
    throws MCRException
  {
    String typeID = file.getContentTypeID();
    
    if( table.containsKey( typeID ) )
      return (String)( table.get( typeID ) );
    return defaultID;
  }
  
  public String[] getAvailableStoreIDs() {
      return storeIDs;
  }
}
