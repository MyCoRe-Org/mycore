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

import java.io.IOException;

import org.mycore.access.MCRAccessException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.frontend.cli.MCRDerivateCommands;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles DELTE operation on {@link MCRDerivate}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDeleteDerivateServlet extends MCRPersistenceServlet {


    private static final long serialVersionUID = 1581063299429224344L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
        throws MCRAccessException {
        MCRDerivateCommands.delete(getProperty(request, "id"));
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(response.encodeRedirectURL(getReferer(request).toString()));
    }

}
