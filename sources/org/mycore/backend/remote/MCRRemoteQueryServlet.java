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

package mycore.communication;

import mycore.common.*;
import mycore.datamodel.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;


/**
 * This class provides a Servlet for remote Querying for distributed
 * queries in MyCoRe,
 *
 * @author Mathias Zarick
 * @version $Revision$ $Date$
*/
public class MCRRemoteQueryServlet extends HttpServlet {

    private boolean queryInputSyntaxIsCorrect(String input) {
    boolean result = false;
    if (input != null) result = (input.indexOf("***") > 0);
    return result;
    }

    private String extractType(String input) {
    return input.substring(0,input.indexOf('*'));
    }

    private String extractQuery(String input) {
    return input.substring(input.indexOf('*')+3);
    }


public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
{  response.setContentType("text/xml");
   PrintWriter out = response.getWriter();
   String queryResult="";
   String theInput = request.getParameter("request");
   System.out.println("request="+theInput);
   String host = request.getParameter("host");
   if (host == null) host = request.getServerName();
   try {
     if (queryInputSyntaxIsCorrect(theInput)) {
        // start the local query
        MCRConfiguration config = MCRConfiguration.instance();
        int vec_length = config.getInt("MCR.query_max_results",10);
        String type = extractType(theInput);
        String query = extractQuery(theInput);
        String proptype = "MCR.persistence_type_"+type;
        String persist_type = config.getString(proptype);
        String proppers = "MCR.persistence_"+persist_type.toLowerCase()+"_query_name";
        MCRQueryInterface mcr_query = (MCRQueryInterface)config.getInstanceOf(proppers);
        MCRQueryResultArray result = mcr_query.getResultList(query,type,vec_length);
        for (int i=0; i<result.size();i++)
          result.setHost(i,host);
        queryResult = result.exportAll();
     }
     else queryResult="";
   }
   catch (MCRException mcre) {
        mcre.printStackTrace(System.err);
        queryResult="<ERROR/>";
   }
   out.println(queryResult);

}

public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
{ doGet(request, response);
}

public void init() throws ServletException {
}

}

