/**
 * $Revision: 23345 $ 
 * $Date: 2012-01-30 12:08:41 +0100 (Mo, 30 Jan 2012) $
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2.login;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUser2Constants;
import org.mycore.user2.MCRUserManager;

/**
 * Login user with JA-SIG Central Authentication Service (CAS).
 * The servlet validates the ticket returned from CAS and
 * builds a User object to login to the current session.
 * 
 * For /servlets/MCRCASServlet, a authentication filter must be defined
 * in web.xml. The following properties must be configured in 
 * mycore.properties:
 * 
 *  The URL of the CAS Client Servlet:
 * MCR.user2.CAS.ClientURL=http://localhost:8291/servlets/MCRCASServlet
 *
 * The Base URL of the CAS Server
 * MCR.user2.CAS.ServerURL=https://cas.uni-duisburg-essen.de/cas
 *
 * The realm the CAS Server authenticates for, as in realms.xml
 * MCR.user2.CAS.RealmID=ude
 *
 * Configure store of trusted SSL (https) server certificates
 * MCR.user2.CAS.SSL.TrustStore=/path/to/java/lib/security/cacerts
 * MCR.user2.CAS.SSL.TrustStore.Password=changeit
 * 
 * After successful login, MCRCASServlet queries an LDAP server for
 * the user's properties.
 *
 * @author Frank L\u00fctzenkirchen
 */
public class MCRCASServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    /** The logger */
    private static Logger LOGGER = LogManager.getLogger();

    /** The URL of THIS servlet */
    private String clientURL;

    /** The base URL of the CAS Server */
    private String serverURL;

    /** The realm the CAS Server authenticates for, as in realms.xml */
    private String realmID;

    @Override
    public void init() throws ServletException {
        super.init();

        MCRConfiguration config = MCRConfiguration.instance();
        clientURL = config.getString(MCRUser2Constants.CONFIG_PREFIX + "CAS.ClientURL");
        serverURL = config.getString(MCRUser2Constants.CONFIG_PREFIX + "CAS.ServerURL");
        realmID = config.getString(MCRUser2Constants.CONFIG_PREFIX + "CAS.RealmID");

        // Set properties to enable SSL connection to CAS and accept certificates  
        String trustStore = config.getString(MCRUser2Constants.CONFIG_PREFIX + "CAS.SSL.TrustStore");
        String trustStorePassword = config.getString(MCRUser2Constants.CONFIG_PREFIX + "CAS.SSL.TrustStore.Password");

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String ticket = req.getParameter("ticket");
        if ((ticket == null) || (ticket.trim().length() == 0)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Validate ticket at CAS server
        Cas20ProxyTicketValidator sv = new Cas20ProxyTicketValidator(serverURL);
        sv.setAcceptAnyProxy(true);
        Assertion a = sv.validate(ticket, clientURL);
        AttributePrincipal principal = a.getPrincipal();

        // Get user name logged in
        String userName = principal.getName();
        LOGGER.info("Login " + userName);

        MCRUser user;
        boolean userExists = MCRUserManager.exists(userName, realmID);
        if (userExists)
            user = MCRUserManager.getUser(userName, realmID);
        else
            user = new MCRUser(userName, realmID);

        // Get user properties from LDAP server
        boolean userChanged = MCRLDAPClient.instance().updateUserProperties(user);
        if (userChanged && userExists) {
            MCRUserManager.updateUser(user);
        }

        // Store login user in session and redirect browser to target url
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        // MCR-1154
        req.changeSessionId();
        MCRLoginServlet.redirect(res);
    }
}
