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

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRPersistenceServletFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getServletPath().length() > 0) {
            String url = getURL(req);
            if (url != null) {
                prepareRequest(req);
                req.getRequestDispatcher(url).forward(req, response);
                return;
            }
        }
        chain.doFilter(req, response);
    }

    private void prepareRequest(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            switch (entry.getValue().length) {
                case 0:
                    break;
                case 1:
                    if (!entry.getKey().equals("layout")) {
                        req.setAttribute(entry.getKey(), entry.getValue()[0]);
                    }
                    break;
                default:
                    req.setAttribute(entry.getKey(), entry.getValue());
                    break;
            }
        }
    }

    private String getURL(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String[] pathElements = servletPath.split("/");
        String operation = pathElements[3];
        //get session for DB access
        MCRSession session = MCRServlet.getSession(req);
        MCRSessionMgr.setCurrentSession(session);
        MCRTransactionManager.beginTransactions();
        try {
            String url;
            String mcrId = MCRServlet.getProperty(req, "id");
            if (mcrId == null) {
                String collection = getCollection(req);
                url = MCRURLRetriever.getURLforCollection(operation, collection, false);
            } else {
                url = MCRURLRetriever.getURLforID(operation, mcrId, false);
            }
            LOGGER.info("Matched URL: {}", url);
            return url;
        } finally {
            MCRTransactionManager.commitTransactions();
            MCRSessionMgr.releaseCurrentSession();
        }
    }

    private String getCollection(HttpServletRequest req) {
        //layout is collection string
        String layout = MCRServlet.getProperty(req, "layout");
        if (layout != null) {
            return layout;
        }
        String mcrId = MCRServlet.getProperty(req, "id");
        return MCRClassificationUtils.getCollection(mcrId);
    }

}
