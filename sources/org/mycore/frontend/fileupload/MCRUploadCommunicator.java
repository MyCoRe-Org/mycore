/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
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
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class is used by applets to communicate with the
 * corresponding MCRUploadServlet servlet to execute tasks
 * on the server. For example, the applet may invoke
 * the createDocument method and pass it a document instance
 * to create this document in the persistent datastore
 * on the server side. The MCRUploadCommunicator does
 * some marshalling etc. and sends the request to the
 * MCRUploadServlet servlet that does the job.
 *
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRUploadServlet
 */

public final class MCRUploadCommunicator
{
  protected static String URL;

  public static void setPeerURL( String peer )
  { URL = peer; }

  public void sendDerivate( String uploadId, File selectedFiles[] )
    throws Exception
  {
    int total = 0;
     
    Vector files = listFiles( selectedFiles )[ 0 ];
    for( int i = 0; i < files.size(); i++ )
    {
      File f = (File)( files.elementAt( i ) );
      total += (int)( f.length() );
    }

    total = (int)( (double)total * 1.124 ); 
    
    MCRUploadProgressMonitor.getDialog().startProgressBar( total );
    startDerivateSession( uploadId );
    MCRUploadProgressMonitor.getDialog().updateProgressBar( total / 100 ); 

    xloadFilesFrom( selectedFiles, uploadId );
    endDerivateSession( uploadId );

    MCRUploadProgressMonitor.getDialog().finishProgressBar();
    return;
  }

  
  /**
   * Imports all files from a given directory on the local filesystem, 
   * creates MCROldFiles for them and stores their content in the system.
   */
  public void xloadFilesFrom( File selectedFiles[], String uploadId )
    throws Exception
  {
    Vector[] list = listFiles( selectedFiles );
    if( list[ 0 ].size() == 0 )
      throw new IllegalArgumentException(
      "The directory you specified contains no files." );

    for( int i = 0; i < list[ 0 ].size(); i++ )
    {
      File   file = (File)  ( list[ 0 ].elementAt( i ) );
      String path = (String)( list[ 1 ].elementAt( i ) );

      createFile( path, file, uploadId );
    }
  }
  public void createFile( String path, File file, String uploadId )
    throws Exception
  {
    MCRUploadProgressMonitor.getDialog().setMessage( 
      "\u00dcbertrage Datei " + path + "..." );

    try
    {
      System.out.println( "---------------------------- Starting filetransfer ---------------------------" );

      Hashtable request = new Hashtable();
      request.put( "method", "createFile String" );
      request.put( "path",  path );
      request.put( "uploadId",  uploadId );
 
      InputStream source = new FileInputStream( file );
      String md5 = getMD5String( file );
      request.put( "md5", md5 );
      
      System.out.println( md5 );
      System.out.println( "Sending filename to server: " + path );
      String reply = (String)( send( request ) );
      if ( "skip file".equals( reply ) )
      {
        MCRUploadProgressMonitor.getDialog().setMessage( 
          "Datei bereits vorhanden " + path );
        return;
      }
      System.out.println( "Received reply from server." );

      StringTokenizer st = new StringTokenizer( reply, ":" );
      String host = st.nextToken();
      int    port = Integer.parseInt( st.nextToken() );
      System.out.println( "Server says we should connect to " + host + ":" + port );

      System.out.println( "Trying to create client socket..." );
      Socket socket = new Socket( host, port );
      System.out.println( "Socket created, connected to server." );

      ZipOutputStream zos = new ZipOutputStream( socket.getOutputStream() );
      DataInputStream dis = new DataInputStream( socket.getInputStream()  );
      System.out.println( "Created ZipOutputStream and DataInputStream." );

      zos.setLevel( Deflater.NO_COMPRESSION );
      ZipEntry ze = new ZipEntry( "content" );   
      zos.putNextEntry( ze );
      System.out.println( "Prepared ZipEntry." );

      int    num    = 0;
      byte[] buffer = new byte[ 65536 ];

      System.out.println( "Starting to send file content..." );
      while( ( num = source.read( buffer ) ) != -1 )
      {
        zos.write( buffer, 0, num );
        System.out.println( "Sended " + num + " bytes of file content." );
        MCRUploadProgressMonitor.getDialog().updateProgressBar( num );
      }
      zos.closeEntry();
      System.out.println( "Finished sending file content." );

      String storageID = dis.readUTF();
      System.out.println( "Received storage location from server: " + storageID );

      socket.close();
      System.out.println( "Socket closed, file transfer successfully completed." );
      
      return;
    }
    catch( Exception exc )
    {
      String exmsg = exc.getClass().getName() + " " + exc.getMessage();
      System.out.println( "Exception caught: " + exmsg );
      exc.printStackTrace();

      if( exc instanceof IOException )
      {
        IOException ioe = (IOException)exc;
        String msg = "\u00dcbertragungsfehler beim Senden der Datei: ";
        msg += ioe.getClass().getName() + " " + ioe.getMessage();
        MCRUploadProgressMonitor.getDialog().setMessage( msg );
      } 
      else if( exc instanceof MCRUploadException )
      {
        MCRUploadException sex = (MCRUploadException)exc;
        String msg = "Fehlermeldung des Servers: ";
        msg += sex.getServerSideClassName() + " " + sex.getMessage();
        MCRUploadProgressMonitor.getDialog().setMessage( msg );
      }
      else
      {
        String msg = "Fehler beim Senden der Datei: ";
        msg += exc.getClass().getName() + " " + exc.getMessage();
        MCRUploadProgressMonitor.getDialog().setMessage( msg );
      }

      throw exc;
    }
  }

  /**
   * Creates a list of all files in the filesystem below a given starting path.
   *
   * @param location a path leading to a file or directory
   */
  public static Vector[] listFiles( File selectedFiles[] )
    throws Exception
  {
    Vector[] list = new Vector[ 2 ];
    list[ 0 ] = new Vector();
    list[ 1 ] = new Vector();

    if ( null == selectedFiles || 0 == selectedFiles.length )
      return list;
    
    for (int i = 0; i< selectedFiles.length; i++)
    {
      File f = selectedFiles[i];
      if( ! f.exists() )
        throw new FileNotFoundException(
        "Datei oder Verzeichnis " + f.getPath() + " nicht gefunden!" );
      if( ! f.canRead() )
        throw new IOException(
        "Datei oder Verzeichnis " + f.getPath() + " nicht lesbar!" );

      if( f.isFile() )
      {
        list[ 0 ].addElement( f           );
        list[ 1 ].addElement( f.getName() );
      }
      else
      {
        Stack dirStack  = new Stack();
        Stack baseStack = new Stack();

        dirStack .push( f  );
//        baseStack.push( "" );
        baseStack.push( f.getName() + "/" );

        while( ! dirStack.empty() )
        {
          File   dir  = (File)  ( dirStack.pop()  );
          String base = (String)( baseStack.pop() );

          String[] files = dir.list();

          for( int j = 0; j < files.length; j++ )
          {
            f = new File( dir, files[ j ] );

            if( f.isFile() )
            { 
              list[ 0 ].addElement( f );
              list[ 1 ].addElement( base + files[ j ] );
            }
            else
            {
              dirStack .push( f );
              baseStack.push( base + files[ j ] + "/" );
            }
          }
        }
      }
    }

    return list; 
  }
  
  protected void startDerivateSession( String uploadId )
    throws IOException, MCRUploadException
  {
    Hashtable request = new Hashtable();
    request.put( "method", "startDerivateSession String" );
    request.put( "uploadId",    uploadId                 );
    send( request );
  }

  protected void endDerivateSession( String uploadId )
    throws IOException, MCRUploadException
  {
    Hashtable request = new Hashtable();
    request.put( "method", "endDerivateSession String" );
    request.put( "uploadId",    uploadId                 );
    String xml = (String)( send( request ) );
    return;
  }

  protected Object send( Hashtable parameters )
    throws IOException, MCRUploadException
  {
    Hashtable response = getResponse( doPost( parameters ) );
    return response.get( "return" );
  }

  protected InputStream doPost( Hashtable parameters )
    throws IOException
  {
    String data = encodeParameters( parameters, "UTF-8");
    String mime = "application/x-www-form-urlencoded";

    URLConnection connection = null;
    try{ connection = new URL( URL ).openConnection(); }
    catch( MalformedURLException ignored ) {} // will never happen if base URL is ok

    connection.setDoInput         ( true  );
    connection.setDoOutput        ( true  );
    connection.setUseCaches       ( false );
    connection.setDefaultUseCaches( false );
    connection.setRequestProperty( "Content-type",   mime );
    connection.setRequestProperty( "Content-length", String.valueOf( data.length() ) );

    DataOutputStream out = new DataOutputStream( connection.getOutputStream() );
    out.writeBytes( data );
    out.flush();
    out.close();

    return connection.getInputStream();
  }
  
  protected String encodeParameters( Hashtable parameters, String encoding ) throws UnsupportedEncodingException
  {
    StringBuffer data = new StringBuffer();
    Enumeration  e    = parameters.keys();

    while( e.hasMoreElements() )
    {
      String name  = (String) e.nextElement();
      String value = (String) parameters.get( name );

      data.append( URLEncoder.encode( name, encoding) )
          .append( "=" )
          .append( URLEncoder.encode( value, encoding ) )
          .append( "&amp;" );
    }
    data.setLength( data.length() - 5 );

    return data.toString();
  }

  protected Hashtable getResponse( InputStream is )
    throws IOException, MCRUploadException
  {
    DataInputStream dis   = new DataInputStream( is );
    String          mime  = dis.readUTF();
    byte[]          dummy = new byte[ 0 ];

    Hashtable response = new Hashtable();
    while( dis.read( dummy, 0, 0 ) != -1 )
    {
      String key    = dis.readUTF();
      String clname = dis.readUTF();
      Object value  = null;
      if( clname.equals( String.class.getName() ) )
        value = dis.readUTF();
      else if( clname.equals( Integer.class.getName() ) )
        value = new Integer( dis.readInt() );
      else
        value = dis.readUTF();
      response.put( key, value );
    }

    if( mime.equals( "upload/exception" ) )
    {
      String clname  = (String)( response.get( "clname"  ) );
      String message = (String)( response.get( "message" ) );
      String strace  = (String)( response.get( "strace"  ) );
      throw new MCRUploadException( clname, message, strace );
    }

    return response;
  }
  
  protected String getMD5String( File file ) throws Exception
  {
    // Obtain a message digest object.
    MessageDigest digest = MessageDigest.getInstance("MD5");

    InputStream source = new FileInputStream( file );
    // Calculate the digest for the given file.
    DigestInputStream in = new DigestInputStream( source, digest );
    byte[] buffer = new byte[8192];
    while (in.read(buffer) != -1)
      ;
    source.close();
    
    byte[] bytes = digest.digest();
    StringBuffer sb = new StringBuffer();
    for( int i = 0; i < bytes.length; i++ )
    {
      String sValue = "0" + Integer.toHexString(bytes[i]);
      sb.append( sValue.substring( sValue.length() - 2 ) );
    }
    return sb.toString();
  }
  
}

