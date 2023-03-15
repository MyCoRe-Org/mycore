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

package org.mycore.orcid2.client;

import java.net.URI;
import java.util.Locale;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.mycore.orcid2.client.exception.MCRORCIDRequestException;

/**
 * See {@link MCRORCIDUserClient}.
 * Write actions require the member API and a corresponding scope.
 */
public class MCRORCIDUserClientImpl extends MCRORCIDBaseClient implements MCRORCIDUserClient {

    private final WebTarget baseTarget;

    private final String orcid;

    /**
     * Creates an MCRORCIDUserClient with given api url and MCRORCIDCredential.
     * 
     * @param restURL rest url of ORCID api url
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     */
    public MCRORCIDUserClientImpl(String restURL, String orcid, MCRORCIDCredential credential) {
        super(restURL, credential.getAccessToken());
        this.orcid = orcid;
        this.baseTarget = getBaseTarget().path(orcid);
    }

    @Override
    public <T> T fetch(MCRORCIDSection section, Class<T> valueType, long... putCodes) throws MCRORCIDRequestException {
        return doFetch(orcid, section, valueType, putCodes);
    }

    @Override
    public long create(MCRORCIDSection section, Object object) throws MCRORCIDRequestException {
        final Response response = write(section.getPath(), HttpMethod.POST, object);
        if (!Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
            handleError(response);
        }
        return Long.parseLong(getLastPathSegment(response.getLocation()));
    }

    @Override
    public void update(MCRORCIDSection section, long putCode, Object object) throws MCRORCIDRequestException {
        final Response response = write(String.format(Locale.ROOT, "%s/%d", section.getPath(), putCode),
            HttpMethod.PUT, object);
        if (!Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
            handleError(response);
        }
    }

    @Override
    public void delete(MCRORCIDSection section, long putCode) throws MCRORCIDRequestException {
        final Response response = delete(String.format(Locale.ROOT, "%s/%d", section.getPath(), putCode));
        if (!Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
            handleError(response);
        }
    }

    private Response delete(String path) {
        return this.baseTarget.path(path).request().delete();
    }

    private Response write(String path, String method, Object object) {
        return this.baseTarget.path(path).request().build(method, Entity.entity(object, ORCID_XML_MEDIA_TYPE)).invoke();
    }

    private String getLastPathSegment(URI uri) {
        final String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
