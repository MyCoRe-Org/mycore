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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.cli.MCRObjectCommands;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles DELETE operation on {@link MCRObject}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDeleteObjectServlet extends MCRPersistenceServlet {


    private static final long serialVersionUID = 3148314720092349972L;

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response) throws MCRAccessException,
        MCRActiveLinkException {
        MCRObjectCommands.delete(getProperty(request, "id"));
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String deletedRedirect = MCRConfiguration2.getStringOrThrow("MCR.Persistence.PageDelete");
        response.sendRedirect(response.encodeRedirectURL(MCRFrontendUtil.getBaseURL() + deletedRedirect));
    }

}
