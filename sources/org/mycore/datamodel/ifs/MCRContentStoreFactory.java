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
import java.util.*;

/**
 * This class manages instances of MCRContentStore and MCRAudioVideoExtender 
 * and provides methods to get these for a given Store ID or MCRFile instance.
 * The class is responsible for looking up, loading, instantiating and
 * remembering the implementations of MCRContentStore and MCRAudioVideoExtender 
 * that are used in the system.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRContentStoreFactory
{
  /** Hashtable StoreID to MCRContentStore instance */  
  protected static Hashtable stores = new Hashtable();

  /** Hashtable StoreID to Class that implements MCRAudioVideoExtender */  
  protected static Hashtable extenderClasses = new Hashtable();
  
  /** The MCRContentStoreSelector implementation that will be used */
  protected static MCRContentStoreSelector storeSelector;
  
  /**
   * Returns the MCRContentStore instance that is configured for this
   * StoreID. The instance that is returned is configured by the property
   * <tt>MCR.IFS.ContentStore.<StoreID>.Class</tt> in mycore.properties.
   * 
   * @param storeID the non-null ID of the MCRContentStore implementation
   * @return the MCRContentStore instance that uses this storeID
   * @throws MCRConfigurationException if no MCRContentStore implementation is configured for this storeID
   */
  public static MCRContentStore getStore( String storeID )
  {
    MCRArgumentChecker.ensureNotEmpty( storeID, "Store ID" );
    if( ! stores.containsKey( storeID ) )
    {
      try
      { 
        String storeClass = "MCR.IFS.ContentStore." + storeID + ".Class";
        Object obj = MCRConfiguration.instance().getInstanceOf( storeClass );
        MCRContentStore s = (MCRContentStore)( obj );
        s.init( storeID );
        stores.put( storeID, s );
      }
      catch( Exception ex )
      { 
        String msg = "Could not load MCRContentStore with store ID = " + storeID;
        throw new MCRConfigurationException( msg, ex ); 
      }
    }
    return (MCRContentStore)( stores.get( storeID ) );
  }

  /**
   * Returns the MCRContentStore instance that should be used
   * to store the content of the given file. The configured
   * MCRContentStoreSelector is used to make this decision.
   *
   * @see MCRContentStoreSelector
   * @see MCRContentStore
   **/
  public static MCRContentStore selectStore( MCRFile file )
  {
    if( storeSelector == null )
    {
      String property = "MCF.IFS.ContentStoreSelector.Class";
      Object obj = MCRConfiguration.instance().getInstanceOf( property );
      storeSelector = (MCRContentStoreSelector)obj;
    }
    String store = storeSelector.selectStore( file );
    return getStore( store );
  }
  
  /**
   * Returns the Class instance that implements the MCRAudioVideoExtender for 
   * the MCRContentStore with the given ID. That class is configured by 
   * the property <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in 
   * mycore.properties.
   * 
   * @param storeID the non-null StoreID of the MCRContentStore
   * @return the Class that implements MCRAudioVideoExtender for the StoreID given, or null
   * @throws MCRConfigurationException if the MCRAudioVideoExtender implementation class could not be loaded
  */
  protected static Class getExtenderClass( String storeID )
  {
    MCRArgumentChecker.ensureNotNull( storeID, "store ID" );
    
    String storeClass = "MCR.IFS.AVExtender." + storeID + ".Class";

    String value = MCRConfiguration.instance().getString( storeClass, "" );
    if( value.equals( "" ) ) return null;
    
    if( ! extenderClasses.containsKey( storeID ) )
    {
      try
      { 
        Class cl = Class.forName( value );
        extenderClasses.put( storeID, cl );
      }
      catch( Exception ex )
      { 
        String msg = "Could not load AudioVideoExtender class " + value;
        throw new MCRConfigurationException( msg, ex ); 
      }
    }
    
    return (Class)( extenderClasses.get( storeID ) );
  }
  
  /**
   * Returns true if the MCRContentStore with the given StoreID provides an 
   * MCRAudioVideoExtender implementation, false otherwise.
   * The MCRAudioVideoExtender for a certain MCRContentStore is configured by 
   * the property <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in 
   * mycore.properties.
   * 
   * @param storeID the non-null StoreID of the MCRContentStore
   * @return the MCRAudioVideoExtender for the StoreID given, or null
   * @throws MCRConfigurationException if the MCRAudioVideoExtender implementation class could not be loaded
  */
  static boolean providesAudioVideoExtender( String storeID )
  { return ( getExtenderClass( storeID ) != null ); }
  
  /**
   * If the MCRContentStore of the MCRFile given provides an 
   * MCRAudioVideoExtender implementation, this method creates and initializes 
   * the MCRAudioVideoExtender instance for the MCRFile. 
   * The instance that is returned is configured by the property
   * <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in mycore.properties.
   * 
   * @param storeID the non-null ID of the MCRContentStore for that MCRFile
   * @param storageID the non-null storage ID of that MCRFile
   * @return the MCRAudioVideoExtender for the MCRFile given, or null
   * @throws MCRConfigurationException if the MCRAudioVideoExtender implementation class could not be loaded
   */
  static MCRAudioVideoExtender buildExtender( String storeID, String storageID, String extension )
  {
    MCRArgumentChecker.ensureNotNull( storeID,   "store ID"   );
    MCRArgumentChecker.ensureNotNull( storageID, "storage ID" );

    if( ! providesAudioVideoExtender( storeID ) ) return null;
    
    Class cl = getExtenderClass( storeID );
    
    try
    { 
      Object obj = cl.newInstance();
      MCRAudioVideoExtender ext = (MCRAudioVideoExtender)obj;
      ext.init( storageID, storeID, extension );
      return ext;
    }
    catch( Exception exc )
    { 
      if( exc instanceof MCRException ) throw (MCRException)exc;
      
      String msg = "Could not build MCRAudioVideoExtender instance";
      throw new MCRConfigurationException( msg, exc ); 
    }
  }
}
