/**
 * 
 */
package org.mycore.frontend.servlets.persistence;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.frontend.MCRFrontendUtil;
import org.xml.sax.SAXParseException;

/**
 * Handles UPDATE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUpdateDerivateServlet extends MCRPersistenceServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -8005685456830013040L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        ServletException, MCRActiveLinkException, SAXParseException, JDOMException, IOException {
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
                throw MCRAccessException.missingPermission("Change derivate title.", derivateID.toString(),
                    PERMISSION_WRITE);
            }
            if (objectID != null) {
                //Load additional files
                MCRObjectID mcrObjectID = MCRObjectID.getInstance(objectID);
                request.setAttribute("objectid", mcrObjectID.toString());
                if (!MCRAccessManager.checkPermission(mcrObjectID, PERMISSION_WRITE)) {
                    throw MCRAccessException.missingPermission("Change derivate title.", mcrObjectID.toString(),
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
            return;
        } else
            response.sendRedirect(
                response.encodeRedirectURL(
                    MCRFrontendUtil.getBaseURL() + "receive/" + returnID.toString()));
    }

    /**
     * Updates derivate xml in the persistence backend
     * @param editorSubmission
     *  MyCoRe derivate as XML
     * @return
     *  MCRObjectID of the MyCoRe object
     * @throws SAXParseException
     * @throws MCRAccessException 
     */
    private MCRObjectID updateDerivateXML(Document editorSubmission)
        throws SAXParseException, IOException, MCRAccessException {
        MCRObjectID objectID;
        Element root = editorSubmission.getRootElement();
        root.setAttribute("noNamespaceSchemaLocation", "datamodel-derivate.xsd", XSI_NAMESPACE);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        byte[] xml = new MCRJDOMContent(editorSubmission).asByteArray();
        MCRDerivate der = new MCRDerivate(xml, true);
        MCRObjectID derivateID = der.getId();
        // store entry of derivate xlink:title in object
        objectID = der.getDerivate().getMetaLink().getXLinkHrefID();
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectID);
        MCRObjectStructure structure = obj.getStructure();
        MCRMetaLinkID linkID = structure.getDerivateLink(derivateID);
        linkID.setXLinkTitle(der.getLabel());
        try {
            MCRMetadataManager.update(obj);
            MCRMetadataManager.update(der);
        } catch (MCRPersistenceException | MCRActiveLinkException e) {
            throw new MCRPersistenceException("Can't store label of derivate " + derivateID
                + " in derivate list of object " + objectID + ".", e);
        }
        return objectID;
    }

    /**
     * redirects to either add files to derivate upload form or change derivate title form.
     *
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>derivate ID(required)</dd>
     *   <dt>objectid</dt>
     *   <dd>object ID of the parent mycore object</dd>
     * </dl>
     * If the "objectid" parameter is given, upload form is presented.
     * If not than the user is redirected to the title change form.
     * @param job
     * @throws IOException
     * @throws ServletException 
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
            String page = MCRPersistenceHelper.getWebPage(getServletContext(), "editor_form_derivate.xed",
                "editor_form_derivate.xml");
            String redirectURL = MCRFrontendUtil.getBaseURL() + page;
            response
                .sendRedirect(response.encodeRedirectURL(buildRedirectURL(redirectURL, params)));
        }
    }

}
