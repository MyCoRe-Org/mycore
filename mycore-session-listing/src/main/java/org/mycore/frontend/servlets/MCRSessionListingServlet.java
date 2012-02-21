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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.ifs2.MCRContent;
import org.mycore.services.i18n.MCRTranslation;

public class MCRSessionListingServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    public void doGetPost(MCRServletJob job) throws Exception {
        if (!MCRAccessManager.checkPermission("manage-sessions")) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, MCRTranslation.translate("component.session-listing.page.text"));
            return;
        }
        Document sessionsXML = getSessions();
        MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), MCRContent.readFrom(sessionsXML));
    }

    private Document getSessions() {
        // copy all session to new collection (fixes: ConcurrentModificationException)
        Collection<MCRSession> sessions = new ArrayList<MCRSession>(MCRSessionMgr.getAllSessions().values());
        Element sessionsXML = new Element("sessionListing");
        for (MCRSession session : sessions) {
            Element sessionXML = new Element("session");
            sessionXML.addContent(new Element("id").setText(session.getID()));
            String currentUserID = session.getUserInformation().getUserID();
            sessionXML.addContent(new Element("login").setText(currentUserID));
            sessionXML.addContent(new Element("ip").setText(session.getCurrentIP()));
            try {
                InetAddress inetAddress = InetAddress.getByName(session.getCurrentIP());
                String hostname = inetAddress.getHostName();
                if (hostname != null) {
                    Element hostnameElement = new Element("hostname");
                    hostnameElement.setText(hostname);
                    sessionXML.addContent(hostnameElement);
                }
            } catch (UnknownHostException e1) {
                Logger.getLogger(MCRSessionListingServlet.class).warn("Could not resolve host", e1);
            }
            if (currentUserID != null) {
                String userRealName = session.getUserInformation().getUserAttribute(MCRUserInformation.ATT_REAL_NAME);
                if (userRealName != null) {
                    sessionXML.addContent(new Element("userRealName").setText(userRealName));
                }
            }
            sessionXML.addContent(new Element("createTime").setText(Long.toString(session.getCreateTime())));
            sessionXML.addContent(new Element("lastAccessTime").setText(Long.toString(session.getLastAccessedTime())));
            sessionXML.addContent(new Element("loginTime").setText(Long.toString(session.getLoginTime())));
            Element cst = new Element("constructingStackTrace");
            sessionXML.addContent(cst);
            for (StackTraceElement se : session.getConstructingStackTrace()) {
                Element e = new Element("e");
                e.setAttribute("c", se.getClassName());
                e.setAttribute("f", se.getFileName());
                e.setAttribute("m", se.getMethodName());
                e.setAttribute("l", Long.toString(se.getLineNumber()));
                cst.addContent(e);
            }
            //if session still valid
            if (session.getID() != null)
                sessionsXML.addContent(sessionXML);
        }
        return new Document(sessionsXML);
    }
}
