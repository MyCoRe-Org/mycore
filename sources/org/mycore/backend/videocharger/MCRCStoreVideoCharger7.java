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

import mycore.common.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import com.enterprisedt.net.ftp.*;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects in IBM VideoCharger Server. This allows the content to be 
 * streamed. This implementation uses FTP to manage the files in VideoCharger.
 * The FTP connection parameters are configured in mycore.properties:
 *
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.Hostname   Hostname of VideoCharger Server
 *   MCR.IFS.ContentStore.<StoreID>.FTPPort    Port of VideoCharger FTP interface, default is 4324
 *   MCR.IFS.ContentStore.<StoreID>.UserID     User ID for FTP connections, e. g. vsloader
 *   MCR.IFS.ContentStore.<StoreID>.Password   Password for this user
 *   MCR.IFS.ContentStore.<StoreID>.DebugFTP   If true, FTP debug messages are written to stdout, default is false
 * </code>
 *
 * This class also provides a method to backup all assets stored in VideoCharger
 * to a directory.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 *
 * @see MCRAVExtVideoCharger7
 */
public class MCRCStoreVideoCharger7 implements MCRContentStore
{ 
  /** Hostname of VideoCharger server */
  protected String host;
  
  /** Port of VideoCharger server FTP interface */
  protected int port;

  /** User ID for FTP login */
  protected String user;

  /** Password for FTP login */
  protected String password;

  /** If true, FTP debug messages are written to stdout */
  protected boolean debugFTP;

  /** SimpleDateFormat used to construct new unique asset IDs */
  protected DateFormat formatter;

  /** The last ID that was constructed */
  protected String lastID;
  
  /** The unique store ID for this MCRContentStore implementation */
  protected String storeID;

  /** FTP Return code if "quote site avs attr" is successful */
  protected final static String[] ok = { "200" };

  /** 
   * Creates a new MCRCStoreVideoCharger7 instance. This instance has to 
   * be initialized by calling init() before it can be used.
   */
  public MCRCStoreVideoCharger7()
  {
    formatter = new SimpleDateFormat( "yyyyMMdd_HHmmss" );
    lastID    = "OWNERID_YYYYMMDD_HHMMSS";
  }
  
  public void init( String storeID )
  { 
    this.storeID = storeID;
    String prefix = "MCR.IFS.ContentStore." + storeID + ".";
      
    MCRConfiguration config = MCRConfiguration.instance();  
      
    host     = config.getString ( prefix + "Hostname"        );
    port     = config.getInt    ( prefix + "FTPPort", 4324   );
    user     = config.getString ( prefix + "UserID"          );
    password = config.getString ( prefix + "Password"        );
    debugFTP = config.getBoolean( prefix + "DebugFTP", false );
  }

  public String getID()
  { return storeID; }
 
  public String storeContent( MCRFile file, MCRContentInputStream source )
    throws MCRPersistenceException
  {
    String storageID = buildNextStorageID( file );

    FTPClient connection = connect();
    try
    {
      connection.quote( "site avs attr title=" + storageID, ok );
      connection.put( source, storageID );
      return storageID;
    }
    catch( Exception exc )
    {
      String msg = "Could not store content of file: " + file.getPath();
      throw new MCRPersistenceException( msg, exc );
    }
    finally{ disconnect( connection ); }
  }

  public void deleteContent( MCRFile file )
    throws MCRPersistenceException
  {
    FTPClient connection = connect();
    try
    { connection.delete( file.getStorageID() ); }
    catch( Exception exc )
    {
      String msg = "Could not delete content of stored file: " + file.getStorageID();
      throw new MCRPersistenceException( msg, exc );
    }
    finally{ disconnect( connection ); }
  }

  public void retrieveContent( MCRFile file, OutputStream target )
    throws MCRPersistenceException
  { retrieveContent( file.getStorageID(), target ); }
  
  protected void retrieveContent( String assetID, OutputStream target )
    throws MCRPersistenceException
  {
    FTPClient connection = connect();
    try
    { connection.get( target, assetID );  }
    catch( Exception exc )
    {
      if( ! ( exc instanceof MCRPersistenceException ) )
      {
        String msg = "Could not get stored content to output stream: " + assetID;
        throw new MCRPersistenceException( msg, exc );
      }
      else throw (MCRPersistenceException)exc;
    }
    finally{ disconnect( connection ); }
  }
  
  /**
   * Reads all assets stored in VideoCharger server and writes the contents
   * to a directory for backup. If the directory already contains an asset
   * with the same name, that assets is skipped and not backed up.
   *
   * @param storeID the store ID fo the VideoCharger store to be backed up
   * @param directory the local directory to write the assets to
   **/
  public static void backupContentTo( String storeID, String directory )
    throws MCRPersistenceException, IOException
  {
    MCRAVExtVideoCharger7 extender = new MCRAVExtVideoCharger7();
    extender.readConfig( storeID );
    String[] list = extender.listAssets();

    for( int i = 0; i < list.length; i++ )
    {
      System.out.println( "Backup of asset with ID = " + list[ i ] );
      
      File local = new File( directory, list[ i ] );
      if( local.exists() ) continue;
      
      FileOutputStream target = new FileOutputStream( local );
      new MCRCStoreVideoCharger7().retrieveContent( list[ i ], target );
      target.close();
    }
  }

  /**
   * Constructs a new unique ID for storing content in VideoCharger
   *
   * @param file the MCRFile to build an ID for
   */
  protected synchronized String buildNextStorageID( MCRFile file )
  {
    String ID = null;
    do
    { 
      StringBuffer buffer = new StringBuffer();
      buffer.append( file.getOwnerID() );
      buffer.append( "_" );
      buffer.append( formatter.format( new Date() ) );
      ID = buffer.toString(); 
    }
    while( ID.equals( lastID ) );
    return ( lastID = ID );
  }

  /**
   * Connects to IBM VideoCharger Server via FTP
   */
  protected FTPClient connect() 
    throws MCRPersistenceException
  {
    try
    {
      FTPClient connection =  new FTPClient( host, port );
      connection.debugResponses( debugFTP );
      connection.login( user, password );
      connection.setType( FTPTransferType.BINARY );
      return connection;
    }
    catch( Exception exc )
    {
      String msg = "Could not connect to " + host + ":" + port + " via FTP";
      throw new MCRPersistenceException( msg, exc );
    }
  }

  /**
   * Closes the FTP connection to VideoCharger server
   *
   * @param connection the FTP connection to close
   */
  protected void disconnect( FTPClient connection )
  {
    try{ connection.quit(); }
    catch( Exception ignored ){}
  }
}

