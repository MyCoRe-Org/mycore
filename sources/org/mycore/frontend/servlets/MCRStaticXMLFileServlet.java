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

package org.mycore.frontend.servlets;

import org.mycore.common.*;

import org.apache.log4j.*;

import java.net.*;
import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This servlet displays static *.xml files stored in the web application
 * by sending them to MCRLayoutServlet.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRStaticXMLFileServlet extends MCRServlet
{
  protected final static Logger logger = Logger.getLogger(  MCRStaticXMLFileServlet.class );

  public void doGetPost( MCRServletJob job )
    throws ServletException, java.io.IOException
  {
    String requestedPath = job.getRequest().getServletPath();
    logger.info( "MCRStaticXMLFileServlet " + requestedPath );
    URL url = null;
    
    try{ url = getServletContext().getResource( requestedPath ); }
    catch( MalformedURLException willNeverBeThrown ){}
    
    if( url == null )
    {
      String msg = "Could not find file " + requestedPath;
      job.getResponse().sendError( HttpServletResponse.SC_NOT_FOUND, msg );
      return;
    }

    String path = getServletContext().getRealPath( requestedPath );
    File file = new File( path );
    String documentBaseURL = file.getParent() + File.separator;

    // Store http request parameters into session, for later use
    if( "true".equals( job.getRequest().getParameter( "storeReqParam" ) ) )
    {
      logger.debug( "Storing http request parameters in MCRSession for later use" );
      Map map = new HashMap();
      map.putAll( job.getRequest().getParameterMap() );
      MCRSessionMgr.getCurrentSession().put( "StoredRequestParameters", map );
    }

    job.getRequest().setAttribute( "XSL.StaticFilePath", requestedPath );
    job.getRequest().setAttribute( "XSL.DocumentBaseURL", documentBaseURL );
    job.getRequest().setAttribute( "XSL.FileName", file.getName() );
    job.getRequest().setAttribute( "XSL.FilePath", file.getPath() );
    job.getRequest().setAttribute( "MCRLayoutServlet.Input.FILE", file );
    
    RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
    rd.forward( job.getRequest(), job.getResponse() );
  }
}

