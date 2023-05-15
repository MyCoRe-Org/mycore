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

package org.mycore.orcid2.v3.rest.resources;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.v3.MCRORCIDClientHelper;
import org.mycore.orcid2.v3.MCRORCIDSectionImpl;
import org.mycore.orcid2.v3.MCRORCIDWorkHelper;
import org.mycore.orcid2.v3.MCRORCIDWorkSummaryUtils;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;

/**
 * Basic resource for general methods.
 */
@Path("/v1/")
public class MCRORCIDObjectResource {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns the publication status for a given MCRObjectID in the current user's ORCID profile, e.g.
     *
     * {
     *   "isUsersPublication": true,
     *   "isInORCIDProfile": true,
     * }
     *
     * @param objectID the MCRObjectID
     * @return the publication status
     * @throws WebApplicationException if request fails
     */
    @GET
    @Path("object-status/v3/{objectID}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public MCRORCIDPublicationStatus getPublicationStatus(@PathParam("objectID") MCRObjectID objectID) {
        final MCRObject object = getObject(objectID);
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final Set<String> orcids = getMatchingORCIDs(object, orcidUser);
        if (orcids.isEmpty()) {
            // assumption that a publication cannot be in the profile, it is if not users.
            return new MCRORCIDPublicationStatus(false, false);
        }
        if (orcids.size() > 1) { // should not happen
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        final String orcid = orcids.iterator().next();
        final MCRORCIDCredential credential = orcidUser.getCredentialByORCID(orcid);
        if (credential == null) {
            return new MCRORCIDPublicationStatus(true, false);
        }
        try {
            final Works works = MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential)
                .fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            final List<WorkSummary> summaries
                = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
            final boolean result = MCRORCIDWorkSummaryUtils.findMatchingSummariesByIdentifiers(object, summaries)
                .findAny().isPresent();
            return new MCRORCIDPublicationStatus(true, result);
        } catch (Exception e) {
            LOGGER.error("Error while retrieving status: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    /**
     * Creates MCRObject in ORCID profil for given MCRORCIDCredential.
     *
     * @param objectID the MCRObjectID
     * @return the Response
     * @throws WebApplicationException if request fails
     * @see MCRORCIDWorkHelper#createObjectAndUpdateWorkInfo
     */
    @POST
    @Path("create-object/v3/{objectID}")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @MCRRequireTransaction
    public Response createObject(@PathParam("objectID") MCRObjectID objectID) {
        final MCRObject object = getObject(objectID);
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final String orcid = getMatchingORCID(object, orcidUser);
        final MCRORCIDCredential credential = getCredential(orcidUser, orcid);
        switchToJanitor();
        try {
            MCRORCIDWorkHelper.createObjectAndUpdateWorkInfo(object, orcid, credential);
        } catch (Exception e) {
            LOGGER.error("Error while creating: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return Response.ok().build();
    }

    /**
     * Updates MCRObject in ORCID profil for given MCRORCIDCredential.
     *
     * @param objectID the MCRObjectID
     * @return the Response
     * @throws WebApplicationException if request fails
     * @see MCRORCIDWorkHelper#updateObjectAndUpdateWorkInfo
     */
    @POST
    @Path("update-object/v3/{objectID}")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @MCRRequireTransaction
    public Response updateObject(@PathParam("objectID") MCRObjectID objectID) {
        final MCRObject object = getObject(objectID);
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final String orcid = getMatchingORCID(object, orcidUser);
        final MCRORCIDCredential credential = getCredential(orcidUser, orcid);
        switchToJanitor();
        try {
            MCRORCIDWorkHelper.updateObjectAndUpdateWorkInfo(object, orcid, credential);
        } catch (Exception e) {
            LOGGER.error("Error while creating: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return Response.ok().build();
    }

    private void switchToJanitor() {
        final MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
        session.setUserInformation(MCRSystemUserInformation.getJanitorInstance());
    }

    private MCRORCIDCredential getCredential(MCRORCIDUser orcidUser, String orcid) {
        final MCRORCIDCredential credential = orcidUser.getCredentialByORCID(orcid);
        if (credential == null || credential.getAccessToken() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return credential;
    }

    private MCRObject getObject(MCRObjectID objectID) {
        if (!MCRMetadataManager.exists(objectID)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        try {
            return MCRMetadataManager.retrieveMCRObject(objectID);
        } catch(MCRPersistenceException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    private String getMatchingORCID(MCRObject object, MCRORCIDUser orcidUser) {
        final Set<String> orcids = getMatchingORCIDs(object, orcidUser);
        if (orcids.isEmpty()) {
            return null;
        }
        if (orcids.size() == 1) {
            return orcids.iterator().next();
        }
        throw new WebApplicationException(Status.BAD_REQUEST);
    }

    private Set<String> getMatchingORCIDs(MCRObject object, MCRORCIDUser orcidUser) {
        final Set<String> orcids = MCRORCIDUtils.getORCIDs(object);
        orcids.retainAll(orcidUser.getORCIDs());
        return orcids;
    }

    static class MCRORCIDPublicationStatus {

        private boolean isUsersPublication;

        private boolean isInORCIDProfile;

        MCRORCIDPublicationStatus(boolean isUsersPublication, boolean isInORCIDProfile) {
            this.isUsersPublication = isUsersPublication;
            this.isInORCIDProfile = isInORCIDProfile;
        }

        public boolean isUsersPublication() {
            return isUsersPublication;
        }

        public boolean isInORCIDProfile() {
            return isInORCIDProfile;
        }
    }
}
