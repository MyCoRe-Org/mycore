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

package org.mycore.backend.remote;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.mycore.common.*;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.common.xml.MCRQueryInterface;
import org.mycore.common.xml.MCRQueryResultArray;


/**
 * This class provides a Servlet for remote Querying for distributed
 * queries in MyCoRe,
 *
 * @author Mathias Zarick
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRRemoteQueryServlet extends HttpServlet {

/**
 * This methode get a distributed query with the GET methode and response
 * with the answer of this query from the local host. 
 *
 * @param request the HTTP request instance
 * @param response the HTTP response instance
 * @exception IOException for an I/O error
 * @exception ServletException for a servlet error
 **/
public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {  
  response.setContentType("text/xml");
  PrintWriter out = response.getWriter();
  byte [] queryResult=(new String("")).getBytes();
  String type = request.getParameter("type");
  String hosts = request.getParameter("hosts");
  if (hosts == null) hosts = request.getServerName();
  String query = request.getParameter("query");
  try {
    // start the local query 
    MCRQueryResultArray result = new MCRQueryResultArray();
    if (type.equalsIgnoreCase("class")) {
      MCRClassification cl = new MCRClassification();
      org.jdom.Document jdom = cl.search(query);
      if (jdom != null) {
        org.jdom.Element el = jdom.getRootElement();
        String id = el.getAttributeValue("ID");
        MCRQueryResultArray res = new MCRQueryResultArray();
        res.add("local",id,1,el);
        result.importElements(res);
        }
      }
    else {
      MCRConfiguration config = MCRConfiguration.instance();
      int vec_length = config.getInt("MCR.query_max_results",10);
      String persist_type = config.getString("MCR.persistence_type","cm7");
      String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
        "_query_name";
      MCRQueryInterface mcr_query = 
        (MCRQueryInterface)config.getInstanceOf(proppers);
      result = mcr_query.getResultList(query,type,vec_length);
      }
    for (int i=0; i<result.size();i++) { result.setHost(i,hosts); }
    try {
      queryResult = result.exportAllToByteArray(); }
    catch(IOException e) {}
    }
  catch (MCRException mcre) {
       mcre.printStackTrace(System.err);
       queryResult=(new String("<mcr_results></mcr_results>")).getBytes(); }
  out.print(new String(queryResult));
  out.close();
  }

/**
 * This methode get a distributed query with the POST methode and response
 * with the answer of this query from the local host. 
 *
 * @param request the HTTP request instance
 * @param response the HTTP response instance
 * @exception IOException for an I/O error
 * @exception ServletException for a servlet error
 **/
public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  { doGet(request, response); }

}

