/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.urn.services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet resolves a given URN (urn:nbn:de) from a HTTP request and
 * redirects the client to the document that is registered for this URN. The URN
 * can be either given as the query string or as the request path. If the URN is
 * assigned to a local document, the request is redirected to the frontpage that
 * displays the document's metadata, as specified by the configuration property
 * MCR.URN.Resolver.DocumentURL. If the URN is not local, the request is
 * redirected to another URN resolver, as specified by the configuration
 * property MCR.URN.Resolver.MasterURL.
 * 
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
@Deprecated
public class MCRURNResolver extends MCRServlet {

    private static final Logger LOGGER = LogManager.getLogger(MCRURNResolver.class);

    protected String masterURL;

    protected String documentURL;

    @Override
    public void init() throws ServletException {
        super.init();
        String base = "MCR.URN.Resolver.";
        masterURL = MCRConfiguration.instance().getString(base + "MasterURL");
        documentURL = MCRConfiguration.instance().getString(base + "DocumentURL");
    }

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String path = req.getPathInfo();

        String urn = req.getQueryString();

        if (urn == null && path != null) {
            urn = path.substring(1).trim();
        }

        if (urn == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        LOGGER.info("Resolving URN {}", urn);

        String docID = MCRURNManager.getDocumentIDforURN(urn);

        if (docID == null) {
            res.sendRedirect(masterURL + urn);
        } else {
            res.sendRedirect(documentURL + docID);
        }
    }
}
