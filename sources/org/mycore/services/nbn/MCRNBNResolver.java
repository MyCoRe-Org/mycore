/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.services.nbn;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;

/**
 * This servlet resolves a given NBN URN from a HTTP request and redirects the
 * client to the URL that is stored for this NBN. The URN can be the query
 * string or the request path parameter. If the URN is valid, but not local, the
 * request is redirected to the national URN resolver, as specified by the
 * configuration parameter MCR.NBN.TopLevelResolver.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRNBNResolver extends HttpServlet {
    /** Logger */
    static Logger logger = Logger.getLogger(MCRNBNResolver.class);

    /** The URL of the non-local URN resolver script */
    protected String resolver;

    /** Initializes the URN Resolver */
    public void init() {
        resolver = MCRConfiguration.instance().getString(
                "MCR.NBN.TopLevelResolver");
    }

    /** Handles HTTP GET requests to resolve a given URN */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        String param = req.getQueryString();

        logger.info("The servlet path: " + path);
        logger.info("The servlet's parameters: " + param);

        MCRNBN urn = null;

        if (path != null) {
            urn = new MCRNBN(path.substring(1));
        } else {
            if (param != null) {
                urn = new MCRNBN(param);
            } else {
                logger.info("No information given to extract URN information.");
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        if (!urn.isValid()) {
            logger.info("The URN " + urn.toString() + "is not valid.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!urn.isLocal()) {
            logger.info("The URN " + urn.toString() + "is not local.");
            res.sendRedirect(resolver + urn.getNBN());
            return;
        }

        String url = urn.getURL();
        if (url == null) {
            logger.info("No URL found in store for the URN " + urn.toString()
                    + ".");
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            res.sendRedirect(url);
        }
    }

}