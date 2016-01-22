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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.frontend.filter.MCRRequestAuthenticationFilter;
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

    private static Logger LOGGER = LogManager.getLogger();

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
            session.setUserInformation(new Servlet3ContainerUserInformation(session, realm));
            req.getSession().setAttribute(MCRRequestAuthenticationFilter.SESSION_KEY, Boolean.TRUE);
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
        throws IOException, TransformerException, SAXException, JAXBException {
        String returnURL = MCRLoginServlet.getReturnURL(req);
        String formAction = req.getRequestURI();
        MCRLogin loginForm = new MCRLogin(MCRSessionMgr.getCurrentSession().getUserInformation(), returnURL,
            formAction);
        MCRLoginServlet.addCurrentUserInfo(loginForm);
        String realm = getProperty(req, "realm");
        if (realm != null) {
            req.setAttribute("XSL.Realm", realm);
        }
        MCRLoginServlet.addFormFields(loginForm, realm);
        if (ex != null) {
            //Login failed
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            loginForm.setLoginFailed(true);
            loginForm.setErrorMessage(ex.getMessage());
        }
        getLayoutService().doLayout(req, res, new MCRJAXBContent<>(JAXBContext.newInstance(MCRLogin.class), loginForm));
    }

    private static class Servlet3ContainerUserInformation extends ContainerUserInformation {

        private String realm;

        public Servlet3ContainerUserInformation(MCRSession session, String realm) {
            super(session);
            this.realm = realm;
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
