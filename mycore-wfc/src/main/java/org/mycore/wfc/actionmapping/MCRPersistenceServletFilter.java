/*
 * $Id$
 * $Revision: 5697 $ $Date: 18.04.2012 $
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

package org.mycore.wfc.actionmapping;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRPersistenceServletFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger(MCRPersistenceServletFilter.class);

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
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
        String type = pathElements[2];
        String operation = pathElements[3];
        //get session for DB access
        MCRSession session = MCRServlet.getSession(req);
        MCRSessionMgr.setCurrentSession(session);
        session.beginTransaction();
        try {
            String url;
            String mcrId = MCRServlet.getProperty(req, "id");
            if (mcrId == null) {
                String collection = getCollection(req);
                url = MCRURLRetriever.getURLforCollection(operation, collection, false);
            } else {
                url = MCRURLRetriever.getURLforID(operation, mcrId, false);
            }
            LOGGER.info("Matched URL: " + url);
            return url;
        } finally {
            session.commitTransaction();
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

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
    }

}
