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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.common.*;

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
  protected final static MCRCache sessions = new MCRCache( 100 );

  public void init()
  {
    super.init();
    MCREditorResolver.init( getServletContext(), getBaseURL() );
  }

  public void doGetPost( MCRServletJob job )
	throws ServletException, java.io.IOException
  {
    HttpServletRequest  req = job.getRequest();
    HttpServletResponse res = job.getResponse();
  
    MCRRequestParameters parms = new MCRRequestParameters( req );
    
    String action = parms.getParameter( "_action" );
    if( "start.session".equals( action ) )
      processStartSession( req, res );
    else if( "load.session".equals( action ) )
      processLoadSession( req, res );
    else if( "show.popup".equals( action ) )
      processShowPopup( req, res );
    else if( "submit".equals( action ) )
      processSubmit( req, res, parms );
    else
      res.sendError( HttpServletResponse.SC_BAD_REQUEST );
  }

  private void processShowPopup( HttpServletRequest  req, 
                                 HttpServletResponse res )
	throws ServletException, java.io.IOException
  {
    String sessionID = req.getParameter( "_session" );
    String ref       = req.getParameter( "_ref" );

    logger.info( "Editor session " + sessionID + " show popup " + ref );
	
    Element editor = (Element)( sessions.get( sessionID ) );
    Element popup  = MCREditorDefReader.findElementByID( ref, editor );
    Element clone = (Element)( popup.clone() );
	
    sendToDisplay( req, res, new Document( clone ) );
  }

  private void processLoadSession( HttpServletRequest  req,
                                   HttpServletResponse res )
    throws ServletException, java.io.IOException
  {
    String sessionID = req.getParameter( "_session" );
    logger.info( "Editor session " + sessionID + " reload form data" );

    Element editor = (Element)( sessions.get( sessionID ) );

    req.setAttribute( "XSL.Style", "xml" );
    sendToDisplay( req, res, editor.getDocument() );
  }
  
  private void processStartSession( HttpServletRequest  req, 
                                    HttpServletResponse res )
    throws ServletException, java.io.IOException
  {
    String uri = req.getParameter( "_uri" );
    String ref = req.getParameter( "_ref" );
    String key = req.getParameter( "_requestParamKey" );

    logger.info( "Editor start editor session from " + ref + "@" + uri );

    Map requestParameters = getRequestParameters( key );
    Element param  = getTargetParameters( requestParameters );
    Element editor = MCREditorDefReader.readDef( uri, ref );
    if( param != null ) editor.addContent( param );

    MCREditorSubmission sub = MCREditorSourceReader.readSource( editor, requestParameters ); 
    if( sub != null )
    {
      editor.addContent( sub.buildInputElements()  );
      editor.addContent( sub.buildRepeatElements() );
    }

    String sessionID = buildSessionID();
    sessions.put( sessionID, editor );
    editor.setAttribute( "session", sessionID );

    logger.info( "Editor session " + sessionID + " created" );

    req.setAttribute( "XSL.Style", "xml" );
    sendToDisplay( req, res, new Document( editor ) );
  }

  private Map getRequestParameters( String key )
  { 
    MCRCache cache = MCRSessionMgr.getCurrentSession().requestParamCache;
    Map parameters = (Map)( cache.get( key ) );
    cache.clear();
    return parameters;
  }

  private Element getTargetParameters( Map parameters )
  {
    if( parameters == null ) return null;

    Element tps = new Element( "target-parameters" );
    Iterator keys = parameters.keySet().iterator();
    while( keys.hasNext() )
    {
      String key = (String)( keys.next() );
      if( key.startsWith( "XSL." ) ) continue;

      String[] values = (String[])( parameters.get( key ) );
      for( int i = 0; ( values != null ) && ( i < values.length ); i++ )
      {
        logger.debug( "Editor target parameter " + key + "=" + values[ i ] );
        Element tp = new Element( "target-parameter" );
        tp.setAttribute( "name", key );
        tp.addContent( values[ i ] );
        tps.addContent( tp );
      }
    }
    return tps;
  }

  private static Random random = new Random();

  private static synchronized String buildSessionID()
  {
    StringBuffer sb = new StringBuffer();
    sb.append( Long.toString( System.currentTimeMillis(), 36 ) );
    sb.append( Long.toString( random.nextLong(), 36 ) );
    sb.reverse();
    return sb.toString();
  }
  
  private void processSubmit( HttpServletRequest req, HttpServletResponse res, 
                              MCRRequestParameters parms )
	throws ServletException, java.io.IOException
  {
    String sessionID = parms.getParameter( "_session" );
    Element editor = (Element)( sessions.get( sessionID ) );

    String button = null;
      
    for( Enumeration e = parms.getParameterNames(); e.hasMoreElements(); )
    {
      String name = (String)( e.nextElement() );
      if( name.startsWith( "_p-" ) || name.startsWith( "_m-" ) ||
          name.startsWith( "_u-" ) || name.startsWith( "_d-" ) )
      {
        button = name;
        break;
      }
    } 

    if( button == null )
    { 
      logger.info( "Editor session " + sessionID + " submitting form data" );
      processTargetSubmission( req, res, parms, editor );
    }
    else
    {
      int pos = button.lastIndexOf( "-" );
  
      String action = button.substring( 1, 2 );
      String path   = button.substring( 3, pos );
      int    nr     = Integer.parseInt( button.substring( pos + 1, button.length() - 2 ) );

      logger.debug( "Editor action " + action + " " + nr + " " + path );

      editor.removeChild( "input" );
      editor.removeChild( "repeats" );

      MCREditorSubmission sub = new MCREditorSubmission( parms, editor );

      if( "p".equals( action ) )
        sub.doPlus( path, nr );
      else if( "m".equals( action ) )
        sub.doMinus( path, nr );
      else if( "u".equals( action ) )
        sub.doUp( path, nr );
      else if( "d".equals( action ) )
        sub.doUp( path, nr + 1 );

      editor.addContent( sub.buildInputElements()  );
      editor.addContent( sub.buildRepeatElements() );

      // Redirect to webpage to reload editor form
      StringBuffer sb = new StringBuffer(  getBaseURL() );
      sb.append( parms.getParameter( "_webpage" ) );
      sb.append( "?XSL.editor.session.id=" );
      sb.append( sessionID );

      logger.debug( "Editor redirect to " + sb.toString() );
      res.sendRedirect( sb.toString() );
    }
  }

  private void processTargetSubmission( 
    HttpServletRequest req, HttpServletResponse res,
    MCRRequestParameters parms, Element editor )
    throws ServletException, java.io.IOException
  {    
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
    throws IOException, UnsupportedEncodingException
  {
    res.setContentType( "text/html; charset=UTF-8" );
    PrintWriter pw = res.getWriter();

    pw.println( "<html><body><p><pre>" );

    for( int i = 0; i < sub.getVariables().size(); i++ )
    {
      MCREditorVariable var = (MCREditorVariable)( sub.getVariables().get( i ) );
      pw.println( var.getPath() + " = " + var.getValue() );
      FileItem file = var.getFile();
      if( file != null )
      {
        pw.println( "      is uploaded file " + file.getContentType() + 
                    ", "  + file.getSize() + " bytes" );
      }
    }

    pw.println( "</pre></p><p>" );

    XMLOutputter outputter = new XMLOutputter();
    Format fmt = Format.getPrettyFormat();
    fmt.setLineSeparator( "\n" );
    fmt.setOmitDeclaration( true );
    outputter.setFormat( fmt );

    Element pre = new Element( "pre" );
    pre.addContent( outputter.outputString( sub.getXML() ) );
    outputter.output( pre, pw );

    pw.println( "</p></body></html>" );
    pw.close();
  }
}

