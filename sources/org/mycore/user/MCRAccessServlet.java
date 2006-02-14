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

package org.mycore.user;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet returns a XML Object that contains the access check result.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 * @deprecated use org.mycore.access.MCRAccessCheckServlet
 */
public class MCRAccessServlet extends MCRServlet {
    private static Logger LOGGER = Logger.getLogger(MCRAccessServlet.class);

    /**
     * This method overrides doGetPost of MCRServlet. <br />
     * The method looks for the parameters ip and privilege. As minimum one
     * parameter must be set, else the servlet retuns false. <br />
     * The servlet get the privilages of the user of the current session context
     * and check them against the value of access. Then it looks for the own IP
     * address and compares them with the given host or domain name. If both (or
     * one it it is only one parameter) are true the result of the retuned XML
     * stream is the following. The syntax of the stream is <br />
     * <br />
     * &lt;mycoreaccess&gt; <br />
     * &lt;access return="true"&gt; <br />
     * &lt;/mycoreaccess&gt; <br />
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        boolean retpriv = false;
        boolean retip = false;
        boolean retuser = false;
        boolean result = false;

        // read the parameter
        // mode parameter 'reader' or 'editor'
        String mode = getProperty(job.getRequest(), "mode");

        if (mode == null) {
            mode = "reader";
        }

        mode.trim().toLowerCase();

        if ((!mode.equals("reader")) && (!mode.equals("editor"))) {
            mode = "reader";
        }

        // ip parameter 'ip' or 'ipmask/subnetmask'
        String ip = getProperty(job.getRequest(), "ip");

        if (ip == null) {
            ip = "";
        } else {
            ip = ip.trim();
        }

        if (ip.length() == 0) {
            retip = true;
        }

        // privilege parameter
        String privilege = getProperty(job.getRequest(), "privilege");

        if (privilege == null) {
            privilege = "";
        } else {
            privilege = privilege.trim();
        }

        if (privilege.length() == 0) {
            retpriv = true;
        }

        // list of user parameter
        String userlist = getProperty(job.getRequest(), "userlist");

        if (userlist == null) {
            userlist = "";
        } else {
            userlist = userlist.trim();
        }

        if (userlist.length() == 0) {
            retuser = true;
        }

        HashSet userset = new HashSet();
        StringTokenizer st = new StringTokenizer(userlist, "_");

        while (st.hasMoreTokens()) {
            userset.add(st.nextToken());
        }

        // get the MCRSession for the current thread from the session manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String userid = mcrSession.getCurrentUserID();
        LOGGER.debug("Access check for user " + userid);
        String currip = mcrSession.getCurrentIP();
        LOGGER.debug("Access check for ip " + currip);

        // check for mode reader
        if (mode.equals("reader")) {

            // check the data
            if (!retip) {
                retip = checkIP(currip, ip);
            }

            if (!retpriv) {
                retpriv = checkPrivileg(privilege, userid);
            }

            if (LOGGER.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Access check in mode ").append(mode).append(" for ip [").append(ip).append("](").append(retip).append(") and privilege [").append(privilege).append("](").append(retpriv).append(')');
                LOGGER.debug(sb.toString());
            }

            result = retip && retpriv;
        }

        // check for mode editor
        if (mode.equals("editor")) {

            // check the data
            if (!retip) {
                retip = checkIP(currip, ip);
            }

            if (!retpriv) {
                retpriv = checkPrivileg(privilege, userid);
            }

            if (!retuser) {
                retuser = checkGroupMember(userid, userset);
            }

            if (LOGGER.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Access check in mode ").append(mode).append(" for ip [").append(ip).append("](").append(!retip).append(')').append(" and users [").append(userlist).append("](").append(!retuser).append(") and privilege [").append(privilege).append("](").append(!retpriv).append(')');
                LOGGER.debug(sb.toString());
            }

            result = retip && retpriv && retuser;
        }

        // prepare the document
        org.jdom.Element root = new org.jdom.Element("mycoreaccess");
        org.jdom.Document jdom = new org.jdom.Document(root);
        org.jdom.Element access = new org.jdom.Element("access");
        access.setAttribute("return", String.valueOf(result));
        root.addContent(access);
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        job.getRequest().setAttribute("XSL.Style", "xml");

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }

    /**
     * The method check the given host or domain name against the current ip of
     * this session.
     * 
     * @param currip
     *            the IP of the current session
     * @param ip
     *            the host or domain name
     * @return true if the current session is in the host or domain name.
     */
    boolean checkIP(String currip, String ip) {
        String ipAddr;
        String subNet;
        int i = ip.indexOf("/");

        if (i >= 0) {
            // subnet mask present
            ipAddr = ip.substring(0, i).trim();
            subNet = ip.substring(i + 1, ip.length()).trim();
        } else {
            ipAddr = ip.trim();
            subNet = "255.255.255.255";
        }

        try {
            return MCRAccessChecker.isInetAddressInSubnet(currip, ipAddr, subNet);
        } catch (UnknownHostException e) {
            LOGGER.info("Unknown Host while checking ip : " + ip, e);

            return false;
        }
    }

    /**
     * The method check the given privilege against the privilege of the user.
     * 
     * @param privilege
     *            the given privilege
     * @param userid
     *            the user with his privileges
     * @return true if the user has the privilege, else false
     */
    boolean checkPrivileg(String privilege, String userid) {
        return MCRAccessChecker.hasUserThePrivilege(privilege, userid);
    }

    /**
     * The method check that the given user is in the same group or in a group
     * that is a member of the users group.
     * 
     * @param user
     *            the user which should be checked
     * @param list
     *            the list of group users in which the user should be a member
     *            of himself group. return true if the user is in the same group
     *            or subgroup like one of the li st
     */
    boolean checkGroupMember(String user, HashSet list) {
        return MCRAccessChecker.isUserLikeListedUsers(user, list);
    }
}
