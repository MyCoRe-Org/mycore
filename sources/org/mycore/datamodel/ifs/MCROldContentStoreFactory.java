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
 * This class manages instances of MCROldContentStore and MCROldAudioVideoExtender 
 * and provides methods to get these for a given Store ID or MCROldFile instance.
 * The class is responsible for looking up, loading, instantiating and
 * remembering the implementations of MCROldContentStore and MCROldAudioVideoExtender 
 * that are used in the system.
 *
 * @author Frank Lützenkirchen 
 * @deprecated USED BY MILESS 1.3 ONLY!
 * @version $Revision$ $Date$
 */
public class MCROldContentStoreFactory
{
  /** Hashtable StoreID to MCROldContentStore instance */  
  protected static Hashtable stores = new Hashtable();

  /** Hashtable StoreID to Class that implements MCROldAudioVideoExtender */  
  protected static Hashtable extenderClasses = new Hashtable();
  
  /**
   * Returns the MCROldContentStore instance that is configured for this
   * StoreID. The instance that is returned is configured by the property
   * <tt>MCR.IFS.ContentStore.<StoreID>.Class</tt> in mycore.properties.
   * 
   * @param storeID the non-null ID of the MCROldContentStore implementation
   * @return the MCROldContentStore instance that uses this storeID
   * @throws MCRConfigurationException if no MCROldContentStore implementation is configured for this storeID
   */
  public static MCROldContentStore getStore( String storeID )
  {
    MCRArgumentChecker.ensureNotEmpty( storeID, "Store ID" );
    if( ! stores.containsKey( storeID ) )
    {
      try
      { 
        String storeClass = "MCR.IFS.ContentStore." + storeID + ".Class";
        Object obj = MCRConfiguration.instance().getInstanceOf( storeClass );
        MCROldContentStore s = (MCROldContentStore)( obj );
        s.init( storeID );
        stores.put( storeID, s );
      }
      catch( Exception ex )
      { 
        String msg = "Could not load MCROldContentStore with store ID = " + storeID;
        throw new MCRConfigurationException( msg, ex ); 
      }
    }
    return (MCROldContentStore)( stores.get( storeID ) );
  }
  
  /**
   * Returns the Class instance that implements the MCROldAudioVideoExtender for 
   * the MCROldContentStore with the given ID. That class is configured by 
   * the property <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in 
   * mycore.properties.
   * 
   * @param storeID the non-null StoreID of the MCROldContentStore
   * @return the Class that implements MCROldAudioVideoExtender for the StoreID given, or null
   * @throws MCRConfigurationException if the MCROldAudioVideoExtender implementation class could not be loaded
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
   * Returns true if the MCROldContentStore with the given StoreID provides an 
   * MCROldAudioVideoExtender implementation, false otherwise.
   * The MCROldAudioVideoExtender for a certain MCROldContentStore is configured by 
   * the property <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in 
   * mycore.properties.
   * 
   * @param storeID the non-null StoreID of the MCROldContentStore
   * @return the MCROldAudioVideoExtender for the StoreID given, or null
   * @throws MCRConfigurationException if the MCROldAudioVideoExtender implementation class could not be loaded
  */
  static boolean providesAudioVideoExtender( String storeID )
  { return ( getExtenderClass( storeID ) != null ); }
  
  /**
   * If the MCROldContentStore of the MCROldFile given provides an 
   * MCROldAudioVideoExtender implementation, this method creates and initializes 
   * the MCROldAudioVideoExtender instance for the MCROldFile. 
   * The instance that is returned is configured by the property
   * <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in mycore.properties.
   * 
   * @param file the non-null MCROldFile
   * @return the MCROldAudioVideoExtender for the MCROldFile given, or null
   * @throws MCRConfigurationException if the MCROldAudioVideoExtender implementation class could not be loaded
   */
  static MCROldAudioVideoExtender buildExtender( MCROldFile file )
  {
    MCRArgumentChecker.ensureNotNull( file, "file" );

    String storeID = file.getStoreID();
    if( ! providesAudioVideoExtender( storeID ) ) return null;
    
    Class cl = getExtenderClass( storeID );
    
    try
    { 
      Object obj = cl.newInstance();
      MCROldAudioVideoExtender ext = (MCROldAudioVideoExtender)obj;
      ext.init( file );
      return ext;
    }
    catch( Exception exc )
    { 
      if( exc instanceof MCRException ) throw (MCRException)exc;
      
      String msg = "Could not build MCROldAudioVideoExtender instance";
      throw new MCRConfigurationException( msg, exc ); 
    }
  }
}
