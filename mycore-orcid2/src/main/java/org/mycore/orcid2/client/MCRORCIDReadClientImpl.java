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

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.mycore.orcid2.client.exception.MCRORCIDRequestException;

/**
 * Provides an ORCID Client with read methods.
 * Can be used to talk to Public or Member API.
 */
public class MCRORCIDReadClientImpl extends MCRORCIDBaseClient implements MCRORCIDReadClient {

    private static final int DEFAULT_SEARCH_LIMIT = 1000;

    /**
     * Creates a new Client with given API url.
     * Can be used to read ORCID Public API.
     * 
     * @param restURL url of ORCID API
     */
    public MCRORCIDReadClientImpl(String restURL) {
        super(restURL, null);
    }

    /**
     * Creates a new Client with given API url and token.
     * 
     * @param restURL url of ORCID API
     * @param token the access token
     */
    public MCRORCIDReadClientImpl(String restURL, String token) {
        super(restURL, token);
    }

    @Override
    public <T> T fetch(String orcid, MCRORCIDSection section, Class<T> valueType, long... putCodes)
        throws MCRORCIDRequestException {
        return doFetch(orcid, section, valueType, putCodes);
    }

    @Override
    public <T> T search(MCRORCIDSearch type, String query, int offset, int limit, Class<T> valueType)
        throws MCRORCIDRequestException {
        return doSearch(type.getPath(), query, offset, limit, valueType);
    }

    @Override
    public <T> T search(MCRORCIDSearch type, String query, Class<T> valueType) throws MCRORCIDRequestException {
        return search(type, query, 0, DEFAULT_SEARCH_LIMIT, valueType);
    }

    private <T> T doSearch(String path, String query, int offset, int limit, Class<T> valueType)
        throws MCRORCIDRequestException {
        if (offset < 0 || limit < 0) {
            throw new IllegalArgumentException("Offset or limit must be positive.");
        }
        final Map<String, Object> queryMap = new HashMap<String, Object>();
        queryMap.put("q", query);
        queryMap.put("start", offset);
        queryMap.put("rows", limit);
        final Response response = fetch(path + "/", queryMap);
        if (!Objects.equals(response.getStatusInfo().getFamily(), Response.Status.Family.SUCCESSFUL)) {
            throw new MCRORCIDRequestException(response);
        }
        return response.readEntity(valueType);
    }
}
