/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid.oauth;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.common.config.MCRConfigurationException;

import com.fasterxml.jackson.core.JsonProcessingException;

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
     * @throws MCRConfiguratonException if request fails, e.g. because of misconfigured client ID and secret
     */
    public MCRTokenResponse post() throws MCRConfigurationException, JsonProcessingException, IOException {
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
