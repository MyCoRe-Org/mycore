/*
 * 
 * $Revision: 21346 $ $Date: 2011-06-30 14:53:10 +0200 (Do, 30. Jun 2011) $
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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.SWORDServer;

/**
 * After a successful deposit, an atom document pointing to the deposit content
 * should be available. With the use of this
 * <code>MCRSWORDAtomDocumentServlet</code> one is able to retrieve teh atom
 * document. An instance of this servlet uses the configured
 * {@link MCRSWORDServer} to create the atom document.
 * 
 * @author Nils Verheyen
 * 
 */
public class MCRSWORDAtomDocumentServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(MCRSWORDAtomDocumentServlet.class.getName());

    private SWORDServer myRepository;

    private MCRSWORDAuthenticator authenticator;

    /**
     * Initialise the servlet.
     */
    public void init() throws ServletException {

        myRepository = MCRSWORDUtils.createServer();
        authenticator = MCRSWORDUtils.createAuthenticator();
    }

    /**
     * Process the get request.
     */
    protected void doGet(MCRServletJob job) throws ServletException, IOException {

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        try {
            // Create the atom document request object
            AtomDocumentRequest adr = new AtomDocumentRequest();

            // Are there any authentication details?
            Pair<String, String> requestAuthData = MCRSWORDUtils.readBasicAuthData(request);
            if (requestAuthData != null && authenticator.authenticate(requestAuthData.first, requestAuthData.second)) {

                LOG.info("requesting user: " + requestAuthData.first);

                adr.setUsername(requestAuthData.first);
                adr.setPassword(requestAuthData.second);
            } else {

                LOG.info("unauthorized request for atom document send -> host: " + request.getRemoteHost());

                String s = "BASIC realm=\"" + MCRSWORDUtils.getAuthRealm() + "\"";
                response.setHeader("WWW-Authenticate", s);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Set the IP address
            adr.setIPAddress(request.getRemoteAddr());

            // Generate the response
            AtomDocumentResponse dr = myRepository.doAtomDocument(adr, job);

            // Print out the Deposit Response
            response.setStatus(dr.getHttpResponse());
            response.setContentType("application/atom+xml; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(dr.marshall());
            out.flush();
        } catch (SWORDAuthenticationException sae) {
            // Ask for credentials again
            String s = "Basic realm=\"SWORD\"";
            response.setHeader("WWW-Authenticate", s);
            response.setStatus(401);
        } catch (SWORDException se) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            se.printStackTrace();
        } catch (SWORDErrorException see) {
            // Get the details and send the right SWORD error document
            LOG.error(see.getMessage());
            MCRSWORDUtils.makeErrorDocument(see.getErrorURI(), see.getStatus(), see.getDescription(), request, response);
        }
    }

    /**
     * Utility method to construct the URL called for this Servlet
     * 
     * @param req
     *            The request object
     * @return The URL
     */
    protected static String getUrl(HttpServletRequest req) {
        String reqUrl = req.getRequestURL().toString();
        String queryString = req.getQueryString();
        if (queryString != null) {
            reqUrl += "?" + queryString;
        }
        return reqUrl;
    }
}
