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

package org.mycore.frontend.editor2;

import org.mycore.common.*;
import org.mycore.frontend.servlets.*;

import org.apache.log4j.*;
import org.apache.commons.fileupload.*;

import org.jdom.output.*;
import org.jdom.*;

import java.net.*;
import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This servlet handles form submissions from
 * MyCoRe XML Editor pages and converts the submitted data
 * into a JDOM XML document for further processing. It can
 * also handle file uploads.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
public class MCREditorServlet extends MCRServlet
{
  protected final static Logger logger = Logger.getLogger(  MCREditorServlet.class );

  public void doGetPost( MCRServletJob job )
    throws ServletException, java.io.IOException
  {
    HttpServletRequest req  = job.getRequest();
    HttpServletResponse res = job.getResponse();

    MCRRequestParameters parms = new MCRRequestParameters( req );

    String  editorID = parms.getParameter( "_editorID" );
    Element editor   = getEditorDefinition( editorID );

    MCREditorSubmission sub = new MCREditorSubmission( parms, editor );

    // If there is no input, handle as if "cancel" button was pressed
    if( sub.getVariables().size() == 0 )
    {
      String cancelURL = parms.getParameter( "_cancelURL" );
      if( cancelURL != null ) res.sendRedirect( cancelURL );
      return;
    }

    String targetType = parms.getParameter( "_target-type" );
    if( targetType.equals( "servlet" ) )
      sendToServlet( req, res, sub );
    else if( targetType.equals( "url" ) )
      sendToURL( req, res );
    else if( targetType.equals( "debug" ) )
      sendToDebug( req, res, sub );
    else if( targetType.equals( "display" ) )
      sendToDisplay( req, res, sub.getXML() );
  }
  
  private void sendToServlet( HttpServletRequest req, HttpServletResponse res, MCREditorSubmission sub )
    throws IOException, ServletException
  {
    String name = sub.getParameters().getParameter( "_target-name" );
    String url  = sub.getParameters().getParameter( "_target-url"  );
    
    RequestDispatcher rd = null;
    if( ( name != null ) && ( name.trim().length() > 0 ) )
      rd = getServletContext().getNamedDispatcher( name );
    else if( ( url != null ) && ( url.trim().length() > 0 ) )
      rd = getServletContext().getRequestDispatcher( url );
      
    if( rd != null )
    {
      req.setAttribute( "MCREditorSubmission", sub );
      rd.forward( req, res );
    }
  }

  private void sendToURL( HttpServletRequest req, HttpServletResponse res )
    throws IOException
  {
    StringBuffer url = new StringBuffer( req.getParameter( "_target-url" ) );
    url.append( '?' ).append( req.getQueryString() );
    res.sendRedirect( url.toString() );
  }

  private void sendToDisplay( HttpServletRequest req, HttpServletResponse res, Document xml )
    throws IOException, ServletException
  {
    req.setAttribute( "MCRLayoutServlet.Input.JDOM", xml );
    RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
    rd.forward( req, res );
  }

  private void sendToDebug( HttpServletRequest req, HttpServletResponse res, MCREditorSubmission sub )
    throws IOException
  {
    res.setContentType( "text/plain; charset=UTF-8" );
    PrintWriter pw = new PrintWriter( res.getOutputStream() );

    for( int i = 0; i < sub.getVariables().size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( sub.getVariables().get( i ) );
      pw.println( var.getName() + " = " + var.getValue() );
      FileItem file = var.getFile();
      if( file != null )
      {
        pw.println( "      is uploaded file " + file.getContentType() + 
                    ", "  + file.getSize() + " bytes" );
      }
    }

    pw.println();
    pw.println();

    Document xml = sub.getXML();
    
    XMLOutputter outputter = new XMLOutputter();
    outputter.setEncoding( "UTF-8" );
    outputter.setNewlines( true );
    outputter.setIndent( "  " );
    outputter.output( xml, pw );

    pw.close();
  }

  private Element getEditorDefinition( String ID )
  {
    return loadEditorXML( "editor-" + ID + ".xml" );
  }

  protected static MCRCache editorCache = new MCRCache( 50 );

  private Element loadEditorXML( String fileName )
  {
    String name = "/editor/" + fileName;
    String path = getServletContext().getRealPath( name );
    File   file = new File( path );
    long   time = file.lastModified();

    Element editor = (Element)( editorCache.getIfUpToDate( path, time ) );
    if( editor == null )
    {
      Document doc = null;
      try{ doc = new org.jdom.input.SAXBuilder().build( file ); }
      catch( Exception ex )
      {
        String msg = "Error while loading editor definition xml file " + path;
        throw new MCRConfigurationException( msg );
      }
      editor = doc.getRootElement().detach();
      editorCache.put( path, editor );
    }

    return editor;
  }
}
