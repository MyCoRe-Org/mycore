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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.http.*;
import org.mycore.frontend.servlets.*;
import org.apache.log4j.Logger;

/**
 * This servlet implements the server side of communication with the upload applet
 * The content of the uploaded files is handled by an upload handler
 * derived from AppletCommunicator
 *
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRMCRUploadHandlerInterface
 */
public final class MCRUploadServlet
  extends MCRServlet
{
  
  static Logger logger = Logger.getLogger( MCRUploadServlet.class );

  public void doGetPost( MCRServletJob job )
    throws Exception
  {
    try{ invokeMethod( job ); }
    catch( Exception ex )
    {
      logger.debug( ex.getClass().getName() + " " + ex.getMessage() );
      sendException( job.getResponse(), ex );
      throw ex;
    }
  }

  protected void invokeMethod( MCRServletJob job )
    throws Exception
  {
    HttpServletRequest  req = job.getRequest();
    HttpServletResponse res = job.getResponse();

    String method = getStringParameter( req, "method" );
    
    if( method.equals( "redirecturl" ) )
    {
      String uploadId = this.getStringParameter( req, "uploadId" );
      String url = MCRUploadHandlerManager.instance().getHandle( uploadId ).getRedirectURL( );
      logger.info("REDIRECT " + url);
      res.sendRedirect( url );
      return;
    }
    else if( method.equals( "startDerivateSession String" ) )
    {
      String uploadId = this.getStringParameter( req, "uploadId" );
      uploadId = MCRUploadHandlerManager.instance().getHandle( uploadId ).startUpload();
      logger.info( "MCRUploadServlet start session " + uploadId );
      sendResponse( res, uploadId );
    }
    else if( method.equals( "createFile String" ) )
    {
      final String           path  = getStringParameter( req, "path" );
      
      logger.info("PATH: " + path);
      final String   uploadId = getStringParameter( req, "uploadId" );
      final String   md5      = getStringParameter( req, "md5" );

      logger.debug( "MCRUploadServlet receives file " + path + " with md5 " + md5 );
      if ( ! MCRUploadHandlerManager.instance().getHandle( uploadId ).acceptFile( path, md5 ) )
      {
        logger.debug( "Skip file " + path );
        sendResponse( res, "skip file" );
        return;
      }

      String servername = req.getServerName();

      logger.debug( "Applet wants to send content of file " + path );
      logger.debug( "Next trying to create a server socket for file transfer..." );

      final ServerSocket server = new ServerSocket( 0, 1, InetAddress.getByName( servername ) );
      logger.debug( "Server socket successfully created." );

      final int    port = server.getLocalPort();
      final String host = server.getInetAddress().getHostAddress();

      logger.debug( "Informing applet that server socket is ready." );

      sendResponse( res, host + ":" + port );

      // Define a separate Thread that accepts the file content,
      // otherwise Tomcat will not finish the HTTP response correctly
      Thread thread = new Thread( new Runnable()
      { 
        public void run()
        {
          Socket socket = null;

          try
          {
            logger.debug( "Listening on " + host + ":" + port + " for incoming data..." );
            socket = server.accept();

            logger.debug( "Client applet connected to socket now." );
        
            DataOutputStream dos = new DataOutputStream( socket.getOutputStream() );
            ZipInputStream   zis = new ZipInputStream  ( socket.getInputStream()  );

            logger.debug( "Constructed ZipInputStream and DataOutputStream, receiving data soon." );

            zis.getNextEntry();
       
            String erg = MCRUploadHandlerManager.instance().getHandle( uploadId ).receiveFile( path, zis );
            logger.debug( "Stored incoming file content under " + erg );
            logger.debug( "Informing applet about location where content was stored..." );

            dos.writeUTF( erg );
 
            logger.debug( "Sended acknowledgement to applet." );
            logger.debug( "File transfer completed successfully." );
          }
          catch( Exception ex )
          { 
            logger.error( "Exception while receiving and storing file content from applet:" );
            logger.error( ex.getClass().getName() + " " + ex.getMessage() ); 
          }
          finally
          { 
            try
            {
              if( socket != null ) socket.close();
              if( server != null ) server.close();
            }
            catch( Exception ignored ){}

            logger.debug( "Socket closed." );
          }
        } 
      } );

      thread.start(); // Starts separate thread that will receive and store file content
    }
    else if( method.equals( "endDerivateSession String" ) )
    {
      String   uploadId = this.getStringParameter( req, "uploadId" );
      MCRUploadHandlerInterface uploadHandler = MCRUploadHandlerManager.instance().getHandle( uploadId );
      uploadHandler.finishUpload( ); 
      sendResponse( res, "upload finished." );
    }
  }

  protected String getStringParameter( HttpServletRequest req, String label )
  {
    String[] values = req.getParameterValues( label );
    if( values == null )
      return null;
    else
      return values[ 0 ];
  }

  protected void sendException( HttpServletResponse res, Exception ex )
    throws Exception
  {
    Hashtable response = new Hashtable();
    response.put( "clname",  ex.getClass().getName()  );
    response.put( "strace",  getStackTrace( ex )      );
    if( ex.getLocalizedMessage() != null )
      response.put( "message", ex.getLocalizedMessage() );
    sendResponse( res, "upload/exception", response );
  }

  protected void sendResponse( HttpServletResponse res,
                               Object              value )
    throws Exception
  {
    Hashtable parameters = new Hashtable();
    if( value != null ) parameters.put( "return", value );
    sendResponse( res, "upload/response", parameters );
  }

  protected void sendResponse( HttpServletResponse res,
                               int                 value )
    throws Exception
  {
    Hashtable parameters = new Hashtable();
    parameters.put( "return", new Integer( value ) );
    sendResponse( res, "upload/response", parameters );
  }

  protected void sendResponse( HttpServletResponse res,
                               String              mime,
                               Hashtable           parameters )
    throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 );
    DataOutputStream      dos  = new DataOutputStream( baos );
    Enumeration           keys = parameters.keys();

    dos.writeUTF( mime );
    while( keys.hasMoreElements() )
    {
      String key   = (String)( keys.nextElement() );
      Object value = parameters.get( key );
      Class  cl    = value.getClass();
      dos.writeUTF( key          );
      dos.writeUTF( cl.getName() );

      if( cl == String.class )
        dos.writeUTF( ( (String )(value) ) );
      else if( cl == Integer.class )
        dos.writeInt( ( (Integer)(value) ).intValue() );
      else
        dos.writeUTF( value.toString() );
    }
    dos.close();

    byte[] response = baos.toByteArray();
    res.setContentType( mime );
    res.setContentLength( response.length );
    OutputStream out = res.getOutputStream();
    out.write( response, 0, response.length );
    out.close();
    res.flushBuffer();
  }

  protected String getStackTrace( Exception ex )
  {
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 );
      PrintWriter           pw   = new PrintWriter( baos );

      ex.printStackTrace( pw );
      pw.close();

      byte[]               bytes = baos.toByteArray();
      ByteArrayInputStream bais  = new ByteArrayInputStream( bytes );
      InputStreamReader    isr   = new InputStreamReader   ( bais  );
      BufferedReader       br    = new BufferedReader      ( isr   );
      return br.readLine();
    }
    catch( Exception ex2 ) {}
    return null;
  }
}
