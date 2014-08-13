/*
 * $Id$
 * $Revision: 5697 $ $Date: Aug 16, 2013 $
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

package org.mycore.user2.login;

import static org.mycore.user2.login.MCRLoginServlet.HTTPS_ONLY_PROPERTY;
import static org.mycore.user2.login.MCRLoginServlet.LOCAL_LOGIN_SECURE_ONLY;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRContainerLoginServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRRealm;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRServlet3LoginServlet extends MCRContainerLoginServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRServlet3LoginServlet.class);

    @Override
    public void init() throws ServletException {
        if (!LOCAL_LOGIN_SECURE_ONLY) {
            LOGGER.warn("Login over unsecure connection is permitted. Set '" + HTTPS_ONLY_PROPERTY
                + "=true' to prevent cleartext transmissions of passwords.");
        }
        super.init();
    }

    @Override
    protected void think(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (LOCAL_LOGIN_SECURE_ONLY && !req.isSecure()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, getErrorI18N("component.user2.login", "httpsOnly"));
            return;
        }
        String uid = getProperty(req, "uid");
        String pwd = getProperty(req, "pwd");
        String realm = getProperty(req, "realm");
        if (uid != null && pwd != null) {
            MCRSession session = MCRSessionMgr.getCurrentSession();
            req.login(uid, pwd);
            session.setUserInformation(new Servlet3ContainerUserInformation(session, realm, uid, pwd));
            LOGGER.info("Logged in: " + session.getUserInformation().getUserID());
        }
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (ex != null) {
            if (ex instanceof ServletException) {
                //Login failed
                presentLoginForm(req, res, (ServletException) ex);
            } else {
                throw ex;
            }
        }
        if (!res.isCommitted()) {
            String uid = getProperty(req, "uid");
            if (uid == null) {
                presentLoginForm(req, res, null);
            }
            if (!job.getResponse().isCommitted())
                super.render(job, ex);
        }
    }

    private void presentLoginForm(HttpServletRequest req, HttpServletResponse res, ServletException ex)
        throws IOException, TransformerException, SAXException {
        Element root = new Element("login");
        MCRLoginServlet.addCurrentUserInfo(root);
        root.addContent(new org.jdom2.Element("returnURL").addContent(MCRLoginServlet.getReturnURL(req)));
        req.setAttribute("XSL.FormTarget", req.getRequestURI());
        String realm = getProperty(req, "realm");
        if (realm != null) {
            req.setAttribute("XSL.Realm", realm);
        }
        if (ex != null) {
            //Login failed
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            root.setAttribute("loginFailed", "true");
            root.addContent(new Element("errorMessage").setText(ex.getMessage()));
        }
        getLayoutService().doLayout(req, res, new MCRJDOMContent(root));
    }

    private static class Servlet3ContainerUserInformation extends ContainerUserInformation {

        private String user, pwd, realm;

        public Servlet3ContainerUserInformation(MCRSession session, String realm, String user, String pwd) {
            super(session);
            this.realm = realm;
            this.user = user;
            this.pwd = pwd;
        }

        private void loginIfNeeded() {
            HttpServletRequest currentRequest = getCurrentRequest();
            if (currentRequest != null && currentRequest.getUserPrincipal() == null) {
                try {
                    currentRequest.login(user, pwd);
                    LOGGER.debug("Re-Logged in: " + user);
                } catch (ServletException e) {
                    throw new MCRException(e);
                }
            }
        }

        @Override
        public String getUserID() {
            loginIfNeeded();
            return super.getUserID();
        }

        @Override
        public boolean isUserInRole(String role) {
            loginIfNeeded();
            return super.isUserInRole(role);
        }

        @Override
        public String getUserAttribute(String attribute) {
            if (attribute.equals(MCRRealm.USER_INFORMATION_ATTR)) {
                return this.realm;
            }
            return super.getUserAttribute(attribute);
        }

    }
}
