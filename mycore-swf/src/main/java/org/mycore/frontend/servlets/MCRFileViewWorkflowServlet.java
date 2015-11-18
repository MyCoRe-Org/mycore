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

// package
package org.mycore.frontend.servlets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.streams.MCRHeaderInputStream;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * This servlet read a digital object from the workflow and put it to the web. <br>
 * Call this servlet with <b>.../servlets/MCRFileViewWorkflowServlet/
 * <em>path_to_file</em> ?[base=... | type=...]</b>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date: 2009-07-28 11:32:04 +0200 (Di, 28. Jul
 *          2009) $
 * @deprecated
 */
public class MCRFileViewWorkflowServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRFileViewWorkflowServlet.class.getName());

    private static MCRSimpleWorkflowManager WFM = null;

    /** Initialization of the servlet */
    public void init() throws MCRConfigurationException, javax.servlet.ServletException {
        super.init();
        WFM = MCRSimpleWorkflowManager.instance();
    }

    /**
     * This method overrides doGetPost of MCRServlet and responds the file
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String requestPath = req.getPathInfo();
        LOGGER.debug("Request path = " + requestPath);
        if (requestPath == null) {
            String msg = "Error: HTTP request path is null";
            LOGGER.error(msg);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        if (requestPath.length() <= 1) {
            String msg = "Error: HTTP request path is empty";
            LOGGER.error(msg);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        String file = requestPath.substring(1, requestPath.length());

        String base = getProperty(job.getRequest(), "base");
        if (base != null) {
            base = base.trim();
            LOGGER.debug("Property from request : base = " + base);
        }

        String type = getProperty(job.getRequest(), "type").trim();
        if (type != null) {
            type = type.trim();
            LOGGER.debug("Property from request : type = " + type);
        }

        if (base == null && type == null) {
            String msg = "Error: HTTP request has no base or type argument";
            LOGGER.error(msg);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        boolean haspriv = MCRAccessManager.checkPermission("create-" + base);
        if (!haspriv)
            haspriv = MCRAccessManager.checkPermission("create-" + type);

        if (!haspriv) {
            LOGGER.warn("The user has no privileges!");
            return;
        }

        File dirname = null;
        if (base != null) {
            dirname = WFM.getDirectoryPath(base);
        } else {
            dirname = WFM.getDirectoryPath(type);
        }
        if (dirname == null) {
            LOGGER.warn("Can't get directory for base or type property!");
            return;
        }

        File in = new File(dirname, file);

        if (in.isFile()) {
            FileInputStream fin = new FileInputStream(in);
            MCRHeaderInputStream his = new MCRHeaderInputStream(fin);
            OutputStream out = new BufferedOutputStream(job.getResponse().getOutputStream());
            try {
                byte[] header = his.getHeader();
                String mime = MCRFileContentTypeFactory.detectType(in.getName(), header).getMimeType();
                LOGGER.debug("MimeType = " + mime);
                job.getResponse().setContentType(mime);
                job.getResponse().setContentLength((int) (in.length()));

                IOUtils.copy(his, out);
            } finally {
                out.close();
                his.close();
            }
        } else {
            LOGGER.warn("File " + in.getName() + " not found.");
        }
    }
}
