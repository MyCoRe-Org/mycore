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

import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.support.MCRObjectIDLockTable;
import org.xml.sax.SAXParseException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles UPDATE operation on {@link MCRObject}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUpdateObjectServlet extends MCRPersistenceServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        ServletException, SAXParseException, JDOMException, IOException {
        MCRObjectID objectID = updateObject(MCRPersistenceHelper.getEditorSubmission(request, true));
        request.setAttribute(OBJECT_ID_KEY, objectID);
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MCRObjectID returnID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        if (returnID == null) {
            throw new ServletException("No MCRObjectID given.");
        } else {
            response.sendRedirect(
                response.encodeRedirectURL(buildRedirectURL(
                    MCRFrontendUtil.getBaseURL() + "receive/" + returnID,
                    MCRPersistenceHelper.getXSLProperties(request))));
        }
    }

    /**
     * Updates a mycore object in the persistence backend.
     * @param doc
     *  MyCoRe object as XML
     * @return
     *  MCRObjectID of the newly created object.
     * @throws JDOMException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     * @throws IOException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     */
    private MCRObjectID updateObject(Document doc) throws JDOMException, IOException,
        MCRException, MCRAccessException {
        MCRObject mcrObject = MCRPersistenceHelper.getMCRObject(doc);
        LogManager.getLogger().info("ID: {}", mcrObject::getId);
        try {
            MCRMetadataManager.update(mcrObject);
            return mcrObject.getId();
        } finally {
            MCRObjectIDLockTable.unlock(mcrObject.getId());
        }
    }

}
