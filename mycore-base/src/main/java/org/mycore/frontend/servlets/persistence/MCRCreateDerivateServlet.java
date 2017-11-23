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

package org.mycore.frontend.servlets.persistence;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * Handles CREATE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCreateDerivateServlet extends MCRPersistenceServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 4735574336094275787L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
        throws MCRAccessException {
        String objectID = getObjectId(request);
        if (!MCRAccessManager.checkPermission(objectID, PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Add derivate.", objectID, PERMISSION_WRITE);
        }
    }

    /**
     * redirects to new derivate upload form.
     *
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>object ID of the parent mycore object (required)</dd>
     * </dl>
     */
    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        redirectToUploadForm(getServletContext(), request, response, getObjectId(request), null);
    }

    private String getObjectId(HttpServletRequest request) {
        return getProperty(request, "id");
    }

}
