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

package org.mycore.user;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.*;
import org.mycore.common.*;
import org.mycore.frontend.servlets.*;
import org.mycore.user.*;

/**
 * This servlet is used to login a user to the mycore system.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */

public class MCRLoginServlet extends MCRServlet
{
  // The configuration
  private static MCRConfiguration config;
  private static Logger logger=Logger.getLogger(MCRLoginServlet.class);

  // user ID and password of the guest user
  private static String guestID  = null;
  private static String guestPWD = null;

  /** Initialisation of the servlet */
  public void init()
  {
    MCRConfiguration.instance().reload(true);
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());

    guestID  = config.getString("MCR.users_guestuser_username","gast");
    guestPWD = config.getString("MCR.users_guestuser_userpasswd","gast");
  }

  /** This method overrides doGetPost of MCRServlet. */
  public void doGetPost(MCRServletJob job) throws Exception
  {
    boolean loginOk = false;

    // Get the MCRSession object for the current thread from the session manager.
    MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

    String uid = getStringParameter(job, "uid").trim();
    String pwd = getStringParameter(job, "pwd").trim();
    String url = getStringParameter(job, "url").trim();

    if (uid.length() == 0) uid = null;
    if (pwd.length() == 0) pwd = null;
    if( url.length() == 0 ) url = null;

    // Do not change login, just redirect to given url:
    if (mcrSession.getCurrentUserID().equals(uid) && (pwd == null) && (url != null)) {
      job.getResponse().sendRedirect(url);
      return;
    }

    if (url == null) url = MCRServlet.getBaseURL();

    org.jdom.Element root = new org.jdom.Element("mcr_user");
    org.jdom.Document jdomDoc = new org.jdom.Document(root);

    root.addContent(new org.jdom.Element("guest_id").addContent(guestID));
    root.addContent(new org.jdom.Element("guest_pwd").addContent(guestPWD));

    try {
      loginOk = ((uid != null) && (pwd != null) && MCRUserMgr.instance().login(uid, pwd));

      // If the login attempt was successfull, change the user ID and redirect to target URL
      if (loginOk) {
        mcrSession.setCurrentUserID(uid);
        job.getRequest().getSession().setAttribute( "XSL.CurrentUser", uid );
        job.getResponse().sendRedirect(url);
        return;
      }
      else {
        if (uid != null)
          root.setAttribute("invalid_password", "true");
      }
    }
    catch (MCRException e) {
      if (e.getMessage().equals("Unknown user.")) {
        root.setAttribute("unknown_user", "true");
        logger.info("MCRLoginServlet: unknown user:" + uid);
      }
      else if (e.getMessage().equals("Login denied. User is disabled.")) {
        root.setAttribute("user_disabled", "true");
        logger.info("MCRLoginServlet: disabled user " + uid + " tried to login.");
      }
      else throw e;
    }

    root.addContent(new org.jdom.Element("url").addContent(url));
    doLayout(job, "login", jdomDoc); // use the stylesheet mcr_user-login-*.xsl
  }

  /**
   * Gather information about the XML document to be shown and the corresponding XSLT
   * stylesheet and redirect the request to the LayoutServlet
   *
   * @param job
   * @param styleBase
   * @param jdomDoc
   * @throws ServletException
   * @throws IOException
   */
  protected void doLayout(MCRServletJob job, String styleBase, Document jdomDoc)
                          throws ServletException, IOException
  {
    String language = MCRSessionMgr.getCurrentSession().getCurrentLanguage();

    job.getRequest().getSession().setAttribute("mycore.language", language);
    job.getRequest().getSession().setAttribute("XSL.CurrentUser", MCRSessionMgr.getCurrentSession().getCurrentUserID());

    String styleSheet = styleBase + "-" + language;
    job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdomDoc);
    job.getRequest().setAttribute("XSL.Style", styleSheet);

    RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
    rd.forward(job.getRequest(), job.getResponse());
  }
}
