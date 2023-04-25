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

package org.mycore.orcid2.v3;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ProcessingException;

import org.mycore.orcid2.client.MCRORCIDClientErrorHandler;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.orcid.jaxb.model.v3.release.error.OrcidError;

/**
 * See {@link org.mycore.orcid2.client.MCRORCIDClientErrorHandler}.
 */
public class MCRORCIDClientErrorHandlerImpl implements MCRORCIDClientErrorHandler {

    @Override
    public void handleErrorResponse(Response response) {
        if (response.hasEntity()) {
            response.bufferEntity();
            try {
                final OrcidError error = response.readEntity(OrcidError.class);
                throw new MCRORCIDRequestException(error.getDeveloperMessage(), response);
            } catch (ProcessingException e) {
                try {
                    final String error = response.readEntity(String.class);
                    throw new MCRORCIDRequestException(error, response);
                } catch (ProcessingException f) {
                    // ignore
                }
            }
        }
        throw new MCRORCIDRequestException("Unknown error", response);
    }
}
