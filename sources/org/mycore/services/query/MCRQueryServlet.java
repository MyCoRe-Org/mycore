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

import java.io.*;
import java.util.*;

import javax.servlet.http.*;
import javax.servlet.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.jdom.transform.*;
import org.jdom.*;
import org.mycore.common.*;
import org.mycore.datamodel.classifications.*;
import org.mycore.common.xml.MCRLayoutServlet;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.common.xml.MCRXMLSorter;
import org.mycore.common.xml.MCRXMLSortInterface;

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
// The configuration
private MCRConfiguration conf = null;

// Default Language (as UpperCase)
private String defaultLang = "";

 /**
  * The initialization method for this servlet. This read the default
  * language from the configuration.
  **/
  public void init() throws MCRConfigurationException
    {
    conf = MCRConfiguration.instance();
    String defaultLang = conf
      .getString( "MCR.metadata_default_lang", "en" ).toUpperCase();
    }

 /**
  * This method handles HTTP POST requests and resolves them to output.
  *
  * @param request the HTTP request instance
  * @param response the HTTP response instance
  * @exception IOException for java I/O errors.
  * @exception ServletException for errors from the servlet engine.
  **/
  public void doPost( HttpServletRequest  request, 
                      HttpServletResponse response )
    throws IOException, ServletException
  { doGet(request,response); }

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
    String view  = request.getParameter( "view");
    String ref   = request.getParameter( "ref");
    int status=0;

    String att_mode  = (String) request.getAttribute( "mode"  );
    if (att_mode!=null) { mode = att_mode; }
    String att_query = (String) request.getAttribute( "query" );
    if (att_query!=null) { query = att_query; }
    String att_type  = (String) request.getAttribute( "type"  );
    if (att_type!=null) { type = att_type; }
    String att_host  = (String) request.getAttribute( "hosts" );
    if (att_host!=null) { host = att_host; }
	String att_lang  = (String) request.getAttribute( "lang" );
	if (att_lang!=null) { lang = att_lang; }
	String att_view  = (String) request.getAttribute( "view" );
	if (att_view!=null) { view = att_view; }

    if( mode  == null ) { mode  = "ResultList"; }
    if( mode.equals("") ) { mode  = "ResultList"; }
    if( host  == null ) { host  = "local"; }
    if( host.equals("") ) { host  = "local"; }
    if( query == null ) { query = ""; }
    if( type  == null ) { return; }
    if( type.equals("") ) { return; }
    if (!conf.getBoolean("MCR.type_"+type.toLowerCase(),false)) { return; }
    if( lang  == null ) { lang  = defaultLang; }
    if (lang.equals("")) { lang = defaultLang; }
    type = type.toLowerCase();
    lang = lang.toUpperCase();
    
    if (view == null) view="";
    else view=view.toLowerCase();

    System.out.println("MCRQueryServlet : mode = "+mode);
    System.out.println("MCRQueryServlet : type = "+type);
    System.out.println("MCRQueryServlet : hosts = "+host);
    System.out.println("MCRQueryServlet : lang = "+lang);
    System.out.println("MCRQueryServlet : query = "+query);

	if (type.equals("document")){
		status = (request.getParameter( "status")!=null) ? Integer.parseInt(request.getParameter( "status")) : 0;
		String att_status  = (String) request.getAttribute( "status"  );
		if (att_status!=null) { status = Integer.parseInt(att_status); }
		boolean successor = ((status % 2) == 1) ? true : false;
		boolean predecessor   = (((status>>1) % 2) == 1) ? true : false;
		System.out.println("MCRQueryServlet : status = "+status);
		System.out.println("MCRQueryServlet : predecessor = "+predecessor);
		System.out.println("MCRQueryServlet : successor = "+successor);
	}

    // query for classifications
    if (type.equals("class")) {
      Properties parameters = MCRLayoutServlet.buildXSLParameters( request );
      String style = parameters.getProperty("Style",mode+"-class-"+lang);
      MCRQueryResult result = new MCRQueryResult();
      String squence = conf.getString("MCR.classifications_search_sequence",
        "remote-local");
      MCRXMLContainer resarray = new MCRXMLContainer();
      if (squence.equalsIgnoreCase("local-remote")) { 
        resarray = result.setFromQuery("local",type, query );
        if (resarray.size()==0) {
          resarray = result.setFromQuery(host,type, query ); }
        }
      else {
        resarray = result.setFromQuery(host,type, query ); 
        if (resarray.size()==0) {
          resarray = result.setFromQuery("local",type, query ); }
        } 
      if (resarray.size()==0) {
        throw new MCRException( 
          "No classification or category exists" ); }
      jdom = resarray.exportAllToDocument();
//System.out.println(new String(MCRUtils.getByteArray(jdom)));
      try {
        if (style.equals("xml")) {
          response.setContentType( "text/xml" );
          OutputStream out = response.getOutputStream();
          new org.jdom.output.XMLOutputter( "  ", true ).output( jdom, out );
          out.close();
          }
        else {
          request.setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
          request.setAttribute( "XSL.Style", style );
          RequestDispatcher rd = getServletContext()
            .getNamedDispatcher( "MCRLayoutServlet" );
          rd.forward( request, response );
          }
        }
      catch( Exception ex ) {
        System.out.println( ex.getClass().getName() );
        System.out.println( ex ); 
        }
      return;
      }

    // all other document types
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
          jdom = (org.jdom.Document) session.getAttribute( "CachedList" );
          type = (String)            session.getAttribute( "CachedType" );
        }
        else
          System.out.println("session for getAttribute is null");
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

    // prepare the stylesheet name
    Properties parameters = MCRLayoutServlet.buildXSLParameters( request );
    String style = parameters.getProperty("Style",mode+"-"+type+"-"+lang);
    System.out.println("Style = "+style);

    if (! cachedFlag)
    {
      MCRQueryResult result = new MCRQueryResult();
      MCRXMLContainer resarray = result.setFromQuery(host, type, query );
	  if (type.equals("document")){
		/** Status setzen für Dokumente **/
    	if (resarray.size()==1)
    	  resarray.setStatus(0,status);  
	  }
	  
      // create a new session if not already alive and encache result list
      if (mode.equals("ResultList"))
      {
        if (session == null)
          session = request.getSession(true);
        if (session != null)
        {
		  if (type.equals("document"))
		  	jdom = sort(resarray, lang.toLowerCase()).exportAllToDocument();
		  else
		    jdom = resarray.exportAllToDocument(); // no Resultlist for documents: why sort?
          session.setAttribute( "CachedList", jdom );
          session.setAttribute( "CachedType", type );
        }
        else
          System.out.println("session for setAttribute is null");
      }
      else
      	jdom = resarray.exportAllToDocument(); // no result list --> no sort needed
    }
    
    if ((view.equals("prev") || view.equals("next")) && (ref != null)){
    	/* change generate new query */
    	if (cachedFlag){
    		StringTokenizer refGet = 
    		   new StringTokenizer(this.getBrowseElementID(jdom,ref,view.equals("next")),"@");
    		if (refGet.countTokens() < 3)
    			throw new ServletException("MCRQueryServlet: Sorry \"refGet\" has not 3 Tokens: "+refGet);
    		String StrStatus=refGet.nextToken();
    		query=new StringBuffer("/mycoreobject[@ID='")
    		          .append(refGet.nextToken()).append("']").toString();
    		host=refGet.nextToken();
    		mode="ObjectMetadata";
    		type="document";
    		request.setAttribute("mode",mode);
    		request.removeAttribute("status");
    		request.setAttribute("status",StrStatus);
    		request.setAttribute("type",type);
    		request.setAttribute("hosts",host);
    		request.setAttribute("lang",lang);
    		request.setAttribute("query",query);
    		request.setAttribute("view","done");
    		System.out.println("MCRQueryServlet: sending to myself:" +
    		   "?mode="+mode+"&status="+StrStatus+"&type="+type+"&hosts="+host+
    		   "&lang="+lang+"&query="+query );
    		doGet(request,response);
    	}
    }
	else {
		try {
			if (style.equals("xml")) {
				response.setContentType( "text/xml" );
				OutputStream out = response.getOutputStream();
				new org.jdom.output.XMLOutputter( "  ", true ).output( jdom, out );
				out.close();
			}
			else {
				request.setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
				request.setAttribute( "XSL.Style", style );
				RequestDispatcher rd = getServletContext()
				                       .getNamedDispatcher( "MCRLayoutServlet" );
        		rd.forward( request, response );
        	}
      	}
    	catch( Exception ex ) {
      		System.out.println( ex.getClass().getName() );
      		System.out.println( ex );
    	}
	}
  }
  
  /**
   * <em>getStructure</em> retrieves all links outgoing from a single input document
   * and gives the result as a vector of link lists (link folders).
   *
   * @param inp                              the single input object as JDOM
   * @return Vector                          the vector of JDOM's each of which is
   *                                         a link folder
   */
  private static Vector getStructure (org.jdom.Document inp)
  {
    Vector inpStruct = new Vector ();
    return inpStruct;
  }
  
  /**
   * <em>getBrowseElementID</em> retrieves the previous or next element ID in
   * the ResultList and gives it combined with the host back as a String in the
   * following form:<br/>
   * status@id@host
   * @author Thomas Scheffler
   * @param jdom					cached ResultList
   * @param ref						the refering Document id@host
   * @param next					true for next, false for previous Document
   * @return String					String in the given form, representing the
   * 								searched Document.
   */
  private String getBrowseElementID(org.jdom.Document jdom, String ref, boolean next)
  	      throws ServletException, IOException{
  	org.jdom.Document tempDoc = (org.jdom.Document)jdom.clone();
    System.out.println("MCRQueryServlet: getBrowseElementID() got: "+ref);
	StringTokenizer refGet = new StringTokenizer(ref,"@");
	if (refGet.countTokens() < 2)
		throw new ServletException("MCRQueryServlet: Sorry \"ref\" has not 2 Tokens: "+ref);
	String id  =refGet.nextToken();
	String host=refGet.nextToken();
	List elements = tempDoc.getRootElement()
	                      .getChildren(MCRXMLContainer.TAG_RESULT);
	org.jdom.Element search=null;
	org.jdom.Element prev=null;
	while(!elements.isEmpty()){
		search=(Element)elements.get(0);
		if (((Element)search).getAttributeValue("id").equals(id) &&
		    ((Element)search).getAttributeValue("host").equals(host))
			if (next){
				search=(Element)elements.get(1);
				elements.clear();
			}
			else {
				search=prev;
				elements.clear();
			}
		else {
			prev=search;
			elements.remove(0);
		}
	}
	
	if (search==null)
		throw new ServletException("MCRQueryServlet: Sorry doesn't found searched document");
	int status = ((search.getAttributeValue(MCRXMLContainer.ATTR_SUCC).equals("true"))?1:0)
	            +((search.getAttributeValue(MCRXMLContainer.ATTR_PRED).equals("true"))?2:0);
	id=search.getAttributeValue("id");
	host=search.getAttributeValue("host");
	String result=new StringBuffer().append(status).append('@').append(id)
	.append('@').append(host).toString();
	System.out.println("MCRQueryServlet: getBrowseElementID() returns: "+result);
  	return result;
  }
  private MCRXMLContainer sort(MCRXMLContainer xmlcont, String lang){
  	MCRXMLSorter sorter=new MCRXMLSorter();
  	/*maybe here should be a propertie used
  	 * XPath Expression can be relative to mcr_result
  	 */
  	sorter.addSortKey("./*/*/*/title[lang('"+lang+"')]");
  	xmlcont.sort(sorter);
  	return xmlcont;
  }
}
