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

package mycore.nbn;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import mycore.common.*;

/**
 * This servlet resolves a given NBN URN from a HTTP request
 * and redirects the client to the URL that is stored for this NBN.
 * The URN can be the query string or the request path parameter.
 * If the URN is valid, but not local, the request is redirected to
 * the national URN resolver, as specified by the configuration parameter
 * MCR.NBN.TopLevelResolver.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRNBNResolver extends HttpServlet
{
  /** The URL of the non-local URN resolver script */
  protected String resolver;

  /** The object that implements the URN persistency functions */
  protected MCRNBNManager manager;

  /** Initializes the URN Resolver */    
  public void init()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    resolver = config.getString( "MCR.NBN.TopLevelResolver" );  

    Object object = config.getInstanceOf( "MCR.NBN.ManagerImplementation" );  
    manager = (MCRNBNManager)object;
  }

  /** Handles HTTP GET requests to resolve a given URN */    
  public void doGet( HttpServletRequest req, HttpServletResponse res )
    throws ServletException, IOException
  {
    String path  = req.getPathInfo();
    String param = req.getQueryString();

    System.out.println( path  );
    System.out.println( param );
    
    MCRNBN urn;
    
    if( path != null )
      urn = new MCRNBN( path.substring( 1 ) ); 
    else if( param != null )
      urn = new MCRNBN( param );
    else
    {    
      res.sendError( res.SC_BAD_REQUEST );
      return;
    }
    
    if( ! urn.isValid() )
    {    
      res.sendError( res.SC_BAD_REQUEST );
      return;
    }
    
    if( ! urn.isLocal() )
    {    
      res.sendRedirect( resolver + urn.getNBN() );
      return;
    }
    
    String url = manager.getURL( urn );
    if( url == null )
      res.sendError( res.SC_NOT_FOUND );
    else
      res.sendRedirect( url );
  }
}
