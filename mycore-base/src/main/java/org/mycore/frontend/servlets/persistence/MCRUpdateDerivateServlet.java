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

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.io.Serial;
import java.util.Properties;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRMissingPermissionException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles UPDATE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUpdateDerivateServlet extends MCRPersistenceServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        ServletException, JDOMException, IOException {
        Document editorSubmission = MCRPersistenceHelper.getEditorSubmission(request, false);
        if (editorSubmission != null) {
            MCRObjectID objectID = updateDerivateXML(editorSubmission);
            request.setAttribute(OBJECT_ID_KEY, objectID);
        } else {
            //access checks
            String objectID = getProperty(request, "objectid");
            MCRObjectID derivateID = MCRObjectID.getInstance(getProperty(request, "id"));
            request.setAttribute("id", derivateID.toString());
            if (!MCRAccessManager.checkPermission(derivateID, PERMISSION_WRITE)) {
                throw new MCRMissingPermissionException("Change derivate title.", derivateID.toString(),
                    PERMISSION_WRITE);
            }
            if (objectID != null) {
                //Load additional files
                MCRObjectID mcrObjectID = MCRObjectID.getInstance(objectID);
                request.setAttribute("objectid", mcrObjectID.toString());
                if (!MCRAccessManager.checkPermission(mcrObjectID, PERMISSION_WRITE)) {
                    throw new MCRMissingPermissionException("Change derivate title.", mcrObjectID.toString(),
                        PERMISSION_WRITE);
                }
            }
        }
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MCRObjectID returnID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        if (returnID == null) {
            //calculate redirect to title change form or add files form
            redirectToUpdateDerivate(request, response);
        } else {
            response.sendRedirect(
                response.encodeRedirectURL(MCRFrontendUtil.getBaseURL() + "receive/" + returnID));
        }
    }

    /**
     * Updates derivate xml in the persistence backend
     * @param editorSubmission
     *  MyCoRe derivate as XML
     * @return
     *  MCRObjectID of the MyCoRe object
     */
    private MCRObjectID updateDerivateXML(Document editorSubmission)
        throws JDOMException, IOException, MCRAccessException {
        Element root = editorSubmission.getRootElement();
        root.setAttribute("noNamespaceSchemaLocation", "datamodel-derivate.xsd", XSI_NAMESPACE);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        byte[] xml = new MCRJDOMContent(editorSubmission).asByteArray();
        MCRDerivate der = new MCRDerivate(xml, true);
        MCRMetadataManager.update(der);
        return der.getOwnerID();
    }

    /**
     * Redirects to either add files to derivate upload form or change derivate title form.
     * <p>
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>derivate ID(required)</dd>
     *   <dt>objectid</dt>
     *   <dd>object ID of the parent mycore object</dd>
     * </dl>
     * If the "objectid" parameter is given, upload form is presented.
     * If not than the user is redirected to the title change form.
     *
     */
    private void redirectToUpdateDerivate(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        String objectID = getProperty(request, "objectid");
        String derivateID = getProperty(request, "id");
        if (objectID != null) {
            //Load additional files
            redirectToUploadForm(getServletContext(), request, response, objectID, derivateID);
        } else {
            //set derivate title
            Properties params = new Properties();
            params.put("sourceUri", "xslStyle:mycorederivate-editor:mcrobject:" + derivateID);
            params.put("cancelUrl", MCRPersistenceHelper.getCancelUrl(request));
            String page = MCRPersistenceHelper.getWebPage("editor_form_derivate.xed",
                "editor_form_derivate.xml");
            String redirectURL = MCRFrontendUtil.getBaseURL() + page;
            response
                .sendRedirect(response.encodeRedirectURL(buildRedirectURL(redirectURL, params)));
        }
    }

}
