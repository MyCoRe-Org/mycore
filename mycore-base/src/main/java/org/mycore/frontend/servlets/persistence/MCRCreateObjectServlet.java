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

package org.mycore.frontend.servlets.persistence;

import java.io.IOException;
import java.io.Serial;
import java.util.Enumeration;
import java.util.Properties;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.xml.sax.SAXParseException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles CREATE operation on {@link MCRObject}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCreateObjectServlet extends MCRPersistenceServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean appendDerivate;

    @Override
    public void init() throws ServletException {
        super.init();
        String appendDerivate = getInitParameter("appendDerivate");
        if (appendDerivate != null) {
            this.appendDerivate = true;
        }
    }

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
        throws MCRAccessException, ServletException, SAXParseException, JDOMException,
        IOException {
        Document editorSubmission = MCRPersistenceHelper.getEditorSubmission(request, false);
        MCRObjectID objectID;
        if (editorSubmission != null) {
            objectID = createObject(editorSubmission);
        } else {
            //editorSubmission is null, when editor input is absent (redirect to editor form in render phase)
            String projectID = getProperty(request, "project");
            String type = getProperty(request, "type");
            String formattedId = MCRObjectID.formatID(projectID + "_" + type, 0);
            objectID = MCRObjectID.getInstance(formattedId);
            MCRMetadataManager.checkCreatePrivilege(objectID);
        }
        request.setAttribute(OBJECT_ID_KEY, objectID);
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //return to object itself if created, else call editor form
        MCRObjectID returnID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        if (returnID.getNumberAsInteger() == 0) {
            redirectToCreateObject(request, response);
        } else {
            Properties params = MCRPersistenceHelper.getXSLProperties(request);
            if (this.appendDerivate) {
                params.put("id", returnID.toString());
                params.put("cancelURL", MCRFrontendUtil.getBaseURL() + "receive/" + returnID);
                response.sendRedirect(
                    response.encodeRedirectURL(
                        buildRedirectURL(MCRFrontendUtil.getBaseURL() + "servlets/derivate/create", params)));
            } else {
                response.sendRedirect(response
                    .encodeRedirectURL(
                        buildRedirectURL(MCRFrontendUtil.getBaseURL() + "receive/" + returnID, params)));
            }
        }
    }

    /**
     * Adds a new mycore object to the persistence backend.
     * @param doc
     *  MyCoRe object as XML
     * @return
     *  MCRObjectID of the newly created object.
     * @throws JDOMException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     * @throws IOException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     */
    private MCRObjectID createObject(Document doc)
        throws JDOMException, IOException, MCRException, MCRAccessException {
        MCRObject mcrObject = MCRPersistenceHelper.getMCRObject(doc);
        MCRMetadataManager.create(mcrObject);
        return mcrObject.getId();
    }

    /**
     * redirects to new mcrobject form.
     * <p>
     * At least "type" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>type</dt>
     *   <dd>object type of the new object (required)</dd>
     *   <dt>project</dt>
     *   <dd>project ID part of object ID (required)</dd>
     *   <dt>layout</dt>
     *   <dd>special editor form layout</dd>
     * </dl>
     */
    private void redirectToCreateObject(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        MCRObjectID objectID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        StringBuilder sb = new StringBuilder();
        sb.append("editor_form_author").append('-').append(objectID.getTypeId());
        String layout = getProperty(request, "layout");
        if (layout != null && layout.length() != 0) {
            sb.append('-').append(layout);
        }
        String baseName = sb.toString();
        String form = MCRPersistenceHelper.getWebPage(baseName + ".xed", baseName + ".xml");
        Properties params = new Properties();
        params.put("cancelUrl", MCRPersistenceHelper.getCancelUrl(request));
        params.put("mcrid", objectID.toString());
        Enumeration<String> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        response
            .sendRedirect(response.encodeRedirectURL(buildRedirectURL(MCRFrontendUtil.getBaseURL() + form, params)));
    }

}
