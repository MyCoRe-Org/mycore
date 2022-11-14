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

import org.mycore.orcid2.client.exception.MCRORCIDRequestException;

/**
 * Provides an ORCID Client with read methods.
 * Can be used to talk to Public or Member API.
 */
public class MCRORCIDReadClientImpl extends MCRORCIDAPIClientImpl implements MCRORCIDReadClient {

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

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T fetch(String orcid, MCRORCIDSection section, Class<T> valueType, long... putCodes)
        throws MCRORCIDRequestException {
        return doFetch(orcid, section, valueType, putCodes);
    }
}
