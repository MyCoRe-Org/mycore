/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.frontend.servlets;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;
import java.net.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.*;
import org.mycore.common.xml.MCRLayoutServlet;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for
 * logging and managing the current session data. Part of the code has been taken
 * from MilessServlet.java written by Frank L�tzenkirchen.
 *
 * @author Detlev Degenhardt
 * @author Frank L�tzenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 **/
public class MCRServlet extends HttpServlet
{
  // Some configuration details
  protected static MCRConfiguration config = null;

  private static Logger logger=Logger.getLogger(MCRServlet.class);
  private static String baseURL, servletURL;

  // These values serve to remember if we have a GET or POST request
  private final static boolean GET  = true;
  private final static boolean POST = false;

  protected String ReqCharEncoding;

	/** Initialisation of the servlet */
  public void init()
  {
    MCRConfiguration.instance().reload(true);
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
  }

  /** returns the base URL of the mycore system */
  public static String getBaseURL()
  { return baseURL; }

  /** returns the servlet base URL of the mycore system */
  public static String getServletBaseURL()
  { return servletURL; }

  /**
   * Initialisation of the static values for the base URL and servlet URL of
   * the mycore system.
   */
  private static synchronized void prepareURLs(HttpServletRequest req)
    throws ServletException, IOException
  {
    String contextPath = req.getContextPath();
    if( contextPath == null ) contextPath = "";
    contextPath += "/";

    String requestURL = req.getRequestURL().toString();
    int pos = requestURL.indexOf( contextPath, 9 );
    baseURL = requestURL.substring( 0, pos ) + contextPath;

    servletURL = baseURL + "servlets/";
  }

  // The methods doGet() and doPost() simply call the private method doGetPost(),
  // i.e. GET- and POST requests are handled by one method only.

  public void doGet(HttpServletRequest  req,
                    HttpServletResponse res )
                    throws ServletException, IOException
  { doGetPost(req, res, GET); }

  protected void doGet(MCRServletJob job) throws Exception
  { doGetPost(job); }

  public void doPost(HttpServletRequest  req,
                     HttpServletResponse res )
                     throws ServletException, IOException
  { doGetPost(req, res, POST); }

  protected void doPost(MCRServletJob job) throws Exception
  { doGetPost(job); }

  /**
   * This private method handles both GET and POST requests and is invoked by doGet()
   * and doPost().
   *
   * @param req the HTTP request instance
   * @param res the HTTP response instance
   * @param GETorPOST boolean value to remember if we have a GET or POST request
   *
   * @exception IOException for java I/O errors.
   * @exception ServletException for errors from the servlet engine.
   */
  private void doGetPost(HttpServletRequest  req,
                         HttpServletResponse res, boolean GETorPOST)
                         throws ServletException, IOException
  {
		// Try to set encoding of form values
		ReqCharEncoding = req.getCharacterEncoding();
		if (ReqCharEncoding == null) {
			// Set default to UTF-8
			ReqCharEncoding = config.getString("MCR.request_charencoding","UTF-8");
			req.setCharacterEncoding(ReqCharEncoding);
			logger.debug("Setting ReqCharEncoding to: "+ReqCharEncoding);
		}
    String c = getClass().getName();
           c = c.substring(c.lastIndexOf( "." ) + 1);

    if (baseURL == null) prepareURLs(req);

    try
    {
      HttpSession theSession = req.getSession(true);
      MCRSession  session    = null;    

      String sessionID = req.getParameter( "MCRSessionID" );
      MCRSession fromRequest = null;
      if( sessionID != null ) fromRequest = MCRSession.getSession( sessionID );
    
      MCRSession fromHttpSession = (MCRSession)theSession.getAttribute( "mycore.session" );

      // Choose: 
      if( fromRequest != null )
        session = fromRequest; // Take session from http request parameter MCRSessionID
      else if ( fromHttpSession != null )
        session = fromHttpSession; // Take session from HttpSession with servlets
      else
        session =  MCRSessionMgr.getCurrentSession(); // Create a new session

      // Store current session in HttpSession
      theSession.setAttribute( "mycore.session", session );
      
      // Bind current session to this thread:
      MCRSessionMgr.setCurrentSession(session);

      // Forward MCRSessionID to XSL Stylesheets
      req.setAttribute( "XSL.MCRSessionID", session.getID() );

      String s = ( theSession.isNew() ? "new" : "old" ) + " HttpSession=" + theSession.getId() + " MCRSession=" + session.getID() ;
      String u = session.getCurrentUserID();
      String h = req.getRemoteHost();

      if ((h == null) || (h.trim().length() == 0))
      {
        h = req.getRemoteAddr();
        h = InetAddress.getByName(h).getHostName();
      }
      h = h.toLowerCase();
      logger.info(c + " request from " + h + " : " + s + " user " + u);

      MCRServletJob job = new MCRServletJob(req, res);

      // Uebernahme der gewuenschten Sprache aus dem Request zunaechst mal nur als Test!!!
      String lang = getStringParameter(job, "lang");
      if (lang.trim().length() != 0)
        session.setCurrentLanguage(lang.trim().toUpperCase());
      if( GETorPOST == GET ) doGet( job ); else doPost( job );
    }

    catch(Exception ex)
    {
      if(ex instanceof ServletException)
        throw (ServletException)ex;
      else if(ex instanceof IOException)
        throw (IOException)ex;
      else
        handleException(ex);
    }

    finally {
      // Release current MCRSession from current Thread,
      // in case that Thread pooling will be used by servlet engine
      MCRSessionMgr.releaseCurrentSession();
    }
  }

  /**
   * This method should be overwritten by other servlets. As a default response we
   * indicate the HTTP 1.1 status code 501 (Not Implemented).
   */
  protected void doGetPost(MCRServletJob job) throws Exception
  { job.getResponse().sendError(HttpServletResponse.SC_NOT_IMPLEMENTED); }

  /**
   * This method gets a string parameter defined by parameterName out of the request.
   *
   * @param job the ServletJob instance
   * @param parameterName the name of the parameter to be extracted
   * @return the parameter
   */
  protected String getStringParameter(MCRServletJob job, String parameterName )
  {
    String   param = "";
    String[] array = job.getRequest().getParameterValues(parameterName);

    if (array != null) param = array[0];
    return param;
  }

  /** Handles an exception by reporting it and its embedded exception */
  protected void handleException( Exception ex )
  {
    try 
    {
      reportException( ex );
      if( ex instanceof MCRException )
      {
        ex = ( (MCRException)ex ).getException();
        if( ex != null ) handleException( ex );
      }
    }
    catch( Exception ignored ){}
  }

  /** Reports an exception to the log */
  protected void reportException( Exception ex ) 
    throws Exception
  {
    String msg     = ( ex.getMessage() == null ? "" : ex.getMessage() );
    String type    = ex.getClass().getName();
    String cname   = this.getClass().getName();
    String servlet = cname.substring( cname.lastIndexOf( "." ) + 1 );
    String trace   = MCRException.getStackTraceAsString( ex );

    logger.info( "Exception caught in : " + servlet );
    logger.info( "Exception type      : " + type    );
    logger.info( "Exception message   : " + msg     );
    logger.debug( trace ); 
  }

protected void generateErrorPage(HttpServletRequest request, HttpServletResponse response, int error, String msg, Exception ex, boolean xmlstyle) throws IOException, ServletException {
	logger.error(getClass().getName()+": Error "+
				 error+ " occured. The following message was given: "+
				 msg,ex);
    String rootname="mcr_error";
	String defaultLang = config
	  .getString( "MCR.metadata_default_lang", "en" ).toUpperCase();
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

protected String getProperty(HttpServletRequest request, String name) {
	String value = (String) request.getAttribute(name);
	//if Attribute not given try Parameter
	if (value == null || value.length() == 0)
		value = request.getParameter(name);
	//this fixes NullPointerException with value.getBytes() below
	if (value == null || value.length() == 0)
		return value;
	return value;
}
  
}
