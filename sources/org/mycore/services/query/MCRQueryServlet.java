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
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This servlet provides a web interface to query
 * the datastore using XQueries and deliver the result list
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
*/
public class MCRQueryServlet extends HttpServlet 
{
// The configuration
private MCRConfiguration conf = null;

// Default Language (as UpperCase)
private String defaultLang = "";
private String sortType = "";
private static final String MCRSorterConfPrefix="MCR.XMLSorter";
private static final String MCRSorterConfDelim="\"+lang+\"";
private static final String MCRStdSorter="org.mycore.common.xml.MCRXMLSorter";
private static final String SortParam="SortKey";
private static final String InOrderParam="inOrder";
private boolean customSort=false;
private String SortKey;
private boolean inOrder=true;
private static Logger logger=Logger.getLogger(MCRQueryServlet.class);

 /**
  * The initialization method for this servlet. This read the default
  * language from the configuration.
  **/
  public void init() throws MCRConfigurationException
    {
    conf = MCRConfiguration.instance();
	PropertyConfigurator.configure(conf.getLoggingProperties());
    defaultLang = conf
      .getString( "MCR.metadata_default_lang", "en" ).toUpperCase();
    sortType = conf.getString("MCR.XMLSortType", "OutOfFunction");
    if (sortType.equals("OutOfFunction"))
    	throw new MCRException("Property MCR.XMLSortType undefined but needed for sorting and browsing!");
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
    HttpSession session = request.getSession(false); //if session exists;

    org.jdom.Document jdom = null;

    String mode  = getProperty(request, "mode"  );
    String query = getProperty(request, "query" );
    String type  = getProperty(request, "type"  );
    String lang  = getProperty(request, "lang" );

	//multiple host are allowed
	String[] hosts  = request.getParameterValues( "hosts" );
	String att_host  = (String) request.getAttribute( "hosts" );
	//dont't overwrite host if getParameter("hosts") was successful 
	String host="";
	if (att_host!=null && (hosts == null ||hosts.length==0)) { host = att_host; }
	else if (hosts!=null && hosts.length>0){
		// find a Instance of the local one
		String ServerName = request.getServerName();
		logger.info("MCRQueryServlet: Try to map remote request to local one!");
		logger.info("MCRQueryServlet: Local Server Name="+ServerName);
		StringBuffer hostBf=new StringBuffer();
		for (int i=0;i<hosts.length;i++){
			if (!hosts[i].equals("local"))
				//the following replaces a remote request with "local" if needed
				hosts[i]= (isInstanceOfLocal(hosts[i],request)) ? "local" : hosts[i];
			//make a comma seperated list of all hosts
			hostBf.append(",").append(hosts[i]);
		}
		host=hostBf.deleteCharAt(0).toString();
		if (host.indexOf("local")!=host.lastIndexOf("local")){
			logger.info("MCRQueryServlet: multiple \"local\" will be removed by MCRQueryResult!");
		}
	}
    
    String view  = request.getParameter( "view");
    String ref   = request.getParameter( "ref");
    String offsetStr = request.getParameter( "offset" );
    String sizeStr = request.getParameter( "size" );
    String max_results = request.getParameter( "max_results" );
    SortKey = request.getParameter(SortParam);
	if (SortKey != null) {
		if (request.getParameter(InOrderParam)!=null &&
			request.getParameter(InOrderParam).toLowerCase().equals("false"))
			inOrder = false;
		else inOrder = true;
		customSort = true;
	}
	else customSort = false;
   	
    int status=0;
	int maxresults=0;
	if (max_results!=null) maxresults=Integer.parseInt(max_results);
	int offset=0;
	if (offsetStr!=null) offset=Integer.parseInt(offsetStr);
	int size=0;
	if (sizeStr!=null) size=Integer.parseInt(sizeStr);

    if( mode  == null ) { mode  = "ResultList"; }
    if( mode.equals("") ) { mode  = "ResultList"; }
    if( host  == null ) { host = "local"; }
    if( host.equals("") ) { host = "local"; }
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

    logger.info("MCRQueryServlet : mode = "+mode);
    logger.info("MCRQueryServlet : type = "+type);
    logger.info("MCRQueryServlet : hosts = "+ host);
    logger.info("MCRQueryServlet : lang = "+lang);
    logger.info("MCRQueryServlet : query = \""+query+"\"");

	// check for valid session
	if (mode.equals("CachedResultList"))
	{
	  String sId= (session != null)? session.getId(): "null";
	  if (!request.isRequestedSessionIdValid()){
		//page session timed out
		MCRException ex= new MCRException("Session invalid!");
		StringBuffer msg=new StringBuffer("Requested session is invalid, maybe it was timed out!\n");
		msg.append("requested session was: ").append(request.getRequestedSessionId()).append("!\n")
		   .append("actual session is: ").append(sId).append("!");
		generateErrorPage(request,response,HttpServletResponse.SC_REQUEST_TIMEOUT,msg.toString(),ex,false);
		return;
      	
	  }
	  cachedFlag = true;
	  mode = "ResultList";
	}
	// prepare the stylesheet name
	// TODO: Speed this up - it's tooo slow
	Properties parameters = MCRLayoutServlet.buildXSLParameters( request );
	String style = parameters.getProperty("Style",mode+"-"+type+"-"+lang);
	logger.info("Style = "+style);

	if (type.equals(sortType)){
		status = (request.getParameter( "status")!=null) ? Integer.parseInt(request.getParameter( "status")) : 0;
		String att_status  = (String) request.getAttribute( "status"  );
		if (att_status!=null) { status = Integer.parseInt(att_status); }
		boolean successor = ((status % 2) == 1) ? true : false;
		boolean predecessor   = (((status>>1) % 2) == 1) ? true : false;
		logger.info("MCRQueryServlet : status = "+status);
		logger.info("MCRQueryServlet : predecessor = "+predecessor);
		logger.info("MCRQueryServlet : successor = "+successor);
	}

    // query for classifications
    if (type.equals("class")) {
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
		generateErrorPage(request,response,
		HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		"Internal Server Error!",
		new MCRException( "No classification or category exists" ),false);
		return;
      }
      jdom = resarray.exportAllToDocument();
//System.out.println(new String(MCRUtils.getByteArray(jdom)));
      try {
          request.setAttribute( MCRLayoutServlet.JDOM_ATTR,  jdom  );
          request.setAttribute( "XSL.Style", style );
          RequestDispatcher rd = getServletContext()
            .getNamedDispatcher( "MCRLayoutServlet" );
          rd.forward( request, response );
        }
      catch( Exception ex ) {
		generateErrorPage(request,response,
		HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		"Error while forwarding XML document to LayoutServlet!",
		ex,false);
		return;
        }
      return;
      }

    if (cachedFlag)
    {
    	// retrieve result list from session cache
    	try {
    		//session at this point is valid, load objects
    		jdom = (org.jdom.Document) session.getAttribute( "CachedList" );
    		type = (String)            session.getAttribute( "CachedType" );
    		if (jdom == null || type == null)
    			throw new MCRException("Either jdom or type (or both) were null!");
      	}
		catch (Exception ex){
			generateErrorPage(request,response,
			                  HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		                      "Failed to get jdom and type out of session cache!",
		                      ex,false);
			return;
		}

		if (customSort && type.equals(sortType)){
			// when I'm in here a ResultList exists and I have to resort it.
			MCRXMLContainer resarray = new MCRXMLContainer();
			try {
				resarray.importElements(jdom);
			}
			catch (JDOMException e) {
				generateErrorPage(request,response,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error while RE-sorting JDOM",
				new MCRException("Import of elements failed due to some reason!"),false);
				return;
			}
			if (resarray.size()>0){
				//let's do resorting.
				jdom = sort(resarray, lang.toLowerCase()).exportAllToDocument();
				session.setAttribute( "CachedList", jdom );
				session.setAttribute( "CachedType", type );
			}
			else 
				logger.fatal("MCRQueryServlet: Error while RE-sorting JDOM:" +
				             "After import Containersize was ZERO!");
		}
		if ((view.equals("prev") || view.equals("next")) && (ref != null)){
			/* change generate new query */
			StringTokenizer refGet=null;
			try {
				refGet =
					new StringTokenizer(
						this.getBrowseElementID(jdom, ref, view.equals("next")),
						"@");
			} catch (Exception ex) {
				generateErrorPage(request,response,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Could not resolve browse origin!",
				ex,false);
				return;
			}
			if (refGet.countTokens() < 3){
				generateErrorPage(request,response,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Could not resolve browse origin!",
				new MCRException("MCRQueryServlet: Sorry \"refGet\" has not 3 Tokens: "+refGet),false);
				return;
			}
			String StrStatus=refGet.nextToken();
			query=new StringBuffer("/mycoreobject[@ID='")
					  .append(refGet.nextToken()).append("']").toString();
			host=refGet.nextToken();
			mode="ObjectMetadata";
			type=sortType;
			request.setAttribute("mode",mode);
			request.removeAttribute("status");
			request.setAttribute("status",StrStatus);
			request.setAttribute("type",type);
			request.setAttribute("hosts",host);
			request.setAttribute("lang",lang);
			request.setAttribute("query",query);
			request.setAttribute("view","done");
			logger.info("MCRQueryServlet: sending to myself:" +
			   "?mode="+mode+"&status="+StrStatus+"&type="+type+"&hosts="+host+
			   "&lang="+lang+"&query="+query );
			doGet(request,response);
			return;
		}
    }
    else {
    	//cachedFlag==false
    	MCRQueryResult result = new MCRQueryResult();
    	MCRXMLContainer resarray = result.setFromQuery(host, type, query );
    	if (type.equals(sortType)){
    		// Status setzen für Dokumente
    		if (resarray.size()==1)
    			resarray.setStatus(0,status);
    		else if (maxresults>0)
    			resarray.cutDownTo(maxresults);
    	}
    	// create a new session if not already alive and encache result list
    	if (mode.equals("ResultList")){
    		session = request.getSession(true);
    		if (type.equals(sortType))
    			jdom = sort(resarray, lang.toLowerCase()).exportAllToDocument();
    		else
    			jdom = resarray.exportAllToDocument(); // no Resultlist for documents: why sort?
    		session.setAttribute( "CachedList", jdom );
    		session.setAttribute( "CachedType", type );
    	}
    	else
    		jdom = resarray.exportAllToDocument(); // no result list --> no sort needed
    }
	try {
		if (mode.equals("ResultList") && !style.equals("xml"))
			request.setAttribute( MCRLayoutServlet.JDOM_ATTR, cutJDOM(jdom,offset,size));
		else
			request.setAttribute( MCRLayoutServlet.JDOM_ATTR,  jdom );
		request.setAttribute( "XSL.Style", style );
		RequestDispatcher rd = getServletContext()
		                       .getNamedDispatcher( "MCRLayoutServlet" );
		logger.info("MCRQueryServlet: forward to MCRLayoutServlet!");
		rd.forward( request, response );
	}
	catch( Exception ex ) {
		generateErrorPage(request,response,
		HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		"Error while forwarding XML document to LayoutServlet!",
		ex,false);
		return;
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
  	      throws MCRException, IOException{
  	org.jdom.Document tempDoc = (org.jdom.Document)jdom.clone();
    logger.info("MCRQueryServlet: getBrowseElementID() got: "+ref);
	StringTokenizer refGet = new StringTokenizer(ref,"@");
	if (refGet.countTokens() < 2)
		throw new MCRException("MCRQueryServlet: Sorry \"ref\" has not 2 Tokens: "+ref);
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
		throw new MCRException("MCRQueryServlet: Sorry doesn't found searched document");
	int status = ((search.getAttributeValue(MCRXMLContainer.ATTR_SUCC).equals("true"))?1:0)
	            +((search.getAttributeValue(MCRXMLContainer.ATTR_PRED).equals("true"))?2:0);
	id=search.getAttributeValue("id");
	host=search.getAttributeValue("host");
	String result=new StringBuffer().append(status).append('@').append(id)
	.append('@').append(host).toString();
	logger.info("MCRQueryServlet: getBrowseElementID() returns: "+result);
  	return result;
  }
  private Document getObjectMetaDataByID(HttpSession session,String ID, String host){
  	Document MetaData=null;
  	if(session!=null && ID!=null && host!=null){
		Document jdom = (org.jdom.Document) session.getAttribute( "CachedList" );
		if (jdom == null)
			return null;
		List elements = jdom.getRootElement()
							  .getChildren(MCRXMLContainer.TAG_RESULT);
		org.jdom.Element search=null;
		boolean found=false;
		while(!elements.isEmpty()){
			search=(Element)elements.get(0);
			if ((search).getAttributeValue("id").equals(ID) &&
				(search).getAttributeValue("host").equals(host)){
					elements.clear();
					found=true;
			}
			else {
				elements.remove(0);
			}
		}
	
		if (search==null || !found)
			return null;
		MetaData = new Document(new Element(MCRXMLContainer.TAG_RESULTS));
		MetaData.getRootElement().addContent((Element)search.clone());
		logger.info("MCRQueryServlet: found Element with ID "+ID+" in cache!");
  	}
  	return MetaData;
  }
  private MCRXMLContainer sort(MCRXMLContainer xmlcont, String lang){
  	MCRXMLSortInterface sorter=null;
  	try {
		sorter = (MCRXMLSortInterface)(Class.forName(
		               conf.getString("MCR.XMLSortInterfaceImpl", MCRStdSorter)))
		               .newInstance();
	} catch (InstantiationException e) {
		throw new MCRException(e.getMessage(),e);
	} catch (IllegalAccessException e) {
		throw new MCRException(e.getMessage(),e);
	} catch (ClassNotFoundException e) {
		throw new MCRException(e.getMessage(),e);
	}
	if (sorter.getServletContext()==null)
		sorter.setServletContext(getServletContext());
  	//MCRXMLSorter sorter=new MCRXMLSorter();
  	/*maybe here should be a propertie used
  	 * XPath Expression can be relative to mcr_result
  	 */
  	// sorter.addSortKey("./*/*/*/title[lang('"+lang+"')]");
  	if (customSort){
  		logger.info("MCRQueryServlet: CustomSort enalbed. Sorting inorder: " +inOrder);
  		sorter.addSortKey(replString(SortKey,MCRSorterConfDelim,lang), inOrder);
  	}
  	else {
  		int keynum=Integer.parseInt(conf.getString(MCRSorterConfPrefix+".keys.count","0"));
  		boolean inorder=true;
  		for (int key=1; key<=keynum; key++){
  			// get XPATH Expression and hope it's good, if not exist sort for title
  			inorder=conf.getBoolean(MCRSorterConfPrefix+".keys."+key+".inorder",true);
			sorter.addSortKey(replString(conf.getString(MCRSorterConfPrefix+".keys."+key,"./*/*/*/title[lang('"+lang+"')]"),MCRSorterConfDelim,lang),inorder);
  		}
	}
  	xmlcont.sort(sorter);
  	return xmlcont;
  }
  private static String replString(String parse, String from, String to){
  StringBuffer result= new StringBuffer(parse);
  if ((result.charAt(0)=='\"') && (result.charAt(result.length()-1)=='\"')){
  result.deleteCharAt(result.length()-1).deleteCharAt(0);
	  for (int i=result.toString().indexOf(from);i!=-1;i=result.toString().indexOf(from)){
		  result.replace(i,i+from.length(),to);
	  }
	  return result.toString();
  }
  else
	  return null;
  }
  
  private Document cutJDOM(Document jdom, int offset, int size){
     Document returns = (Document) jdom.clone();
     returns.getRootElement().removeChildren("mcr_result");
     List children = jdom.getRootElement().getChildren("mcr_result");
     if (size<=0){
     	offset=0;
     	size=children.size();
     }
     int amount=size;
     for (int i=offset;((amount>0) && (i<children.size()) && (i<(offset+size)));i++){
       	returns.getRootElement().addContent((Element)((Element)children.get(i)).clone());
       	amount--;
     }
     returns.getRootElement().setAttribute("count",""+children.size())
                             .setAttribute("offset",""+offset)
                             .setAttribute("size",""+size);
     return returns;
  }
  
  private boolean isInstanceOfLocal(String host, HttpServletRequest request){
  	String ServletHost=request.getServerName();
  	String RemoteHost= conf.getString("MCR.remoteaccess_"+host+"_host");
  	String ServletPath=request.getServletPath().substring(0,
  	                   request.getServletPath().lastIndexOf("/"))+
                       "/MCRQueryServlet";
	String RemotePath= conf.getString("MCR.remoteaccess_"+host+"_query_servlet");
  	int ServletPort=request.getServerPort();
  	int RemotePort=	 Integer.parseInt(conf.getString("MCR.remoteaccess_"+host+"_port"));
  	return ((RemoteHost.equals(ServletHost)) &&
  	        (RemotePath.equals(ServletPath)) && 
  	        (ServletPort==RemotePort)) ? true : false;
  }
  
  private String getProperty(HttpServletRequest request, String name){
	String value  = (String) request.getAttribute(name);
	//if Attribute not given try Parameter
  	if (value == null || value.length()==0)
		value = request.getParameter(name);
  	return value;
  }
    
  private void generateErrorPage(HttpServletRequest request,
                                 HttpServletResponse response,
                                 int error,
                                 String msg,
                                 Exception ex,
                                 boolean xmlstyle)
               throws IOException, ServletException{
	logger.error("MCRQueryServlet: Error "+
				 error+ " occured. The following message was given: "+
				 msg,ex);
    String rootname="mcr_error";
    String lang= (getProperty(request,"lang")!=null)?
                  getProperty(request,"lang"):defaultLang;
    String style=(xmlstyle)? "xml":("query-"+lang.toUpperCase());
	Element root=new Element(rootname);
	Element exception= new Element("exception");
	Document errorDoc=new Document(root,new DocType(rootname));
	root.setAttribute("HttpError",Integer.toString(error))
	    .setText(msg);
	if (ex != null){
		Element trace=new Element("trace");
		Element message=new Element("message");
		trace.setText(MCRException.getStackTraceAsString(ex));
		message.setText(ex.getMessage());
		exception.addContent(message)
		         .addContent(trace);
	}
	root.addContent(exception);
	request.setAttribute( MCRLayoutServlet.JDOM_ATTR,  errorDoc );
	request.setAttribute( "XSL.Style", style );
	RequestDispatcher rd = getServletContext()
	                       .getNamedDispatcher( "MCRLayoutServlet" );
	logger.info("MCRQueryServlet: forward to MCRLayoutServlet!");
	rd.forward( request, response );
  }
}
