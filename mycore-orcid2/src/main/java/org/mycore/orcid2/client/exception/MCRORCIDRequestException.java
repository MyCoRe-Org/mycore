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

package org.mycore.orcid2.client.exception;

import org.mycore.orcid2.exception.MCRORCIDException;

import jakarta.ws.rs.core.Response;

/**
 * This class is used if a request fails.
 */
public class MCRORCIDRequestException extends MCRORCIDException {

    private static final long serialVersionUID = 1L;

    /**
     * The error response.
    */
    private final Response errorResponse;

    /**
     * Creates exception with error response.
     *
     * @param errorResponse the response
     */
    public MCRORCIDRequestException(Response errorResponse) {
        super("Request failed");
        this.errorResponse = errorResponse;
    }

    /**
     * Returns the error response.
     * 
     * @return the error response
     */
    public Response getErrorResponse() {
        return errorResponse;
    }
}
