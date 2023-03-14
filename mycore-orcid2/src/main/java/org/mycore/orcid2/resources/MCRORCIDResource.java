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

package org.mycore.orcid2.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.orcid2.user.MCRORCIDUserCredential;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;

/**
 * Base resource for orcid methods.
 */
@Path("orcid")
public class MCRORCIDResource {

    /**
     * Returns the ORCID status of the current user as JSON, e.g.
     *
     * {
     *   orcids: ['0000-0001-5484-889X'],
     *   trustedOrcids: ['0000-0001-5484-889X'],
     * }
     * @return the user status
     */
    @GET
    @Path("userStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public MCRORCIDUserStatus getUserStatus() throws WebApplicationException {
        if (isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRORCIDUser user = MCRORCIDSessionUtils.getCurrentUser();
        final String[] orcids = user.getORCIDs().toArray(String[]::new);
        final String[] credentials =
            user.listCredentials().stream().map(MCRORCIDUserCredential::getORCID).toArray(String[]::new);
        return new MCRORCIDUserStatus(orcids, credentials);
    }

    /**
     * Revokes ORCID iD for current user.
     * 
     * @param orcid the ORCID iD
     * @return Response
     */
    @GET
    @Path("revoke")
    public Response revoke(@PathParam("orcid") String orcid) throws WebApplicationException {
        if (orcid == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        try {
            MCRORCIDUserUtils.revokeCredentialsByORCID(orcidUser, orcid);
            return Response.ok().build();
        } catch (Exception e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    private boolean isCurrentUserGuest() {
        return MCRSystemUserInformation.getGuestInstance().getUserID()
            .equals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
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
