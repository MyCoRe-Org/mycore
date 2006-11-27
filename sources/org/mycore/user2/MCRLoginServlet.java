/*
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet is used to login a user to the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRLoginServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    // The configuration
    private static Logger LOGGER = Logger.getLogger(MCRLoginServlet.class);

    // user ID and password of the guest user
    private static String GUEST_ID;

    private static String GUEST_PWD;

    public void init() throws ServletException {
        super.init();

        if ((GUEST_ID == null) || (GUEST_PWD == null)) {
            GUEST_ID = CONFIG.getString("MCR.users_guestuser_username");
            GUEST_PWD = CONFIG.getString("MCR.users_guestuser_userpasswd");
        }
    }

    /** This method overrides doGetPost of MCRServlet. */
    public void doGetPost(MCRServletJob job) throws Exception {
        boolean loginOk = false;

        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        String uid = getProperty(job.getRequest(), "uid");
        String pwd = getProperty(job.getRequest(), "pwd");
        String backto_url = getProperty(job.getRequest(), "url");

        if (uid != null) {
            uid = (uid.trim().length() == 0) ? null : uid.trim();
        }

        if (pwd != null) {
            pwd = (pwd.trim().length() == 0) ? null : pwd.trim();
        }

        if (backto_url != null) {
            backto_url = (backto_url.trim().length() == 0) ? null : backto_url.trim();
        }

        // Do not change login, just redirect to given url:
        if (mcrSession.getCurrentUserID().equals(uid) && (pwd == null) && (backto_url != null)) {
            job.getResponse().setHeader("Cache-Control", "no-cache");
            job.getResponse().setHeader("Pragma", "no-cache");
            job.getResponse().setHeader("Expires","0");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(addParameter(backto_url,"reload","true")));

            return;
        }

        if (backto_url == null) {
            backto_url = MCRServlet.getBaseURL();
        }

        org.jdom.Element root = new org.jdom.Element("mcr_user");
        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        root.addContent(new org.jdom.Element("guest_id").addContent(GUEST_ID));
        root.addContent(new org.jdom.Element("guest_pwd").addContent(GUEST_PWD));

        try {
            loginOk = ((uid != null) && (pwd != null) && MCRUserMgr.instance().login(uid, pwd));

            // If the login attempt was successfull, change the user ID and
            // forward to the
            // UserServlet with mode=Select. However, if the user to login is
            // the guest user,
            // then the request will just be redirected to the originating URL,
            // i.e. not
            // forwarded to the UserServlet.
            if (loginOk) {
                mcrSession.setCurrentUserID(uid);
                mcrSession.setLoginTime();
                LOGGER.info("MCRLoginServlet: user " + uid + " logged in successfully.");

                // We here put the list of groups separated by blanks as a
                // string into the HTTP
                // session. The LayoutServlet then forwards them to the XSL
                // Stylesheets.
                StringBuffer groups = new StringBuffer();
                ArrayList groupList = MCRUserMgr.instance().retrieveUser(uid).getGroupIDs();

                for (int i = 0; i < groupList.size(); i++) {
                    groups.append((String) groupList.get(i)).append(" ");
                }

                job.getRequest().getSession().setAttribute("XSL.CurrentGroups", groups.toString());

                if (uid.equals(GUEST_ID)) {
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(backto_url));

                    return;
                }

                RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRUserServlet");
                job.getRequest().removeAttribute("lang");
                job.getRequest().setAttribute("lang", mcrSession.getCurrentLanguage());
                job.getRequest().removeAttribute("mode");
                job.getRequest().setAttribute("mode", "Select");
                rd.forward(job.getRequest(), job.getResponse());

                return;
            }

            if (uid != null) {
                root.setAttribute("invalid_password", "true");
            }
        } catch (MCRException e) {
            if (e.getMessage().equals("user can't be found in the database")) {
                root.setAttribute("unknown_user", "true");
                LOGGER.info("MCRLoginServlet: unknown user: " + uid);
            } else if (e.getMessage().equals("Login denied. User is disabled.")) {
                root.setAttribute("user_disabled", "true");
                LOGGER.info("MCRLoginServlet: disabled user " + uid + " tried to login.");
            } else {
                LOGGER.debug("MCRLoginServlet: unknown error: " + e.getMessage());
                throw e;
            }
        }

        root.addContent(new org.jdom.Element("backto_url").addContent(backto_url));
        doLayout(job, "login", jdomDoc); // use the stylesheet

        // mcr_user-login-*.xsl
    }

    /**
     * Gather information about the XML document to be shown and the
     * corresponding XSLT stylesheet and redirect the request to the
     * LayoutServlet
     * 
     * @param job
     *            The MCRServletJob instance
     * @param style
     *            String value to select the correct XSL stylesheet
     * @param jdomDoc
     *            The XML representation to be presented by the LayoutServlet
     * @throws ServletException
     *             for errors from the servlet engine.
     * @throws IOException
     *             for java I/O errors.
     */
    protected void doLayout(MCRServletJob job, String style, Document jdomDoc) throws IOException {
        job.getRequest().setAttribute("XSL.Style", style);
        getLayoutService().doLayout(job.getRequest(),job.getResponse(),jdomDoc);
    }
    
    private static final String addParameter(String url, String name, String value){
        if (url.indexOf("?")==-1){
            return url+"?"+name+"="+value;
        }
        return url+"&"+name+"="+value;
    }
}
