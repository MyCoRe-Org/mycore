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
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mycore.common.*;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for
 * logging and managing the current session data. Part of the code has been taken
 * from MilessServlet.java by Frank Lützenkirchen.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/

public class MCRServlet extends HttpServlet
{
  // Some configuration details
  private static MCRConfiguration config = null;
  private static Logger logger=Logger.getLogger(MCRServlet.class);

  // These values serve to remember if we have a GET or POST request
  private final static boolean GET  = true;
  private final static boolean POST = false;

  /** Initialisation of the servlet */
  public void init()
  {
    MCRConfiguration.instance().reload(true);
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
  }

  private static String baseURL, servletURL;

  public static String getBaseURL()
  { return baseURL; }

  public static String getServletBaseURL()
  { return servletURL; }

  private static synchronized void prepareURLs( HttpServletRequest req )
    throws ServletException, IOException
  {
    String contextPath = req.getContextPath();
    if( contextPath == null ) contextPath = "";
    contextPath += "/";

    String requestURL = HttpUtils.getRequestURL( req ).toString();
    int pos = requestURL.indexOf( contextPath, 9 );
    baseURL = requestURL.substring( 0, pos ) + contextPath;

    servletURL = baseURL + "servlets/";
  }

  //
  // The methods doGet() and doPost() simply call the private method doGetPost(),
  // i.e. GET- and POST requests are handled by one method only.
  //

  public void doGet(HttpServletRequest  req,
                    HttpServletResponse res )
                    throws ServletException, IOException
  { doGetPost(req, res, GET); }

  public void doPost(HttpServletRequest  req,
                     HttpServletResponse res )
                     throws ServletException, IOException
  { doGetPost(req, res, POST); }

  /**
   *
   * @param req the HTTP request instance
   * @param res the HTTP response instance
   * @param GETorPOST boolean value to remember if we have a GET or POST request
   * @exception IOException for java I/O errors.
   * @exception ServletException for errors from the servlet engine.
   */
  private void doGetPost(HttpServletRequest  req,
                         HttpServletResponse res, boolean GETorPOST)
                         throws ServletException, IOException
  {
    String c = getClass().getName();
           c = c.substring(c.lastIndexOf( "." ) + 1);

    if (baseURL == null) prepareURLs(req);

    try
    {
      HttpSession theSession = req.getSession();

      // Get MCRSession object from the session or create it
      MCRSession mcrSession = (MCRSession)(theSession.getAttribute("MCRSession"));
      if (mcrSession == null)
      {
        mcrSession = new MCRSession();
        theSession.setAttribute("MCRSession", mcrSession);
      }

      String s = (theSession.isNew() ? "new" : "old" ) + " session " + theSession.getId();
      String u = mcrSession.getCurrentUserID();
      String h = req.getRemoteHost();

      if((h == null) || (h.trim().length() == 0))
      {
        h = req.getRemoteAddr();
        h = InetAddress.getByName(h).getHostName();
      }
      h = h.toLowerCase();

      logger.info(c + " request from " + h + " : " + s + " user " + u);

      // enthält req, res, MCRSession, am besten auch Servlet Context usw.
      MCRServletJob job = new MCRServletJob(mcrSession, req, res);

      if(GETorPOST == GET)
        doGet(job);
      else doPost(job);
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
  }

  protected void doGetPost(MCRServletJob job) throws Exception
  {
    // default: send HTTP response code that indicates unsupported service
  }

  protected void doGet(MCRServletJob job) throws Exception
  { doGetPost(job); }

  protected void doPost(MCRServletJob job) throws Exception
  { doGetPost(job); }

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

  /**
   * Handles an exception by reporting it and its embedded exception
   */
  protected void handleException(Exception ex)
  {
    try
    {
      reportException(ex);
      if(ex instanceof MCRException)
      {
        ex = ((MCRException)ex).getException();
        if(ex != null) reportException(ex);
      }
    }

    catch(Exception ex2)
    {
      try{ reportException(ex2); }
      catch(Exception ignored){}
    }
  }

  /**
   * Reports an exception to the log (stdout) and to the browser
   */
  protected void reportException(Exception ex) throws Exception
  {
    String msg     = (ex.getMessage() == null ? "" : ex.getMessage());
    String type    = ex.getClass().getName();
    String cname   = this.getClass().getName();
    String servlet = cname.substring(cname.lastIndexOf( "." ) + 1);

    logger.info("Exception caught in " + servlet);
    logger.info("Exception type:     " + type   );
    logger.info("Exception message:  " + msg    );
    ex.printStackTrace();
  }
}
