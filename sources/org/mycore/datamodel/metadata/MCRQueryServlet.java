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
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
*/
public class MCRQueryServlet extends HttpServlet 
{
  // The list of hosts from the configuration
  private ArrayList remoteAliasList = null;

  /**
   * The methode initialized the servlet and read the host list
   * from the mycore.property configuration.
   *
   * @exception ServletException for errors from the servlet engine.
   **/
  public void init() throws ServletException
    {
    // read host list from configuration
    MCRConfiguration config = MCRConfiguration.instance();
    String hostconf = config.getString("MCR.communication_hostaliases",
      "local");
    remoteAliasList = new ArrayList();
    int i = 0;
    int j = hostconf.length();
    int k = 0;
    while (k!=-1) {
      k = hostconf.indexOf(",",i);
      if (k==-1) {
        remoteAliasList.add(hostconf.substring(i,j)); }
      else {
        remoteAliasList.add(hostconf.substring(i,k)); i = k+1; }
      }
    }

  /**
   * The methode get a request and resolve them to a output.
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

    System.out.println( "mode  = " + mode  );
    System.out.println( "query = " + query );
    System.out.println( "type  = " + type  );
    System.out.println( "hosts = " + host  );

    if( mode  == null ) mode  = "ResultList";
    if( host  == null ) host  = "local";
    if( query == null ) query = "";
    if( type  == null ) return; 

    // build host list from host string
    ArrayList hostAliasList = new ArrayList();
    int i = 0;
    int j = host.length();
    int k = 0;
    int n = 0;
    while (n!=-1) {
      n = host.indexOf(",",i);
      String hostname = "";
      if (n==-1) { hostname = host.substring(i,j); }
      else { hostname = host.substring(i,n); i = n+1; }
      if (hostname.equals("local")) {
        k = -1;
        for (int l=0;l<hostAliasList.size();l++) {
          if (((String)hostAliasList.get(l)).equals("local")) { 
            k = 0; break; }
          }
        if (k==-1) { hostAliasList.add("local"); }
        }
      else {
        if (hostname.equals("remote")) {
          for (int m=0;m<remoteAliasList.size();m++) {
            k = -1;
            for (int l=0;l<hostAliasList.size();l++) {
              if (((String)hostAliasList.get(l))
                .equals(remoteAliasList.get(m))) { k = 0; break; }
              }
            if (k==-1) { hostAliasList.add(remoteAliasList.get(m)); }
            }
          }
        else {
          k = -1;
          for (int m=0;m<remoteAliasList.size();m++) {
            if (((String)remoteAliasList.get(m)).equals(hostname)) { 
              k = 0; break; } }
          if (k==-1) {
            throw new MCRException( "The host name is not in the list."); }
          k = -1;
          for (int l=0;l<hostAliasList.size();l++) {
            if (((String)hostAliasList.get(l)).equals(hostname)) { 
              k = 0; break; } }
          if (k==-1) { hostAliasList.add(hostname); }
          }
        }
      }

    // print list for debug
    for (i=0;i<hostAliasList.size();i++) {
      System.out.println("Host : "+hostAliasList.get(i)); }
    System.out.println();

    // prepare the stylesheet name
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

