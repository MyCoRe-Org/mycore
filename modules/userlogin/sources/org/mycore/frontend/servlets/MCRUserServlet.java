/*
 * 
 * $Revision: 1.1 $ $Date: 2008/04/11 09:09:38 $
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

package org.mycore.frontend.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

/**
 * This servlet provides a web interface for the user management of the mycore
 * system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision: 1.1 $ $Date: 2008/04/11 09:09:38 $
 */
public class MCRUserServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    // The configuration
    private static Logger LOGGER = Logger.getLogger(MCRUserServlet.class);

    // user ID and password of the guest user
    private static String GUEST_ID;

    private static String GUEST_PWD;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {
        super.init();
        GUEST_ID = CONFIG.getString("MCR.Users.Guestuser.UserName", "gast");
        GUEST_PWD = CONFIG.getString("MCR.Users.Guestuser.UserPasswd", "gast");
    }

    /**
     * This method overrides doGetPost of MCRServlet and handles HTTP requests.
     * Depending on the request parameter "mode" this method delegates the
     * request to different methods of this servlet.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    public void doGetPost(MCRServletJob job) throws IOException {
        String mode = getProperty(job.getRequest(), "mode");

        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        if (mode.length() == 0) {
            mode = "Select";
        }
        LOGGER.debug("SessionID: "+mcrSession.getID());
        LOGGER.debug("CurrentID: "+mcrSession.getCurrentUserID());
        LOGGER.debug("Mode     : "+mode);

        if (mode.equals("ChangePwd")) {
            if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse(), getBaseURL()))
                return;
            changePwd(job);
        } else if (mode.equals("CreatePwdDialog")) {
            if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse(), getBaseURL()))
                return;
            createPwdDialog(job);
        } else if (mode.equals("Select")) {
            selectTask(job);
        } else if (mode.equals("ShowUser")) {
            showUser(job);
        } else { // no valid mode, redirect to original URL

            String backto_url = getProperty(job.getRequest(), "url");

            if (backto_url.length() == 0) {
                return;
            }
            LOGGER.debug("URL :      "+backto_url);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(backto_url));
            return;
        }
    }

    /**
     * This method handles the "ChangePwd" (change password) mode. The change
     * password dialog of the presentation layer must provide three passwords in
     * the http request: The new password, an repetition of the new password and
     * (for security reasons) the old password again. This method checks if the
     * old password is correct and if both new passwords are equal. If so, the
     * password is changed and the control flow is routed to the presentation of
     * possible task for the current user. If not, error messages are displayed.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    protected void changePwd(MCRServletJob job) throws IOException {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUser = mcrSession.getCurrentUserID();

        String pwd_1 = getProperty(job.getRequest(), "pwd_1").trim();
        String pwd_2 = getProperty(job.getRequest(), "pwd_2").trim();
        String oldpwd = getProperty(job.getRequest(), "oldpwd").trim();

        org.jdom.Document jdomDoc = createJdomDocBase(job);
        org.jdom.Element root = jdomDoc.getRootElement();

        if (!pwd_1.equals(pwd_2)) {
            root.setAttribute("new_pwd_mismatch", "true");
        } else if (!MCRUserMgr.instance().login(currentUser, oldpwd)) {
            root.setAttribute("old_pwd_mismatch", "true");
        } else {
            try {
                MCRUserMgr.instance().setPassword(currentUser, pwd_1);
                root.setAttribute("pwd_change_ok", "true");
                doLayout(job, "SelectTask", jdomDoc); // use the stylesheet

                // mcr_user-SelectTask.xsl
                return;
            } catch (MCRException e) {
                root.addContent(new org.jdom.Element("error").addContent(e.getMessage()));
            }
        }

        doLayout(job, "ChangePwd", jdomDoc); // use the stylesheet

        // mcr_user-ChangePwd.xsl
    }

    /**
     * This method handles the "CreatePwdDialog" mode. It is nothing more than
     * choosing the right stylesheet.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    protected void createPwdDialog(MCRServletJob job) throws IOException {
        org.jdom.Document jdomDoc = createJdomDocBase(job);
        doLayout(job, "ChangePwd", jdomDoc); // use the stylesheet

        // mcr_user-ChangePwd.xsl
    }

    /**
     * This method handles the "Select" mode. Depending on the privileges of the
     * current user a list of possible tasks is collected as an XML
     * representation and forwarded to the LayoutServlet.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    protected void selectTask(MCRServletJob job) throws IOException {
        // For the moment only tasks possible for all users are presented. But
        // this is work
        // in progress. In the future the list of privileges for the current
        // user will be
        // checked here and in case he or she has additional privileges this
        // will be
        // forwarded to the presentation layer (i.e. XSL stylesheets).
        org.jdom.Document jdomDoc = createJdomDocBase(job);
        doLayout(job, "SelectTask", jdomDoc); // use the stylesheet

        // mcr_user-SelectTask.xsl
    }

    /**
     * This method handles the "ShowUser" mode. The current user is retrieved
     * from the user manager and its XML representation is forwarded to the
     * LayoutServlet.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    protected void showUser(MCRServletJob job) throws IOException {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUser = mcrSession.getCurrentUserID();

        org.jdom.Document jdomDoc = createJdomDocBase(job);
        org.jdom.Element root = jdomDoc.getRootElement();

        MCRUser user = MCRUserMgr.instance().retrieveUser(currentUser);
        root.addContent(user.toJDOMElement());

        doLayout(job, "Metadata", jdomDoc); // use the stylesheet

        // mcr_user-Metadata.xsl
    }

    /**
     * creates a jdom document with elements needed by all modes this servlet
     * can run.
     * 
     * @param job
     *            The MCRServletJob instance
     * @return jdom document
     */
    protected org.jdom.Document createJdomDocBase(MCRServletJob job) {
        // Get the MCRSession object for the current thread from the session
        // manager.
        String backto_url = null;
        String url = job.getRequest().getParameter("url");
        if (url != null && url.trim().length() > 0) {
            backto_url = url.trim();
        }

        org.jdom.Element root = new org.jdom.Element("mcr_user");
        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        root.addContent(new org.jdom.Element("guest_id").addContent(GUEST_ID));
        root.addContent(new org.jdom.Element("guest_pwd").addContent(GUEST_PWD));
        root.addContent(new org.jdom.Element("backto_url").addContent(backto_url));

        return jdomDoc;
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
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), jdomDoc);
    }
}
