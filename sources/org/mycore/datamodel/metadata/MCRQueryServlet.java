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
import mycore.common.*;

/**
 * This servlet provides a web interface to query
 * the datastore using XQueries and deliver the result list
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
*/
public class MCRQueryServlet extends HttpServlet 
{
  public void doGet( HttpServletRequest  request, 
                     HttpServletResponse response )
    throws IOException, ServletException
  {  
    String mode  = request.getParameter( "mode"  );
    String query = request.getParameter( "query" );
    String type  = request.getParameter( "type"  );
    String where = request.getParameter( "where" );

    System.out.println( "mode  = " + mode  );
    System.out.println( "query = " + query );
    System.out.println( "type  = " + type  );
    System.out.println( "where = " + where );

    if( mode  == null ) mode  = "ResultList";
    if( where == null ) where = "local";
    if( query == null ) query = "";
    if( type  == null ) return; 

    MCRConfiguration config = MCRConfiguration.instance();

    ArrayList hostAliasList = new ArrayList
      ( config.getInt( "MCR.communication_max_hosts", 3 ) );
    hostAliasList.add( where );

    String style = mode + "-" + type;

    try
    {
      MCRQueryResult result = new MCRQueryResult( type );
      result.setFromQuery( hostAliasList, query );

      String xml = result.getResultArray().exportAll();

      Reader in = new StringReader( xml );

      org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
      org.jdom.Document jdom = builder.build( in );

      request.setAttribute( "jdom",  jdom  );
      request.setAttribute( "style", style );
      RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
      rd.forward( request, response );
    }
    catch( Exception ex )
    {
      System.out.println( ex.getClass().getName() );
      System.out.println( ex );
    }
  }
}

