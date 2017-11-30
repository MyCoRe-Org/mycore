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

package org.mycore.orcid.oauth;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.common.config.MCRConfigurationException;

/**
 * Represents a token request against the OAuth2 API of orcid.org.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRTokenRequest {

    private Form form = new Form();

    private WebTarget baseTarget;

    MCRTokenRequest(WebTarget baseTarget) {
        this.baseTarget = baseTarget;
    }

    void set(String name, String value) {
        form.param(name, value);
    }

    /**
     * Posts the request and returns the response.
     *
     * @throws MCRConfigurationException if request fails, e.g. because of misconfigured client ID and secret
     */
    public MCRTokenResponse post() throws MCRConfigurationException, IOException {
        Entity<Form> formEntity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        WebTarget target = baseTarget.path("token");
        Builder b = target.request().accept(MediaType.APPLICATION_JSON);
        Response r = b.post(formEntity);

        MCRTokenResponse response = new MCRTokenResponse(r);
        if (!response.wasSuccessful()) {
            throw new MCRConfigurationException(response.getStatusMessage());
        }
        return response;
    }
}
