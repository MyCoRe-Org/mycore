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

package org.mycore.backend.cm7;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import java.util.*;
import java.io.*;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects in a IBM Content Manager 7 index class. The index class, the
 * keyfield labels and maximum DKDDO size can be configured in mycore.properties:
 *
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.SegmentSize     Maximum DKDDO size in bytes, default is 1 MB
 *   MCR.IFS.ContentStore.<StoreID>.IndexClass      Index Class to use
 *   MCR.IFS.ContentStore.<StoreID>.Keyfield.File   Keyfield storing file ID
 *   MCR.IFS.ContentStore.<StoreID>.Keyfield.Time   Keyfield storing timestamp
 *   MCR.IFS.ContentStore.<StoreID>.Keyfield.Owner  Keyfield storing owner ID
 * </code>
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCStoreContentManager7 extends MCRContentStore
{ 
  /** The maximum DKDDO size. When filesize is bigger, multiple DKDDOs are written */  
  protected int segmentSize;
  
  /** The index class to use to store content */
  protected String indexClass;
  
  /** The name of the keyfield that stores the MCRFile.getID() */
  protected String keyfieldFile;
  
  /** The name of the keyfield that stores the creation timestamp */
  protected String keyfieldTime;
  
  /** The name of the keyfield that stores the creation MCRFile.getOwnerID */
  protected String keyfieldOwner;
  
  public void init( String storeID )
  {
    super.init( storeID );
      
    MCRConfiguration config = MCRConfiguration.instance();  
      
    segmentSize  = config.getInt   ( prefix + "SegmentSize", 1024 * 1024 );
    indexClass   = config.getString( prefix + "IndexClass"    ); 
    keyfieldFile = config.getString( prefix + "Keyfield.File" );
    keyfieldTime = config.getString( prefix + "Keyfield.Time" );
    keyfieldOwner = config.getString( prefix + "Keyfield.Owner" );
  }
  
  protected String doStoreContent( MCRFileReader file, MCRContentInputStream source )
    throws Exception
  {
    DKDatastoreDL connection = MCRCM7ConnectionPool.instance().getConnection();
    try
    {
      MCRCM7Item item = new MCRCM7Item( connection, indexClass, DKConstant.DK_DOCUMENT );
      item.setKeyfield( keyfieldFile, file.getID()         );
      item.setKeyfield( keyfieldTime, buildNextTimestamp() );
      item.setKeyfield( keyfieldOwner, ((MCRFile)file).getOwnerID() );
      item.create();
      String itemID = item.getItemId();
      
      // Read contents and store each segment of bytes in a separate item part
      for( int num = 0, partID = 1; num != -1; partID++ )
      {
        byte[] buffer = new byte[ segmentSize ];
        num = source.read( buffer );
        
        if( num > 0 )
        {
          DKPidXDODL pID = new DKPidXDODL();
          pID.setPartId   ( partID );
          pID.setPrimaryId( itemID );

          DKBlobDL xdo = new DKBlobDL( connection );
          xdo.setPidObject( pID );
          xdo.setContentClass( DKConstant.DK_CC_UNKNOWN );
          
          byte[] content = new byte[ num ];
          System.arraycopy( buffer, 0, content, 0, num );
          xdo.setContent( content );
          
          xdo.add();
        } 
      }
      
      return itemID;
    }
    finally{ MCRCM7ConnectionPool.instance().releaseConnection( connection ); }
  }

  protected void doDeleteContent( String storageID )
    throws Exception
  {
    DKDatastoreDL connection = MCRCM7ConnectionPool.instance().getConnection();
    try
    {
      MCRCM7Item item = new MCRCM7Item ( connection, indexClass, storageID );
      item.delete();
    }
    finally{ MCRCM7ConnectionPool.instance().releaseConnection( connection ); }
  }

  protected void doRetrieveContent( MCRFileReader file, OutputStream target )
    throws Exception
  {
    DKDatastoreDL connection = MCRCM7ConnectionPool.instance().getConnection();
    
    for( int partID = 1, sum = 0; sum < file.getSize(); partID++ )
    {
      try
      {
        DKPidXDODL pID = new DKPidXDODL();
        pID.setPartId( partID );
        pID.setPrimaryId( file.getStorageID() );

        DKBlobDL xdo = new DKBlobDL( connection );
        xdo.setPidObject( pID );
        xdo.retrieve();
        
        byte[] content = xdo.getContent();
        target.write( content, 0, content.length );
        sum += content.length;
      }
      catch( Exception exc )
      {
        String msg = "Error while retrieving part " + partID +  
                     " from CM item " + file.getStorageID();
        throw new MCRPersistenceException( msg, exc );
      }
      finally{ MCRCM7ConnectionPool.instance().releaseConnection( connection ); }
    }
  }
}
