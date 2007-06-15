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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;

/**
 * This servlet provides a web interface for the editors of the user management
 * of the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserEditorServlet extends MCRUserAdminGUICommons {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRUserEditorServlet.class);

    /** Initialisation of the servlet */
    public void init() throws ServletException {
        super.init();
    }

    /**
     * This method overrides doGetPost of MCRServlet and handles HTTP requests.
     * Depending on the mode parameter in the request, the method dispatches
     * actions to be done by subsequent private methods. In contrast to the
     * MCRUserAdminServlet - which serves as an entry point and dispatcher for
     * all use cases of the user management GUI - this servlet is called by the
     * MyCoRe editor framework, triggered by the editor definition files of the
     * user management GUI. In addition, this servlet is the target of the
     * MyCoRe editor framework for the user management GUI, i.e. it reads the
     * editor submission and acts accordingly.
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

        if (mode == null) {
            mode = "xml";
        }

        if (mode.equals("getAssignableGroupsForUser")) {
            getAssignableGroupsForUser(job);
        } else if (mode.equals("getAllUsers")) {
            getAllUsers(job);
        } else if (mode.equals("getAllGroups")) {
            getAllGroups(job);
        } else if (mode.equals("retrieveuserxml")) {
            retrieveUserXML(job);
        } else if (mode.equals("retrievegroupxml")) {
            retrieveGroupXML(job);
        } else if (mode.equals("retrievealluserxml")) {
            retrieveAllUserXML(job);
        } else if (mode.equals("xml")) {
            getEditorSubmission(job);
        } else { // no valid mode

            String msg = "The request did not contain a valid mode for this servlet!";
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    /**
     * This method retrieves a list of groups the current user may assign to a
     * new or existing user account. Typically this servlet mode is implicitly
     * called by an MyCoRe editor definition file, e.g. to fill drop down boxes
     * or lists in the user administration GUI.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getAssignableGroupsForUser(MCRServletJob job) throws IOException {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();
        List<String> groupIDs = null;

        try {
            // The list of assignable groups depends on the privileges of the
            // current user.
            MCRUser currentUser = MCRUserMgr.instance().retrieveUser(currentUserID);

            if (AI.checkPermission("administrate-user")) {
                groupIDs = MCRUserMgr.instance().getAllGroupIDs();
            } else if (AI.checkPermission("create-user")) {
                groupIDs = currentUser.getAllGroupIDs();
            } else {
                // There are no permissions to assign any groups to a new user.
                // Possibly someone tried to call the new user form directly
                // without
                // checking the privileges using the MCRUserAdminServlet first.
                LOGGER.warn("MCRUserEditorServlet: not enough permissions! " + "Someone might have tried to call the new user form directly.");

                // TODO: Hier muss irgendwie eine vern�nftige Fehlermeldung
                // her!
                // Aktuell spielt das MCREditorServlet noch nicht mit. Die
                // folgenden
                // Zeilen z.B. f�hren zu einem "impossible to open input
                // stream"
                // Fehler im MCREditorServlet (weil halt error 403 gesendet
                // wird)
                String msg = "You do not have enough permissions for this use case!";
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, msg);

                return;
            }
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);

            return;
        }

        // Loop over all assignable group IDs
        org.jdom.Element root = new org.jdom.Element("items");

        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element item = new org.jdom.Element("item").setAttribute("value", (String) groupIDs.get(i))
                    .setAttribute("label", (String) groupIDs.get(i));
            root.addContent(item);
        }

        org.jdom.Document jdomDoc = new org.jdom.Document(root);
        getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdomDoc);
    }

    /**
     * This method retrieves a list of all groups. Typically this servlet mode
     * is implicitly called by an MyCoRe editor definition file, e.g. to fill
     * drop down boxes or lists in the user administration GUI.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getAllGroups(MCRServletJob job) throws IOException {
        List<String> groupIDs;
        try {
            if (AI.checkPermission("administrate-user")) {
                groupIDs = MCRUserMgr.instance().getAllGroupIDs();
            } else {
                // There are no permissions to assign any groups to a new user.
                // Possibly someone tried to call the new user form directly
                // without
                // checking the privileges using the MCRUserAdminServlet first.
                LOGGER.warn("MCRUserEditorServlet: not enough permissions! " + "Someone might have tried to call the new user form directly.");

                // TODO: Hier muss irgendwie eine vern�nftige Fehlermeldung
                // her!
                // Aktuell spielt das MCREditorServlet noch nicht mit. Die
                // folgenden
                // Zeilen z.B. f�hren zu einem "impossible to open input
                // stream"
                // Fehler im MCREditorServlet (weil halt error 403 gesendet
                // wird)
                String msg = "You do not have enough permissions for this use case!";
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, msg);

                return;
            }
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);

            return;
        }

        // Loop over all assignable group IDs
        org.jdom.Element root = new org.jdom.Element("items");

        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element item = new org.jdom.Element("item").setAttribute("value", (String) groupIDs.get(i))
                    .setAttribute("label", (String) groupIDs.get(i));
            root.addContent(item);
        }

        org.jdom.Document jdomDoc = new org.jdom.Document(root);
        getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdomDoc);
    }

    /**
     * This method retrieves a list of all users. Typically this servlet mode is
     * implicitly called by an MyCoRe editor definition file, e.g. to fill drop
     * down boxes or lists in the user administration GUI.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getAllUsers(MCRServletJob job) throws IOException {
        List<String> userIDs;
        try {
            if (AI.checkPermission("administrate-user")) {
                userIDs = MCRUserMgr.instance().getAllUserIDs();
            } else {
                // There are no permissions to assign any groups to a new user.
                // Possibly someone tried to call the new user form directly
                // without
                // checking the privileges using the MCRUserAdminServlet first.
                LOGGER.warn("MCRUserEditorServlet: not enough permissions! " + "Someone might have tried to call the new user form directly.");

                // TODO: Hier muss irgendwie eine vern�nftige Fehlermeldung
                // her!
                // Aktuell spielt das MCREditorServlet noch nicht mit. Die
                // folgenden
                // Zeilen z.B. f�hren zu einem "impossible to open input
                // stream"
                // Fehler im MCREditorServlet (weil halt error 403 gesendet
                // wird)
                String msg = "You do not have enough permissions for this use case!";
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, msg);

                return;
            }
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);

            return;
        }

        // Loop over all assignable group IDs
        org.jdom.Element root = new org.jdom.Element("items");

        for (int i = 0; i < userIDs.size(); i++) {
            org.jdom.Element item = new org.jdom.Element("item").setAttribute("value", (String) userIDs.get(i)).setAttribute("label", (String) userIDs.get(i));
            root.addContent(item);
        }

        org.jdom.Document jdomDoc = new org.jdom.Document(root);
        getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdomDoc);
    }

    /**
     * This method retrieves the all users list
     * 
     * @param job
     *            The MCRServletJob instance
     * @param currentPrivs
     *            The current privlegs in ArrayList
     * 
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void retrieveAllUserXML(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!AI.checkPermission("administrate-user")) {
            showNoPrivsPage(job);

            return;
        }

        try {
            org.jdom.Document userlist = MCRUserMgr.instance().getAllUsers();
            doLayout(job, "ListAllUser", userlist, false);
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }

        return;
    }

    /**
     * This method is still experimental! It is needed in the use case "modify
     * user".
     */
    private void retrieveUserXML(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!AI.checkPermission("modify-user")) {
            showNoPrivsPage(job);

            return;
        }

        try {
            String userID = getProperty(job.getRequest(), "uid");
            MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
            org.jdom.Document jdomDoc = user.toJDOMDocument();
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdomDoc);
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }

        return;
    }

    /**
     * This method is still experimental! It is needed in the use case "modify
     * group".
     */
    private void retrieveGroupXML(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!AI.checkPermission("modify-group")) {
            showNoPrivsPage(job);

            return;
        }

        try {
            String groupID = getProperty(job.getRequest(), "gid");
            MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
            org.jdom.Document jdomDoc = group.toJDOMDocument();
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdomDoc);
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a group object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }

        return;
    }

    /**
     * This method reads the XML data sent by the MyCoRe editor framework.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getEditorSubmission(MCRServletJob job) throws IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();

        // Read the XML data sent by the editor
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        org.jdom.Document jdomDoc = sub.getXML();

        // Read the request parameters
        MCRRequestParameters parms;

        if (sub == null) {
            parms = new MCRRequestParameters(job.getRequest());
        } else {
            parms = sub.getParameters();
        }

        String useCase = parms.getParameter("usecase");

        // Determine the use case
        if ((useCase.equals("create-user") || useCase.equals("modify-user")) && jdomDoc.getRootElement().getName().equals("mycoreuser")) {
            String numID = Integer.toString(MCRUserMgr.instance().getMaxUserNumID() + 1);
            jdomDoc.getRootElement().getChild("user").setAttribute("numID", numID);

            if (jdomDoc.getRootElement().getChild("user").getAttributeValue("id_enabled") == null) {
                jdomDoc.getRootElement().getChild("user").setAttribute("id_enabled", "false");
            }

            if (jdomDoc.getRootElement().getChild("user").getAttributeValue("update_allowed") == null) {
                jdomDoc.getRootElement().getChild("user").setAttribute("update_allowed", "false");
            }

            org.jdom.Element userElement = jdomDoc.getRootElement().getChild("user");

            try {
                if (useCase.equals("create-user")) {
                    MCRUser newUser = new MCRUser(userElement, true);
                    MCRUserMgr.instance().createUser(newUser);

                    LOGGER.info("User " + currentUserID + " has successfully created the new user: " + newUser.getID());
                } else {
                    MCRUser thisUser = new MCRUser(userElement, true);
                    MCRUserMgr.instance().updateUser(thisUser);
                }

                // doLayout(job, "xml", jdomDoc, true);
                showOkPage(job);
            } catch (MCRException ex) {
                generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage(), ex, false);

                return;
            }
        } else if ((useCase.equals("create-group") || useCase.equals("modify-group")) && jdomDoc.getRootElement().getName().equals("mycoregroup")) {
            try {
                String groupID = jdomDoc.getRootElement().getChild("group").getAttributeValue("ID");
                if (groupID == null)
                    throw new MCRException("groupid is not valid");
                MCRGroup group = new MCRGroup(jdomDoc.getRootElement().getChild("group"));
                if (useCase.equals("create-group")) {
                    MCRUserMgr.instance().createGroup(group);
                } else {
                    MCRUserMgr.instance().updateGroup(group);
                }
                // doLayout(job, "xml", jdomDoc, true);
                showOkPage(job);
            } catch (MCRException ex) {
                generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage(), ex, false);
                return;
            }
        } else {
            // TODO: error message
        }
    }
}
