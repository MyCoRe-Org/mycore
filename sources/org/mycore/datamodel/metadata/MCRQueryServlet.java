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
 * @author Mathias Hegner
 * @version $Revision$ $Date$
*/
public class MCRQueryServlet extends HttpServlet 
{
// Default Language (as UpperCase)
private String defaultLang = "";

 /**
  * The initialization method for this servlet. This read the default
  * language from the configuration.
  **/
  public void init() throws MCRConfigurationException
    {
    String defaultLang = MCRConfiguration.instance()
      .getString( "MCR.metadata_default_lang", "en" ).toUpperCase();
    }

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

    boolean cachedFlag = false;
    HttpSession session = null;
    org.jdom.Document jdom = null;

    String mode  = request.getParameter( "mode"  );
    String query = request.getParameter( "query" );
    String type  = request.getParameter( "type"  );
    String host  = request.getParameter( "hosts" );
    String lang  = request.getParameter( "lang" );

    System.out.println("MCRQueryServlet : mode = "+mode);
    System.out.println("MCRQueryServlet : type = "+type);
    System.out.println("MCRQueryServlet : hosts = "+host);
    System.out.println("MCRQueryServlet : lang = "+lang);
    System.out.println("MCRQueryServlet : query = "+query);

    if( mode  == null ) mode  = "ResultList";

    if (mode.equals("CachedResultList"))
    {
      cachedFlag = true;
      mode = "ResultList";
    }

    if (mode.equals("ResultList"))
      session = request.getSession(false);

    if (cachedFlag)
    {
      // retrieve result list from session cache
      try
      {
        if (session != null)
        {
          jdom = (org.jdom.Document) session.getValue("CachedList");
          type = (String) session.getValue("CachedType");
        }
        else
          System.out.println("session for getValue is null");
        if (jdom == null)
          System.out.println("jdom could not be retrieved from session cache");
        if (type == null)
          System.out.println("type could not be retrieved from session cache");
      }
      catch (Exception exc)
      {
        System.out.println(exc.getClass().getName());
        System.out.println(exc);
      }
    }

    if( host  == null ) host  = "local";
    if( query == null ) query = "";
    if( type  == null ) return;
    if( lang  == null ) lang  = defaultLang; else { lang = lang.toUpperCase(); }

    // prepare the stylesheet name
    String style = mode + "-" + type+ "-" + lang;

    try
    {
      if (! cachedFlag)
      {
        MCRQueryResult result = new MCRQueryResult();
        MCRQueryResultArray resarray = result.setFromQuery(host, type, query );

        jdom = resarray.exportAllToDocument();

        // create a new session if not already alive and encache result list
        if (mode.equals("ResultList"))
        {
          if (session == null)
            session = request.getSession(true);
          if (session != null)
          {
            session.putValue("CachedList", jdom);
            session.putValue("CachedType", type);
          }
          else
            System.out.println("session for putValue is null");
        }
      }

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

