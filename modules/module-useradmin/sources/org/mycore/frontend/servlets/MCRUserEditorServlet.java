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

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.editor2.MCREditorSubmission;
import org.mycore.frontend.editor2.MCRRequestParameters;
import org.mycore.user.MCRUserMgr;
import org.mycore.user.MCRUser;

/**
 * This servlet provides a web interface for the editors of the user management 
 * of the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */

public class MCRUserEditorServlet extends MCRUserAdminGUICommons
{
    private static final Logger LOGGER = Logger.getLogger(MCRUserEditorServlet.class);

    /** Initialisation of the servlet */
    public void init()
    throws ServletException
    { super.init(); }
    
    /**
     * This method overrides doGetPost of MCRServlet and handles HTTP requests. Depending
     * on the mode parameter in the request, the method dispatches actions to be done by
     * subsequent private methods. In contrast to the MCRUserAdminServlet - which serves
     * as an entry point and dispatcher for all use cases of the user management GUI - this
     * servlet is called by the MyCoRe editor framework, triggered by the editor definition
     * files of the user management GUI. In addition, this servlet is the target of the 
     * MyCoRe editor framework for the user management GUI, i.e. it reads the editor 
     * submission and acts accordingly.  
     * 
     * @param  job
     *             The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    public void doGetPost(MCRServletJob job) 
    throws IOException, ServletException
    {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();
        ArrayList currentPrivs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(currentUserID);

        String mode = getProperty(job.getRequest(), "mode");
        if (mode == null) {mode = "xml";}
        if (mode.equals("getAssignableGroupsForUser"))
            getAssignableGroupsForUser(job, currentPrivs);
        else if (mode.equals("retrieveuserxml")) 
            retrieveUserXML(job, currentPrivs);
        else if (mode.equals("xml"))
            getEditorSubmission(job);
        else { // no valid mode
            String msg = "The request did not contain a valid mode for this servlet!"; 
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }
    
    /**
     * This method retrieves a list of groups the current user may assign to a new or
     * existing user account. Typically this servlet mode is implicitly called by an
     * MyCoRe editor definition file, e.g. to fill drop down boxes or lists in the user
     * administration GUI.
     *   
     * @param  job
     *             The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getAssignableGroupsForUser(MCRServletJob job, ArrayList currentPrivs) 
    throws IOException, ServletException
    {
        // Get the MCRSession object for the current thread from the session manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();
        ArrayList groupIDs = null;
        
        try {
            // The list of assignable groups depends on the privileges of the current user.
            MCRUser currentUser = MCRUserMgr.instance().retrieveUser(currentUserID);
            if (currentPrivs.contains("user administrator")) {
                groupIDs = MCRUserMgr.instance().getAllGroupIDs();
            } else if (currentPrivs.contains("create user")) {
                groupIDs = currentUser.getAllGroupIDs();
            } else {  
                // There are no privileges to assign any groups to a new user.
                // Possibly someone tried to call the new user form directly without
                // checking the privileges using the MCRUserAdminServlet first.
                
                LOGGER.warn("MCRUserEditorServlet: not enough privileges! " +
                            "Someone might have tried to call the new user form directly.");
                
                // TODO: Hier muss irgendwie eine vernünftige Fehlermeldung her!
                // Aktuell spielt das MCREditorServlet noch nicht mit. Die folgenden
                // Zeilen z.B. führen zu einem "impossible to open input stream"
                // Fehler im MCREditorServlet (weil halt error 403 gesendet wird)
                
                String msg = "You do not have enough privileges for this use case!";
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        catch (MCRException ex) {  
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!"; 
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        // Loop over all assignable group IDs
        org.jdom.Element root = new org.jdom.Element("items");
        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element item = new org.jdom.Element("item")
                    .setAttribute("value", (String)groupIDs.get(i))
                    .setAttribute("label", (String)groupIDs.get(i));
            root.addContent(item);
        }
        org.jdom.Document jdomDoc = new org.jdom.Document(root);
        forwardXML(job, jdomDoc);
    }

    /**
     *  This method is still experimental! It is needed in the use case "modify user".
     */
    private void retrieveUserXML(MCRServletJob job, ArrayList currentPrivs) 
    throws IOException, ServletException
    {
        // We first check the privileges for this use case
        if (!currentPrivs.contains("user administrator") && 
            !currentPrivs.contains("modify user")) {
            showNoPrivsPage(job);
            return;
        }
        
        String userID = getProperty(job.getRequest(), "uid");
        try {
            MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
            org.jdom.Document jdomDoc = user.toJDOMDocument();
            forwardXML (job, jdomDoc);
        }
        catch (MCRException ex) {   
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!"; 
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
        return;
    }
    
    /**
     * This method reads the XML data sent by the MyCoRe editor framework.
     * 
     * @param  job
     *             The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getEditorSubmission(MCRServletJob job) 
    throws IOException, ServletException
    {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getCurrentUserID();
        
        // Read the XML data sent by the editor
        MCREditorSubmission sub = 
            (MCREditorSubmission)(job.getRequest().getAttribute("MCREditorSubmission"));
        org.jdom.Document jdomDoc = sub.getXML();
        
        // Read the request parameters
        MCRRequestParameters parms;
        if (sub == null)
            parms = new MCRRequestParameters(job.getRequest());
        else
            parms = sub.getParameters();
        String useCase = parms.getParameter("usecase");
        
        // Determine the use case 
        if (useCase.equals("create-user") && 
            jdomDoc.getRootElement().getName().equals("mycoreuser"))
        {
            String numID = Integer.toString(MCRUserMgr.instance().getMaxUserNumID() + 1);
            jdomDoc.getRootElement().getChild("user").setAttribute("numID", numID);
            if (jdomDoc.getRootElement().getChild("user").getAttributeValue("id_enabled") == null){
                jdomDoc.getRootElement().getChild("user").setAttribute("id_enabled", "false");
            }
            if (jdomDoc.getRootElement().getChild("user").getAttributeValue("update_allowed") == null){
                jdomDoc.getRootElement().getChild("user").setAttribute("update_allowed", "false");
            }    
            
            org.jdom.Element userElement = jdomDoc.getRootElement().getChild("user"); 
            
            try {
                MCRUser newUser = new MCRUser(userElement, true);
                MCRUserMgr.instance().createUser(newUser);
                
                LOGGER.info("User "+currentUserID+" has successfully created the new user: "
                            +newUser.getID());
                
                //doLayout(job, "xml", jdomDoc, true);
                showOkPage(job);
            }
            catch (MCRException ex) {
                generateErrorPage(job.getRequest(), job.getResponse(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ex.getMessage(), ex, false);
                return;
            }
        }
        else {
            // TODO: error message
        }
    }
}

