/**
 * 
 */
package org.mycore.frontend.servlets.persistence;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.support.MCRObjectIDLockTable;
import org.xml.sax.SAXParseException;

/**
 * Handles UPDATE operation on {@link MCRObject}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUpdateObjectServlet extends MCRPersistenceServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -7507356414480350102L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        ServletException, MCRActiveLinkException, SAXParseException, JDOMException, IOException {
        MCRObjectID objectID = updateObject(MCRPersistenceHelper.getEditorSubmission(request, true));
        request.setAttribute(OBJECT_ID_KEY, objectID);
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MCRObjectID returnID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        if (returnID == null) {
            throw new ServletException("No MCRObjectID given.");
        } else
            response.sendRedirect(
                response.encodeRedirectURL(buildRedirectURL(
                    MCRFrontendUtil.getBaseURL() + "receive/" + returnID.toString(),
                    MCRPersistenceHelper.getXSLProperties(request))));
    }

    /**
     * Updates a mycore object in the persistence backend.
     * @param doc
     *  MyCoRe object as XML
     * @return
     *  MCRObjectID of the newly created object.
     * @throws MCRActiveLinkException
     *  If links from or to other objects will fail.
     * @throws JDOMException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     * @throws IOException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     * @throws SAXParseException
     * @throws MCRException
     * @throws MCRAccessException 
     */
    private MCRObjectID updateObject(Document doc) throws MCRActiveLinkException, JDOMException, IOException,
        MCRException, SAXParseException, MCRAccessException {
        MCRObject mcrObject = MCRPersistenceHelper.getMCRObject(doc);
        LogManager.getLogger().info("ID: " + mcrObject.getId());
        try {
            MCRMetadataManager.update(mcrObject);
            return mcrObject.getId();
        } finally {
            MCRObjectIDLockTable.unlock(mcrObject.getId());
        }
    }

}
