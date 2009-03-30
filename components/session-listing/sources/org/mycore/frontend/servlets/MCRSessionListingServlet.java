/*
 * 
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

import java.util.ArrayList;
import java.util.Collection;

import org.jdom.Document;
import org.jdom.Element;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.user.MCRUserContact;
import org.mycore.user.MCRUserMgr;

public class MCRSessionListingServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    public void doGetPost(MCRServletJob job) throws Exception {

        if (!MCRAccessManager.checkPermission("manage-sessions"))
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + "access_deny.xml"));

        Document sessionsXML = listSessions(job);
        MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), sessionsXML);
    }

    private Document listSessions(MCRServletJob job) {
        // copy all session to new collection (fixes:
        // ConcurrentModificationException)
        Collection<MCRSession> sessions = new ArrayList<MCRSession>(MCRSessionMgr.getAllSessions().values());
        Element sessionsXML = new Element("sessionListing");
        MCRUserMgr um = MCRUserMgr.instance();
        for (MCRSession session : sessions) {
            Element sessionXML = new Element("session");
            sessionXML.addContent(new Element("id").setText(session.getID()));
            sessionXML.addContent(new Element("login").setText(session.getCurrentUserID()));
            sessionXML.addContent(new Element("ip").setText(session.getCurrentIP()));
            if (session.getCurrentUserID() != null) {
                MCRUserContact userContacts = um.retrieveUser(session.getCurrentUserID()).getUserContact();
                String userRealName = userContacts.getFirstName() + " " + userContacts.getLastName();
                sessionXML.addContent(new Element("userRealName").setText(userRealName));
            }
            sessionXML.addContent(new Element("createTime").setText(Long.toString(session.getCreateTime())));
            sessionXML.addContent(new Element("lastAccessTime").setText(Long.toString(session.getLastAccessedTime())));
            sessionXML.addContent(new Element("loginTime").setText(Long.toString(session.getLoginTime())));
            sessionsXML.addContent(sessionXML);
        }
        return new Document(sessionsXML);
    }

}
