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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;

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
     *   trustedOrcid: null,
     * }
     * @return the user status
     */
    @GET
    @Path("userStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public MCRORCIDUserStatus getUserStatusSummary() throws WebApplicationException {
        if (isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRORCIDUser user = MCRORCIDSessionUtils.getCurrentUser();
        final String[] orcids = user.getORCIDs().toArray(String[]::new);
        final MCRORCIDCredentials credentials = user.getCredentials();
        if (credentials == null) {
            return new MCRORCIDUserStatus(orcids, null);
        }
        return new MCRORCIDUserStatus(orcids, credentials.getORCID());
    }

    private boolean isCurrentUserGuest() {
        return MCRSystemUserInformation.getGuestInstance().getUserID()
            .equals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
    }

    static class MCRORCIDUserStatus {

        private String[] orcids;

        private String trustedOrcid;

        MCRORCIDUserStatus(String[] orcids, String trustedOrcid) {
            this.orcids = orcids;
            this.trustedOrcid = trustedOrcid;
        }

        public String[] getOrcids() {
            return orcids;
        }

        public String getTrustedOrcid() {
            return trustedOrcid;
        }
    }
}
