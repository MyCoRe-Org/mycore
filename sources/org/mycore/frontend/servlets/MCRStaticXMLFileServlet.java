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

import java.net.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This servlet displays static *.xml files stored in the web application
 * by sending them to MCRLayoutServlet.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRStaticXMLFileServlet extends HttpServlet
{
  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, java.io.IOException
  {
    String requestedPath = request.getServletPath();
    URL url = null;
    
    try{ url = getServletContext().getResource( requestedPath ); }
    catch( MalformedURLException willNeverBeThrown ){}
    
    if( url == null )
    {
      String msg = "StaticXMLFileServlet could not find file " + requestedPath;
      throw new MCRUsageException( msg );
    }

    String path = getServletContext().getRealPath( requestedPath );
    File file = new File( path );

    String documentBaseURL = file.getParent() + File.separator;
    request.setAttribute( "XSL.DocumentBaseURL", documentBaseURL );
    request.setAttribute( "XSL.FileName", file.getName() );
    request.setAttribute( "XSL.FilePath", file.getPath() );
    request.setAttribute( "MCRLayoutServlet.Input.FILE", file );
    
    RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
    rd.forward( request, response );
  }
}
