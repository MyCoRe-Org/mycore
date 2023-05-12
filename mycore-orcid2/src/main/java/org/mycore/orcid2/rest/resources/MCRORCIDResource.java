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

package org.mycore.orcid2.rest.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.restapi.annotations.MCRRequireTransaction;

/**
 * Base resource for orcid methods.
 */
@Path("v1")
public class MCRORCIDResource {

    /**
     * Returns the ORCID status of the current user as JSON, e.g.
     *
     * {
     *   orcids: ['0000-0001-5484-889X'],
     *   trustedOrcids: ['0000-0001-5484-889X'],
     * }
     * @return the user status
     * @throws WebApplicationException if user is guest
     */
    @GET
    @Path("user-status")
    @Produces(MediaType.APPLICATION_JSON)
    public MCRORCIDUserStatus getUserStatus() {
        if (MCRORCIDUtils.isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRORCIDUser user = MCRORCIDSessionUtils.getCurrentUser();
        final String[] orcids = user.getORCIDs().toArray(String[]::new);
        final String[] credentials = user.getCredentials().entrySet().stream().map(Map.Entry::getKey)
            .toArray(String[]::new);
        return new MCRORCIDUserStatus(orcids, credentials);
    }

    /**
     * Revokes ORCID iD for current user.
     * 
     * @param orcid the ORCID iD
     * @param redirectString optional redirect URI as String
     * @return Response
     * @throws WebApplicationException if orcid is null, user is guest or revoke fails
     */
    @POST
    @Path("revoke/{orcid}")
    @MCRRequireTransaction
    public Response revoke(@PathParam("orcid") String orcid, @QueryParam("redirect_uri") String redirectString) {
        if (orcid == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (MCRORCIDUtils.isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        URI redirectURI = null;
        if (redirectString != null) {
            try {
                redirectURI = new URI(redirectString);
            } catch (URISyntaxException e) {
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
        }
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        try {
            MCRORCIDUserUtils.revokeCredentialByORCID(orcidUser, orcid);
        } catch (MCRORCIDException e) {
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }
        if (redirectURI != null) {
            return Response.seeOther(redirectURI).build();
        }
        return Response.ok().build();
    }

    static class MCRORCIDUserStatus {

        private String[] orcids;

        private String[] trustedOrcids;

        MCRORCIDUserStatus(String[] orcids, String[] trustedOrcids) {
            this.orcids = orcids;
            this.trustedOrcids = trustedOrcids;
        }

        public String[] getOrcids() {
            return orcids;
        }

        public String[] getTrustedOrcids() {
            return trustedOrcids;
        }
    }
}
