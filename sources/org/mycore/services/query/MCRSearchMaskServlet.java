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

package org.mycore.services.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet provides a web interface to create a search mask and
 * analyze the output of them to create a XQuery and start the MCRQueryServlet
 *
 * @author Jens Kupferschmidt
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
*/
public class MCRSearchMaskServlet extends MCRServlet 
{

// logger
protected static Logger LOGGER=Logger.getLogger(MCRSearchMaskServlet.class.getName());

// The default mode for this class
String mode = "CreateSearchMask";

// The configuration XML files
org.jdom.Document jdom = null;

 /**
  * This method handles HTTP requests and resolves them to output.
  * The method can get two modi : 'CreateSearchMask' to generate a new
  * search mask or 'CreateQuery' to read the data from the search mask
  * and start the MCRQueryServlet.
  *
  * @exception IOException for java I/O errors.
  * @exception ServletException for errors from the servlet engine.
  **/
  public void doGetPost( MCRServletJob job )
    throws IOException, ServletException
  {  
    HttpServletRequest  request  = job.getRequest();
    HttpServletResponse response = job.getResponse();

  mode  = request.getParameter( "mode"  );
  if( mode  == null ) mode  = "CreateSearchMask";
  if (mode.equals("CreateSearchMask")) { createSearchMask(request,response); }
  if (mode.equals("CreateQuery")) { createQuery(request,response); }
  }

 /**
  * This method handles the CreateSearchMask mode.
  * It create the request for MCRLayoutServlet and starts them.
  *
  * @param request the HTTP request instance
  * @param response the HTTP response instance
  * @exception IOException for java I/O errors.
  * @exception ServletException for errors from the servlet engine.
  **/
  protected void createSearchMask( HttpServletRequest  request, 
                     HttpServletResponse response )
    throws IOException, ServletException
  {  
  String type  = request.getParameter( "type"  );
  String layout = request.getParameter( "layout"  );
  if( type  == null ) return;
  if( layout  == null ) layout = type;
  type = type.toLowerCase();

  String smc = CONFIG.getString( "MCR.searchmask_config_"+layout.toLowerCase());
  try {
    InputStream in = MCRSearchMaskServlet.class.getResourceAsStream( "/" + smc );
    if( in == null ) throw new MCRConfigurationException( "Can't read config file " + smc );
    jdom = new org.jdom.input.SAXBuilder().build( in );
    }
  catch (org.jdom.JDOMException e) {
    throw new MCRException("SearchMaskServlet : Can't read config file "+
      smc+" or it has a parse error.",e); }

  // prepare the stylesheet name
  String style = mode + "-" + layout+ "-" + MCRSessionMgr.getCurrentSession().getCurrentLanguage();

  // start Layout servlet
    request.setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
    request.setAttribute( "XSL.Style", style );
    RequestDispatcher rd = getServletContext()
      .getNamedDispatcher( "MCRLayoutServlet" );
    rd.forward( request, response );
  }

 /**
  * This method handles the CreateQuery mode.
  * It create the request for MCRQueryServlet and starts them.
  *
  * @param request the HTTP request instance
  * @param response the HTTP response instance
  * @exception IOException for java I/O errors.
  * @exception ServletException for errors from the servlet engine.
  **/
  protected void createQuery( HttpServletRequest  request, 
                     HttpServletResponse response )
    throws IOException, ServletException
  {  
  String type  = request.getParameter( "type"  );
  String layout = request.getParameter( "layout"  );
  String host = "";
  String hosts[] = request.getParameterValues( "hosts" );
  if (hosts != null) {
    StringBuffer hostsb = new StringBuffer("");
    for (int i=0;i<hosts.length;i++) {
      if (i != 0) hostsb.append(',');
      hostsb.append(hosts[i]);
      }
    host = hostsb.toString();
    }
  if( host.length() == 0 ) host  = "local";
  if( type  == null ) return;
  if( layout  == null ) layout = type;
  StringBuffer query = new StringBuffer("");

  String smc = CONFIG.getString( "MCR.searchmask_config_"+layout.toLowerCase());
  try {
    InputStream in = MCRSearchMaskServlet.class.getResourceAsStream( "/" + smc );
    if( in == null ) throw new MCRConfigurationException( "Can't read config file " + smc );
    jdom = new org.jdom.input.SAXBuilder().build(in);
    }
  catch (org.jdom.JDOMException e) {
    throw new MCRException("SearchMaskServlet : Can't read config file "+
      smc+" or it has a parse error."); }
  org.jdom.Element searchpage = jdom.getRootElement().getChild("searchpage");
  List element_list = searchpage.getChildren();
  int len = element_list.size();
  for (int i=0;i<len;i++) {
    org.jdom.Element element = (org.jdom.Element)element_list.get(i);
    if (!element.getName().equals("element")) { continue; }
    if (!element.getAttributeValue("type").equals("query")) { continue; }
    String name = element.getAttributeValue("name");
    String tempquery = (element.getAttributeValue("query")).replace('\'','\"');
    int tempfields = 1;
    try {
      tempfields = (new Integer(element.getAttributeValue("fields")))
        .intValue();
      }
    catch (NumberFormatException e) {
      throw new MCRException("SearchMaskServlet : The field attribute is "+
        "not a number.");
      }
    ArrayList param = new ArrayList();
    ArrayList varia = new ArrayList();
    for (int j=0;j<tempfields;j++) {
      StringBuffer sb = (new StringBuffer(name)).append(j+1);
      param.add(request.getParameter(sb.toString()));
      sb = (new StringBuffer("$")).append(j+1);
      varia.add(sb.toString());
      }
    // check to all attributes for a line are filled
    int k = 0;
    for (int j=0;j<param.size();j++) {
      LOGGER.debug(name+"   "+param.get(j)+"   "+varia.get(j));
      if (param.get(j) == null) { k=1; break; }
      if (((String)param.get(j)).trim().length() ==0 ) { k=1; break; }
      }
    if (k != 0) { continue; }
    for (int j=0;j<param.size();j++) {
      int start = 0;
      int l = ((String)varia.get(j)).length() ;
      k = tempquery.indexOf((String)varia.get(j),0);
      if (k == -1) {
        throw new MCRException("SearchMaskServlet : The query attribute "+
        "has not the elemnt "+((String)varia.get(j)));
        }
      StringBuffer qsb = new StringBuffer(128);
      while(k != -1) {
        if (tempquery.charAt(k-1)=='\'') {
          qsb.append(tempquery.substring(start,k-1)).append("\"")
             .append(((String)param.get(j))).append("\"")   ;
          start = k + 1 + l;
          }
        else{
          qsb.append(tempquery.substring(start,k))
             .append(((String)param.get(j))) ;
          start = k  + l;
          }
        k = tempquery.indexOf((String)varia.get(j),start);
        }
      qsb.append(tempquery.substring(start, tempquery.length()));
      tempquery = qsb.toString();
      }
    if (query.length() != 0) { query.append(" and "); }
    query.append(tempquery);
    }

  // start Query servlet
  	request.setAttribute( "saveResults", "true" );
    request.removeAttribute( "mode" );
    request.setAttribute( "mode", "ResultList" );
    request.removeAttribute( "type" );
    request.setAttribute( "type", type );
    layout = type;
    request.removeAttribute( "layout" );
    request.setAttribute( "layout", layout );
    request.removeAttribute( "hosts" );
    request.setAttribute( "hosts", host );
    request.removeAttribute( "lang" );
    request.removeAttribute( "query" );
    request.setAttribute( "query", query.toString() );
    RequestDispatcher rd = getServletContext()
      .getNamedDispatcher( "MCRQueryServlet" );
    rd.forward( request, response );
  }
}


