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

package mycore.ifs;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import mycore.common.*;
import mycore.ifs.*;
import mycore.cm7.*;
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
 *   MCR.IFS.ContentStore.<StoreID>.Keyfield.Owner  Name of file owner keyfield
 *   MCR.IFS.ContentStore.<StoreID>.Keyfield.Path   Name of file path  keyfield
 * </code>
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCStoreContentManager7 extends MCRContentStoreBase implements MCRContentStore
{ 
  /** The maximum DKDDO size. When filesize is bigger, multiple DKDDOs are written */  
  protected int segmentSize;
  
  /** The index class to use to store content */
  protected String indexClass;
  
  /** The name of the keyfield that stores the MCRFile.getOwnerID() */
  protected String keyfieldOwner;
  
  /** The name of the keyfield that stores the MCRFile.getPath() */
  protected String keyfieldPath;
  
  public void init( String storeID )
  {
    super.init( storeID );
      
    MCRConfiguration config = MCRConfiguration.instance();  
      
    segmentSize   = config.getInt   ( prefix + "SegmentSize", 1024 * 1024 );
    indexClass    = config.getString( prefix + "IndexClass"     ); 
    keyfieldOwner = config.getString( prefix + "Keyfield.Owner" );
    keyfieldPath  = config.getString( prefix + "Keyfield.Path"  );
  }
  
  public String storeContent( MCRFile file, MCRContentInputStream source )
    throws MCRPersistenceException
  {
    DKDatastoreDL connection = MCRCM7ConnectionPool.instance().getConnection();
    try
    {
      MCRCM7Item item = new MCRCM7Item( connection, indexClass, DKConstant.DK_DOCUMENT );
      item.setKeyfield( keyfieldOwner, file.getOwnerID() );
      item.setKeyfield( keyfieldPath,  file.getPath()    );
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
    catch( Exception exc )
    {
      String msg = "Could not store content in ContentManager for file " + file.getPath();
      throw new MCRPersistenceException( msg, exc );
    }
    finally{ MCRCM7ConnectionPool.instance().releaseConnection( connection ); }
  }

  public void deleteContent( String storageID )
    throws MCRPersistenceException
  {
    DKDatastoreDL connection = MCRCM7ConnectionPool.instance().getConnection();
    try
    {
      MCRCM7Item item = new MCRCM7Item ( connection, indexClass, storageID );
      item.delete();
    }
    catch( Exception exc )
    {
      String msg = "Error deleting parts of ContentManager item " + storageID;
      throw new MCRPersistenceException( msg, exc );
    }
    finally{ MCRCM7ConnectionPool.instance().releaseConnection( connection ); }
  }

  public void retrieveContent( MCRFile file, OutputStream target )
    throws MCRPersistenceException
  {
    DKDatastoreDL connection = MCRCM7ConnectionPool.instance().getConnection();
    
    for( int partID = 1, sum = 0; sum < file.getSize(); partID++ )
    {
      try
      {
        DKPidXDODL pID = new DKPidXDODL();
        pID.setPartId   ( partID              );
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
