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

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user.MCRUserMgr;

/**
 * This servlet controls the web interface for the editors of the user
 * management of the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserAdminServlet extends MCRUserAdminGUICommons {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    /** Initialisation of the servlet */
    public void init() throws ServletException {
        super.init();
    }

    /**
     * This method overrides doGetPost of MCRServlet and handles HTTP requests.
     * Depending on the mode parameter in the request, the method dispatches
     * actions to be done by subsequent private methods. Examples for typical
     * actions are creating an new user account (mode=newuser), modifying an
     * existing user account (mode=modifyuser) etc.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     */
    public void doGetPost(MCRServletJob job) throws IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();
        // Read the mode from the HTTP request, dispatch to subsequent methods
        String mode = getProperty(job.getRequest(), "mode");

        if (mode == null) {
            mode = "";
        }

        if (mode.equals("newuser")) {
            createUser(job);
        } else if (mode.equals("modifyuser")) {
            modifyUser(job);
        } else if (mode.equals("listalluser")) {
            listallUser(job);
        } else { // no valid mode

            String msg = "The request did not contain a valid mode for this servlet!";
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    /**
     * This method handles the create user use case. The MyCoRe editor framework
     * is used to obtain an XML representation of a new user account.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     */
    private void createUser(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!AI.checkPermission("create-user")) {
            showNoPrivsPage(job);

            return;
        }

        // Now we redirect the browser to the create-user formular
        String editorFormular = pageDir + "editor_form_create-user.xml";
        String base = getBaseURL() + editorFormular;
        Properties params = new Properties();
        params.put("XSL.editor.source.new", "true");
        params.put("XSL.editor.cancel.url", getBaseURL() + cancelPage);
        params.put("usecase", "create-user");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

        return;
    }

    /**
     * This method handles the list all users use case. The result is the XML of
     * userlist
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     */
    private void listallUser(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!AI.checkPermission("administrate-user")) {
            showNoPrivsPage(job);

            return;
        }

        // Now we redirect the browser to the userlist formular
        String UserServlet = getBaseURL() + "servlets/MCRUserEditorServlet?mode=retrievealluserxml";
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(UserServlet));

        return;
    }

    /**
     * This method is still experimental !
     */
    private void modifyUser(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!AI.checkPermission("modify-user")) {
            showNoPrivsPage(job);
            return;
        }

        // Now we redirect the browser to the modify-user formular
        String editorFormular = pageDir + "editor_form_modify-user.xml";
        String uid = getProperty(job.getRequest(), "uid");
        String base = getBaseURL() + editorFormular;
        Properties params = new Properties();
        params.put("XSL.editor.source.url", "servlets/MCRUserEditorServlet?mode=retrieveuserxml&uid=" + uid);
        params.put("XSL.editor.cancel.url", getBaseURL() + cancelPage);
        params.put("usecase", "modify-user");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

        return;
    }
}
