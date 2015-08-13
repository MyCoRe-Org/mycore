/*
 * 
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

package org.mycore.sword;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.server.SWORDServer;

/**
 * For accessing the SWORD deposit system a service document can be retrieved
 * which describes the workspace and all collections. The service document is
 * delivered as a XML document, if authentification was successful. An instance
 * of this class is able to generate a service document with one workspace and
 * one collection.
 * 
 * @see <a href="http://www.swordapp.org/docs/sword-profile-1.3.html">SWORD
 *      APP Profile</a>
 * @author Nils Verheyen
 * 
 */
public class MCRSWORDServiceDocumentServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(MCRSWORDServiceDocumentServlet.class);

    /** The repository */
    private SWORDServer myRepository;

    /** contains the authenticator used inside this servlet */
    private MCRSWORDAuthenticator authenticator;

    @Override
    public void init() throws ServletException {

        myRepository = MCRSWORDUtils.createServer();
        authenticator = MCRSWORDUtils.createAuthenticator();
    }

    /**
     * If authentification was successful a {@link ServiceDocumentRequest} is
     * send to the concrete implementation of the {@link SWORDServer}. The
     * server itself returns a {@link ServiceDocument}, which can be written to
     * the output of the {@link HttpServletResponse}.
     */
    @Override
    protected void doGet(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        
        LOG.info("executing new SWORD service document request");

        // Create the ServiceDocumentRequest
        ServiceDocumentRequest sdr = new ServiceDocumentRequest();

        // Are there any authentication details?
        Pair<String, String> requestAuthData = MCRSWORDUtils.readBasicAuthData(request);
        if (requestAuthData != null && authenticator.authenticate(requestAuthData.first, requestAuthData.second)) {

            LOG.info("requesting user: " + requestAuthData.first);
            
            sdr.setUsername(requestAuthData.first);
            sdr.setPassword(requestAuthData.second);
        } else {
            
            LOG.info("unauthorized request for service document send -> host: " + request.getRemoteHost());

            String s = "BASIC realm=\"" + MCRSWORDUtils.getAuthRealm() + "\"";
            response.setHeader("WWW-Authenticate", s);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Set the x-on-behalf-of header
        sdr.setOnBehalfOf(request.getHeader(HttpHeaders.X_ON_BEHALF_OF));
        LOG.info("on behalf of user: " + sdr.getOnBehalfOf());

        // Set the IP address
        sdr.setIPAddress(request.getRemoteAddr());

        // Set the deposit location
        String postUriAddition = MCRConfiguration.instance().getString("MCR.SWORD.post.uri", "SwordDeposit");
        sdr.setLocation(MCRFrontendUtil.getBaseURL() + postUriAddition);

        // Get the ServiceDocument from "SWORDServer" instance
        try {
            ServiceDocument sd = myRepository.doServiceDocument(sdr, job);

            // Print out the Service Document
            response.setContentType("application/atomsvc+xml; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(sd.marshall());
            out.flush();
        } catch (SWORDAuthenticationException sae) {
            if ("Basic".equalsIgnoreCase(MCRSWORDUtils.getAuthN())) {
                String s = "BASIC realm=\"" + MCRSWORDUtils.getAuthRealm() + "\"";
                response.setHeader("WWW-Authenticate", s);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (SWORDErrorException see) {
            // Return the relevant HTTP status code
            LOG.error(see.getMessage());
            MCRSWORDUtils.makeErrorDocument(see.getErrorURI(), see.getStatus(), see.getDescription(), request, response);
        } catch (SWORDException se) {
            se.printStackTrace();
            // Throw a HTTP 500
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage());
        }
    }
}
