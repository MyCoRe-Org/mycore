/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserProperties;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.user2.MCRUserManager;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A resource class for managing ORCID-related operations.
 */
@Path("orcid")
public class MCRORCIDResource {

    /**
     * Retrieves the user status for the currently logged-in ORCID user.
     * <p>
     * This method returns a {@link MCRORCIDUserStatus} object that contains the
     * list of ORCIDs associated with the user and a list of trusted ORCIDs.
     * The trusted ORCIDs are derived from the user's credentials.
     *
     * <p>Example JSON response:</p>
     * <pre>
     * {
     *   "orcids": ["0000-0001-5484-889X"],
     *   "trustedOrcids": ["0000-0001-5484-889X"],
     * }
     * </pre>
     *
     * @return an {@link MCRORCIDUserStatus} object containing the user's ORCID iDs and trusted ORCID iDs.
     */
    @GET
    @Path("v1/user-status")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public MCRORCIDUserStatus getUserStatus() {
        final MCRORCIDUser user = MCRORCIDSessionUtils.getCurrentUser();
        final List<String> trustedOrcids = user.getCredentials().entrySet().stream().map(Map.Entry::getKey).toList();
        return new MCRORCIDUserStatus(new ArrayList<>(user.getORCIDs()), trustedOrcids);
    }

    /**
     * Retrieves the user properties associated with a specific ORCID iD for the current user.
     *
     * <p>This method fetches the properties of the user associated with the given ORCID iD for the
     * currently logged-in user. If the ORCID iD is null, a {@link BadRequestException} is thrown.
     *
     * <p>Example of the returned {@link MCRORCIDUserProperties} object in JSON format:
     * <pre>
     * {
     *   "isAlwaysUpdateWork": true,
     *   "isCreateDuplicateWork": true,
     *   "isCreateFirstWork": false,
     *   "isRecreateDeletedWork": null,
     * }
     * </pre>
     *
     * @param orcid the ORCID identifier for which to retrieve user properties
     * @return an object containing the user properties associated with the specified ORCID iD
     * @throws ForbiddenException if the currently logged-in user does not have access to the specified ORCID iD
     */
    @GET
    @Path("v1/user-properties/{" + MCRORCIDRestConstants.PATH_PARAM_ORCID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public MCRORCIDUserProperties setUserProperties(@PathParam(MCRORCIDRestConstants.PATH_PARAM_ORCID) String orcid) {
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        if (!orcidUser.getORCIDs().contains(orcid)) {
            throw new ForbiddenException();
        }
        return orcidUser.getUserPropertiesByORCID(orcid);
    }

    /**
     * Updates the user properties associated with a specific ORCID iD for the current user.
     *
     * <p>This method updates the properties for the user associated with the given ORCID iD. It sets
     * the properties provided in the {@link MCRORCIDUserProperties} object for the currently logged-in
     * user. If the ORCID iD is `null`, a {@link BadRequestException} is thrown. If an error occurs during
     * the update operation, a {@link BadRequestException} is thrown with the underlying exception as the cause.
     *
     * @param orcid the ORCID iD for which to update user properties
     * @param userProperties an object containing the new user properties to be set for the specified ORCID iD
     * @return a {@link Response} indicating a successful operation (HTTP 200 OK)
     *
     * @throws BadRequestException if an error occurs during the update
     * @throws ForbiddenException if the currently logged-in user does not have access to update properties
     */
    @PUT
    @Path("v1/user-properties/{" + MCRORCIDRestConstants.PATH_PARAM_ORCID + "}")
    @MCRRequireTransaction
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public Response setUserProperties(@PathParam(MCRORCIDRestConstants.PATH_PARAM_ORCID) String orcid,
        MCRORCIDUserProperties userProperties) {
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        if (!orcidUser.getORCIDs().contains(orcid)) {
            throw new ForbiddenException();
        }
        try {
            orcidUser.setUserProperties(orcid, userProperties);
            MCRUserManager.updateUser(orcidUser.getUser());
        } catch (MCRORCIDException e) {
            throw new BadRequestException(e);
        }
        return Response.ok().build();
    }

    static class MCRORCIDUserStatus {

        private List<String> orcids;

        private List<String> trustedOrcids;

        MCRORCIDUserStatus(List<String> orcids, List<String> trustedOrcids) {
            this.orcids = orcids;
            this.trustedOrcids = trustedOrcids;
        }

        @JsonProperty("orcids")
        public List<String> getOrcids() {
            return orcids;
        }

        @JsonProperty("trustedOrcids")
        public List<String> getTrustedOrcids() {
            return trustedOrcids;
        }
    }
}
