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

package org.mycore.frontend.servlets;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSessionMgr;

/**
 * This servlet displays static *.xml files stored in the web application by
 * sending them to MCRLayoutServlet.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRStaticXMLFileServlet extends MCRServlet {
    protected final static Logger LOGGER = Logger
            .getLogger(MCRStaticXMLFileServlet.class);

    private Random random = new Random();

    public void doGetPost(MCRServletJob job) throws ServletException,
            java.io.IOException {
        String requestedPath = job.getRequest().getServletPath();
        LOGGER.info("MCRStaticXMLFileServlet " + requestedPath);
        URL url = null;

        try {
            url = getServletContext().getResource(requestedPath);
        } catch (MalformedURLException willNeverBeThrown) {
        }

        if (url == null) {
            String msg = "Could not find file " + requestedPath;
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return;
        }

        String path = getServletContext().getRealPath(requestedPath);
        File file = new File(path);
        String documentBaseURL = file.getParent() + File.separator;

        // Store http request parameters into session, for later use
        Map map = new HashMap();
        map.putAll(job.getRequest().getParameterMap());
        String key = buildRequestParamKey();
        MCRServlet.requestParamCache.put(key, map);

        job.getRequest().setAttribute("XSL.RequestParamKey", key);
        job.getRequest().setAttribute("XSL.StaticFilePath",
                requestedPath.substring(1));
        job.getRequest().setAttribute("XSL.DocumentBaseURL", documentBaseURL);
        job.getRequest().setAttribute("XSL.FileName", file.getName());
        job.getRequest().setAttribute("XSL.FilePath", file.getPath());
        job.getRequest().setAttribute("MCRLayoutServlet.Input.FILE", file);

        // Set XSL Style to current language if no XSL.Style present in request:
        if ((job.getRequest().getParameter("XSL.Style") == null)
                || (job.getRequest().getParameter("XSL.Style").length() == 0))
            job.getRequest().setAttribute("XSL.Style",
                    MCRSessionMgr.getCurrentSession().getCurrentLanguage());

        RequestDispatcher rd = getServletContext().getNamedDispatcher(
                "MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }

    /** Helper method to build a unique key for caching http request params * */
    private synchronized String buildRequestParamKey() {
        StringBuffer sb = new StringBuffer();
        sb.append(Long.toString(System.currentTimeMillis(), 36));
        sb.append(Long.toString(random.nextLong(), 36));
        sb.reverse();
        return sb.toString();
    }
}