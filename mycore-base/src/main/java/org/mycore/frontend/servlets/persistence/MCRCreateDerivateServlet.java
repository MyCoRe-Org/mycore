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

import java.io.IOException;
import java.io.Serial;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRMissingPermissionException;
import org.mycore.datamodel.metadata.MCRDerivate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles CREATE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCreateDerivateServlet extends MCRPersistenceServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
        throws MCRAccessException {
        String objectID = getObjectId(request);
        if (!MCRAccessManager.checkPermission(objectID, PERMISSION_WRITE)) {
            throw new MCRMissingPermissionException("Add derivate.", objectID, PERMISSION_WRITE);
        }
    }

    /**
     * redirects to new derivate upload form.
     * <p>
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>object ID of the parent mycore object (required)</dd>
     * </dl>
     */
    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirectToUploadForm(getServletContext(), request, response, getObjectId(request), null);
    }

    private String getObjectId(HttpServletRequest request) {
        return getProperty(request, "id");
    }

}
