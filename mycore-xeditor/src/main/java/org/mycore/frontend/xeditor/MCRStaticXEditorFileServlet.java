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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.servlets.MCRStaticXMLFileServlet;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRStaticXEditorFileServlet extends MCRStaticXMLFileServlet {

    private static final long serialVersionUID = 1L;

    protected static final Logger LOGGER = LogManager.getLogger(MCRStaticXEditorFileServlet.class);

    /** XML document types that may contain editor forms */
    protected Set<String> docTypesIncludingEditors = new HashSet<>();

    @Override
    public void init() throws ServletException {
        super.init();
        List<String> defaults = Collections.singletonList("MyCoReWebPage");
        List<String> docTypes = MCRConfiguration.instance().getStrings("MCR.XEditor.DocTypes", defaults);
        docTypesIncludingEditors.addAll(docTypes);
    }

    protected boolean mayContainEditorForm(MCRContent content) throws IOException {
        return docTypesIncludingEditors.contains(content.getDocType());
    }

    /** For defined document types like static webpages, replace editor elements with complete editor definition */
    @Override
    protected MCRContent getResourceContent(HttpServletRequest request, HttpServletResponse response, URL resource)
        throws IOException, JDOMException, SAXException {
        MCRContent content = super.getResourceContent(request, response, resource);
        if (mayContainEditorForm(content)) {
            content = doExpandEditorElements(content, request, response,
                request.getParameter(MCREditorSessionStore.XEDITOR_SESSION_PARAM),
                request.getRequestURL().toString());
        }
        return content;
    }

    public static MCRContent doExpandEditorElements(MCRContent content, HttpServletRequest request,
        HttpServletResponse response, String sessionID, String pageURL)
        throws IOException, JDOMException, SAXException {
        MCRParameterCollector pc = new MCRParameterCollector(request, false);
        MCREditorSession session = null;

        if (sessionID != null) {
            session = MCREditorSessionStoreFactory.getSessionStore().getSession(sessionID);
            if (session == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Editor session timed out.");
                return null;
            }
        } else {
            session = new MCREditorSession(request.getParameterMap(), pc);
            session.setPageURL(pageURL);
            MCREditorSessionStoreFactory.getSessionStore().storeSession(session);
        }

        return new MCRXEditorTransformer(session, pc).transform(content);
    }
}
