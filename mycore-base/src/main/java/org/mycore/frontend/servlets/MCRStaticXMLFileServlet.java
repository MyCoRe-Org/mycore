/*
 * $Revision$ 
 * $Date$
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.frontend.editor.MCREditorServlet;
import org.xml.sax.SAXException;

/**
 * This servlet displays static *.xml files stored in the web application by
 * sending them to MCRLayoutService.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRStaticXMLFileServlet extends MCRServlet {

    private static final long serialVersionUID = -9213353868244605750L;

    protected final static Logger LOGGER = Logger.getLogger(MCRStaticXMLFileServlet.class);

    /** XML document types that may contain editor forms */
    protected Set<String> docTypesIncludingEditors = new HashSet<String>();

    @Override
    public void init() throws ServletException {
        super.init();

        String docTypes = MCRConfiguration.instance().getString("MCR.EditorFramework.DocTypes", "MyCoReWebPage");
        for (String docType : docTypes.split(","))
            docTypesIncludingEditors.add(docType);
    }

    @Override
    public void doGetPost(MCRServletJob job) throws java.io.IOException, MCRException, SAXException, JDOMException,
        URISyntaxException, TransformerException {
        URL resource = resolveResource(job);
        if (resource != null) {
            HttpServletRequest request = job.getRequest();
            HttpServletResponse response = job.getResponse();
            setXSLParameters(resource, request);
            MCRContent content = expandEditorElements(request, resource);
            getLayoutService().doLayout(request, response, content);
        }
    }

    private void setXSLParameters(URL resource, HttpServletRequest request) throws MalformedURLException,
        URISyntaxException {
        String path = resource.getProtocol().equals("file") ? resource.getPath() : resource.toExternalForm();
        int lastPathElement = path.lastIndexOf('/') + 1;
        String fileName = path.substring(lastPathElement);
        String parent = path.substring(0, lastPathElement);
        request.setAttribute("XSL.StaticFilePath", request.getServletPath().substring(1));
        request.setAttribute("XSL.DocumentBaseURL", parent.toString());
        request.setAttribute("XSL.FileName", fileName);
        request.setAttribute("XSL.FilePath", path);
    }

    private URL resolveResource(MCRServletJob job) throws IOException {
        String requestedPath = job.getRequest().getServletPath();
        LOGGER.info("MCRStaticXMLFileServlet " + requestedPath);

        URL resource = getServletContext().getResource(requestedPath);
        if (resource != null) {
            return resource;
        }

        String msg = "Could not find file " + requestedPath;
        job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, msg);
        return null;
    }

    /** For defined document types like static webpages, replace editor elements with complete editor definition */
    protected MCRContent expandEditorElements(HttpServletRequest request, URL resource) throws IOException,
        JDOMException, SAXException, MalformedURLException {
        MCRContent content = new MCRURLContent(resource);
        if (mayContainEditorForm(content)) {
            Document xml = content.asXML();
            MCREditorServlet.replaceEditorElements(request, resource.toString(), xml);
            MCRJDOMContent jdomContent = new MCRJDOMContent(xml);
            jdomContent.setFormat(Format.getRawFormat());
            return jdomContent;
        }
        return content;
    }

    protected boolean mayContainEditorForm(MCRContent content) throws IOException {
        return docTypesIncludingEditors.contains(content.getDocType());
    }
}
