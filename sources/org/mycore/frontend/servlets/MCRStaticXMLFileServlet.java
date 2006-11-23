/*
 * $RCSfile$
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

package org.mycore.frontend.servlets;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.frontend.editor.MCREditorServlet;

/**
 * This servlet displays static *.xml files stored in the web application by
 * sending them to MCRLayoutServlet.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRStaticXMLFileServlet extends MCRServlet {
    protected final static Logger LOGGER = Logger.getLogger(MCRStaticXMLFileServlet.class);

    public void doGetPost(MCRServletJob job) throws ServletException, java.io.IOException {
        String requestedPath = job.getRequest().getServletPath();
        LOGGER.info("MCRStaticXMLFileServlet " + requestedPath);

        String path = getServletContext().getRealPath(requestedPath);
        File file = new File(path);
        if (!file.exists()) {
            String msg = "Could not find file " + requestedPath;
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, msg);

            return;
        }

        job.getRequest().setAttribute("XSL.StaticFilePath", requestedPath.substring(1));
        job.getRequest().setAttribute("XSL.DocumentBaseURL", file.getParent() + File.separator);
        job.getRequest().setAttribute("XSL.FileName", file.getName());
        job.getRequest().setAttribute("XSL.FilePath", file.getPath());

        // Find out XML document type: Is this a static webpage or some other XML?
        FileInputStream fis = new FileInputStream(file);
        String type = MCRUtils.parseDocumentType(fis);
        fis.close();

        // For static webpages, replace editor elements with complete editor definition
        if ("MyCoReWebPage".equals(type) || "webpage".equals(type)) {
            MCRURIResolver.init(getServletContext(), getBaseURL());
            Document xml = MCRXMLHelper.parseURI(path, false);
            MCREditorServlet.replaceEditorElements(job, "file://" + path, xml);
            job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", xml);
        } else
            job.getRequest().setAttribute("MCRLayoutServlet.Input.FILE", file);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
