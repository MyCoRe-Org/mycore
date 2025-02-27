/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.wfc.actionmapping;

import java.io.Serial;
import java.net.URI;
import java.util.List;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import com.google.common.base.Splitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Maps collection and action to a redirect URL.
 * call /servlets/MCRActionMappingServlet/{collection}/{action}
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRActionMappingServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Splitter PATH_SPLITTER = Splitter.on('/').trimResults().omitEmptyStrings().limit(2);

    @Override
    protected void doGet(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        String pathInfo = request.getPathInfo();
        HttpServletResponse response = job.getResponse();
        if (pathInfo != null) {
            List<String> splitted = PATH_SPLITTER.splitToList(pathInfo);
            if (splitted.size() == 2) {
                String collection = splitted.get(0);
                String action = splitted.get(1);
                String url = MCRURLRetriever.getURLforCollection(action, collection, true);
                if (url != null) {
                    //MCR-1172 check if we redirect to a valid URI
                    URI uri = URI.create(url);
                    response.sendRedirect(response.encodeRedirectURL(uri.toString()));
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            //misses action
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        //should never happen:
        response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
    }

}
