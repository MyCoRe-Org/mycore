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
import java.io.*;
import java.net.*;
import java.text.*;
import com.enterprisedt.net.ftp.*;

/**
 * This class implements the MCROldContentStore interface to store the content of
 * MCROldFile objects on a Real Server. This allows the content to be 
 * streamed. This implementation uses FTP to manage the files in Real Server.
 * The FTP connection parameters are configured in mycore.properties:
 *
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.Hostname       Hostname of Real Server
 *   MCR.IFS.ContentStore.<StoreID>.FTPPort        FTP port of Real Server, default is 21
 *   MCR.IFS.ContentStore.<StoreID>.UserID         User ID for FTP connections, e. g. vsloader
 *   MCR.IFS.ContentStore.<StoreID>.Password       Password for this user
 *   MCR.IFS.ContentStore.<StoreID>.BaseDirectory  Directory on server where content is stored
 *   MCR.IFS.ContentStore.<StoreID>.DebugFTP       If true, FTP debug messages are written to stdout, default is false
 * </code>
 *
 * @author Frank Lützenkirchen 
 * @deprecated USED BY MILESS 1.3 ONLY!
 * @version $Revision$ $Date$
 *
 * @see MCROldAVExtRealServer8
 */
public class MCROldCStoreRealServer8 implements MCROldContentStore
{ 
  /** Hostname of Real Server */
  protected String host;
  
  /** FTP Port of Real Server host */
  protected int port;

  /** User ID for FTP login */
  protected String user;

  /** Password for FTP login */
  protected String password;
  
  /** Base directory on Real Server where content is stored */
  protected String baseDir;

  /** If true, FTP debug messages are written to stdout */
  protected boolean debugFTP;

  /** DateFormat used to construct new unique IDs */
  protected DateFormat formatter;

  /** The last ID that was constructed */
  protected String lastID;
  
  /** The unique store ID for this MCROldContentStore implementation */
  protected String storeID;

  /** FTP Return codes if mkdir is successful in our interpretation */
  protected final static String[] mkdirOK = { "257", "521", "550" };

  /** FTP Return codes if rmdir is successful in our interpretation */
  protected final static String[] rmdirOK = { "250", "550" };
  
  /** 
   * Creates a new MCROldCStoreRealServer8 instance. This instance has to 
   * be initialized by calling init() before it can be used.
   */
  public MCROldCStoreRealServer8()
  {
    formatter = new SimpleDateFormat( "yyyyMMdd_HHmmss" );
    lastID    = "YYYYMMDD_HHMMSS";
  }
  
  public void init( String storeID )
  {
    this.storeID = storeID;
    String prefix = "MCR.IFS.ContentStore." + storeID + ".";
    
    MCRConfiguration config = MCRConfiguration.instance();  
      
    host      = config.getString( prefix + "Hostname"        );
    port      = config.getInt   ( prefix + "FTPPort", 21     );
    user      = config.getString( prefix + "UserID"          );
    password  = config.getString( prefix + "Password"        );
    baseDir   = config.getString( prefix + "BaseDirectory"   );
    debugFTP = config.getBoolean( prefix + "DebugFTP", false );
  }

  public String getID()
  { return storeID; }

  public String storeContent( MCROldFile file, MCRContentInputStream source )
    throws MCRPersistenceException
  {
    FTPClient connection = connect();
    try
    {
      StringBuffer storageID = new StringBuffer();  
      StringTokenizer st = new StringTokenizer( file.getOwnerID(), "_" );
      
      // Recursively create directories, each "_" marks a new subdirectory:
      while( st.hasMoreTokens() )
      {
        String dir = st.nextToken();
        connection.quote( "MKD " + dir, mkdirOK );
        connection.chdir( dir );
        storageID.append( dir ).append( "/" );
      }
      
      String fileID = buildNextID() + "." + file.getExtension();
      connection.put( source, fileID );
      storageID.append( fileID );
      
      return storageID.toString();
    }
    catch( Exception exc )
    {
      String msg = "Could not store content of file: " + file.getPath();
      throw new MCRPersistenceException( msg, exc );
    }
    finally{ disconnect( connection ); }
  }

  public void deleteContent( MCROldFile file )
    throws MCRPersistenceException
  {
    FTPClient connection = connect();
    try
    { 
      String storageID = file.getStorageID();
      connection.delete( storageID ); 
      
      // Recursively remove all directories that have been created, if empty:
      StringTokenizer st = new StringTokenizer( storageID, "/" );
      int numDirs = st.countTokens() - 1;
      String[] dirs = new String[ numDirs ];
      
      for( int i = 0; i < numDirs; i++ )
      {
        dirs[ i ] = st.nextToken();  
        if( i > 0 ) dirs[ i ] = dirs[ i - 1 ] + "/" + dirs[ i ];  
      }
      
      for( int i = numDirs; i > 0; i-- )
      { connection.quote( "RMD " + dirs[ i - 1 ], rmdirOK ); }
    }
    catch( Exception exc )
    {
      String msg = "Could not delete content of stored file: " + file.getStorageID();
      throw new MCRPersistenceException( msg, exc );
    }
    finally{ disconnect( connection ); }
  }

  public void retrieveContent( MCROldFile file, OutputStream target )
    throws MCRPersistenceException
  {
    FTPClient connection = connect();
    try
    { connection.get( target, file.getStorageID() );  }
    catch( Exception exc )
    {
      if( ! ( exc instanceof MCRPersistenceException ) )
      {
        String msg = "Could not get content of stored file to output stream";
        throw new MCRPersistenceException( msg, exc );
      }
      else throw (MCRPersistenceException)exc;
    }
    finally{ disconnect( connection ); }
  }
  
  /**
   * Constructs a new unique ID for storing content in Real Server
   */
  protected synchronized String buildNextID()
  {
    String ID = null;
    do{ ID = formatter.format( new Date() ); }
    while( ID.equals( lastID ) );
    return ( lastID = ID );
  }

  /**
   * Connects to Real Server host via FTP
   */
  protected FTPClient connect() 
    throws MCRPersistenceException
  {
    FTPClient connection = null;
    
    try
    {
      connection =  new FTPClient( host, port );
      connection.debugResponses( debugFTP );
      connection.login( user, password );
      connection.setType( FTPTransferType.BINARY );
    }
    catch( Exception exc )
    {
      String msg = "Could not connect to " + host + ":" + port + " via FTP";
      throw new MCRPersistenceException( msg, exc );
    }
    
    try
    { connection.chdir( baseDir ); }
    catch( Exception exc )
    {
      String msg = "Could not chdir to " + baseDir + " on FTP host " + host; 
      throw new MCRPersistenceException( msg, exc );
    }
    
    return connection;
  }

  /** 
   * Closes the FTP connection to Real Server host
   *
   * @param connection the FTP connection to close
   */
  protected void disconnect( FTPClient connection )
  {
    try{ connection.quit(); }
    catch( Exception ignored ){}
  }
}

