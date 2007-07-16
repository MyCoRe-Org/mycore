/*
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
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRConfiguration;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserMgr;
import org.mycore.services.i18n.*;

/**
 * This servlet handles requests from the user interface for group and user
 * administration, with help from Ajax
 * 
 * @author Radi Radichev
 * 
 */
public class MCRUserAjaxServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRUserMgr.class.getName());

    private MCRConfiguration CONFIG;

    public MCRUserAjaxServlet() {
        CONFIG = MCRConfiguration.instance();
    }

    /**
     * Modes: users - get all data( groups with users ) update- if username is
     * "undefined" get the current group with all users in it. else put the user
     * in the group and show updated version of the group. delete- if the group
     * is "null" then delete the user from the system. else delete the user from
     * the group
     */
    public void doGetPost(MCRServletJob job) throws IOException {

        String mode = getProperty(job.getRequest(), "mode");
        String username = getProperty(job.getRequest(), "user");
        String group = getProperty(job.getRequest(), "group");
        if (mode == null) {
            buidPage(job);
        } else if (mode.equals("users")) {
            getData(job, "");
        } else if (mode.equals("update")) {
            if (username.equals("undefined")) {
                getGroup(job, group, "0");
            } else {
                updateGroup(group, username, job);
                getGroup(job, group, "0");
            }

        } else if (mode.equals("delete")) {
            if (group.equals("null")) {
                if (removeUser(username) == 1) {
                    getData(job, MCRTranslation.translate("users.error.root"));
                } else {
                    getData(job, "0");
                }
            } else {
                if (deleteUser(group, username) == 1) {
                    getGroup(job, group, MCRTranslation.translate("users.error.primGroup", username + ";" + group));
                } else {
                    getGroup(job, group, "0");
                }
            }
        }
    }

    /**
     * Delete a user from the system. Checks to see if the user to be deleted is
     * the Superuser. If so it can't be done and an error message is being
     * returned.
     * 
     * @param nutzer
     * @return 0 if the user is deleted. 1 if the user is the Super user and
     *         can't be deleted.
     */
    private int removeUser(String nutzer) {
        String rootUser = CONFIG.getString("MCR.Users.Superuser.UserName");
        if (!(nutzer.equals(rootUser))) {
            MCRUserMgr.instance().deleteUser(nutzer);
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Delete the user from the group only.
     * 
     * @param gruppe
     * @param nutzer
     * @return
     */
    private int deleteUser(String gruppe, String nutzer) {
        MCRGroup group = MCRUserMgr.instance().retrieveGroup(gruppe);
        MCRUser user = MCRUserMgr.instance().retrieveUser(nutzer);
        if (user.getPrimaryGroupID().equals(gruppe)) {
            return 1;
        } else {
            group.removeMemberUserID(nutzer);
            MCRUserMgr.instance().updateGroup(group);
            return 0;
        }
    }

    /**
     * Inserts the user in the group. Updates the DB and build a new JSON
     * representation of the group and all users in it.
     * 
     * @param gruppe
     *            Group in which the user comes
     * @param nutzer
     *            User who is being moved
     * @param job
     * @throws IOException
     */
    private void updateGroup(String gruppe, String nutzer, MCRServletJob job) throws IOException {
        LOGGER.debug("User to update: " + nutzer + " , group to update: " + gruppe);
        MCRGroup group = MCRUserMgr.instance().retrieveGroup(gruppe);
        LOGGER.debug("GroupID: " + group.getID());
        group.addMemberUserID(nutzer);
        MCRUserMgr.instance().updateGroup(group);
    }

    /**
     * Build a JSON Representation of a group with all users who are in it, and
     * return it to the java script. The JSON Object json has 2 arrays.1. for
     * the group to be returned and 2. for errors groupToUpdate is a jsonobject
     * with an array with all the users in the group, and another property
     * "name" with the name of the group.
     * 
     * @param job
     *            The group to be returned as JSON Doc.
     * @param gruppe -
     *            The group to be returned as JSON Doc.
     * @throws IOException
     */
    private void getGroup(MCRServletJob job, String gruppe, String Msg) throws IOException {
        MCRGroup group = MCRUserMgr.instance().retrieveGroup(gruppe);

        JSONObject json = new JSONObject();
        JSONObject groupToUpdate = new JSONObject();
        JSONArray users = new JSONArray();
        JSONArray error = new JSONArray();
        ArrayList members = group.getMemberUserIDs();

        if (!(Msg.equals(""))) {
            error.put(Msg);
        } else {
            error.put("none");
        }

        Iterator it = members.iterator();
        while (it.hasNext()) {
            users.put(it.next());
        }
        groupToUpdate.put("name", gruppe);
        groupToUpdate.put("users", users);
        json.put("gruppe", groupToUpdate);
        json.put("error", error);
        LOGGER.debug("JSON STRING" + json.toString());
        job.getResponse().setContentType("application/x-json");
        job.getResponse().getWriter().print(json);
    }

    /**
     * Builds a JSON Representation of all groups with all the users in the
     * database and returns the JSON string to the java script. The root of the
     * document is a JSON Object (json). There are 3 JSON Arrays in it: groups,
     * users and error The group array is an array of json objects with name:
     * groupID and another array with all the users in the current group The
     * users array is just an array with all the users in the system. If an
     * error occurs it can be send as a string parameter and it would be in the
     * error array.
     * 
     * @param job
     * @throws IOException
     */
    private void getData(MCRServletJob job, String Msg) throws IOException {
        List<String> userIDs = MCRUserMgr.instance().getAllUserIDs();
        List<String> groupsIDs = MCRUserMgr.instance().getAllGroupIDs();

        JSONObject json = new JSONObject();
        JSONArray users = new JSONArray();
        JSONArray groups = new JSONArray();
        JSONArray error = new JSONArray();

        Iterator it = userIDs.iterator();
        while (it.hasNext()) {
            users.put(it.next());
        }

        it = groupsIDs.iterator();
        while (it.hasNext()) {
            String groupID = it.next().toString();
            ArrayList memUsers = MCRUserMgr.instance().retrieveGroup(groupID).getMemberUserIDs();
            JSONArray memUserList = new JSONArray();
            JSONObject group = new JSONObject();
            Iterator iter = memUsers.iterator();
            while (iter.hasNext()) {
                memUserList.put(iter.next());
            }
            group.put("name", groupID);
            group.put("users", memUserList);
            groups.put(group);
        }
        if (!(Msg.equals(""))) {
            error.put(Msg);
        } else {
            error.put("none");
        }

        json.put("users", users);
        json.put("groups", groups);
        json.put("error", error);
        LOGGER.debug("JSON STRING" + json.toString());
        job.getResponse().setContentType("application/x-json");
        job.getResponse().getWriter().print(json);
    }

    private void buidPage(MCRServletJob job) throws IOException {
        org.jdom.Element root = new org.jdom.Element("mcr_user");

        org.jdom.Document document = new Document(root);
        doLayout(job, "UserAjax", document);
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
