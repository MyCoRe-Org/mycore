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

package mycore.datamodel;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import org.jdom.*;
import mycore.common.*;

/**
 * This servlet provides a web interface to query
 * the datastore using XQueries and deliver the result list
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
*/
public class MCRQueryServlet extends HttpServlet 
{
 /**
  * This method handles HTTP GET requests and resolves them to output.
  *
  * @param request the HTTP request instance
  * @param response the HTTP response instance
  * @exception IOException for java I/O errors.
  * @exception ServletException for errors from the servlet engine.
  **/
  public void doGet( HttpServletRequest  request, 
                     HttpServletResponse response )
    throws IOException, ServletException
  {  
    String mode  = request.getParameter( "mode"  );
    String query = request.getParameter( "query" );
    String type  = request.getParameter( "type"  );
    String host  = request.getParameter( "hosts" );

    if( mode  == null ) mode  = "ResultList";
    if( host  == null ) host  = "local";
    if( query == null ) query = "";
    if( type  == null ) return; 

    // prepare the stylesheet name
    String style = mode + "-" + type;

    try
    {
      MCRQueryResult result = new MCRQueryResult();
      MCRQueryResultArray resarray = result.setFromQuery(host, type, query );

      org.jdom.Document jdom = resarray.exportAllToDocument();

      request.setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
      request.setAttribute( "XSL.Style", style );

      RequestDispatcher rd = getServletContext()
        .getNamedDispatcher( "MCRLayoutServlet" );

      rd.forward( request, response );
    }
    catch( Exception ex )
    {
      System.out.println( ex.getClass().getName() );
      System.out.println( ex );
    }
  }
}

