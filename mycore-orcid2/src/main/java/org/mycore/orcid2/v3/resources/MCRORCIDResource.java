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

package org.mycore.orcid2.v3.resources;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.v3.MCRORCIDAPIClientFactoryImpl;
import org.mycore.orcid2.v3.MCRORCIDSectionImpl;
import org.mycore.orcid2.v3.MCRORCIDWorkHelper;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformer;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;

@Path("orcid")
public class MCRORCIDResource {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns the publication status for a given MCRObjectID in the current user's ORCID profile, e.g.
     *
     * {
     *   "isUsersPublication": true,
     *   "isInORCIDProfile": true,
     * }
     *
     * @see org.mycore.orcid2.user.MCRPublicationStatus
     */
    @GET
    @Path("objectStatus/{objectID}")
    @Produces(MediaType.APPLICATION_JSON)
    public MCRORCIDPublicationStatus getPublicationStatus(@PathParam("objectID") String objectIDString)
        throws WebApplicationException {
        if (isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRObjectID objectID = MCRJerseyUtil.getID(objectIDString);
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        // assumption that a publication cannot be in the profile, it is if not users.
        if (orcidUser.isMyPublication(objectID)) {
            return new MCRORCIDPublicationStatus(false, false);
        }
        final MCRORCIDCredentials credentials = orcidUser.getCredentials();
        if (credentials == null) {
            return new MCRORCIDPublicationStatus(true, false);
        }
        try {
            final Works works = MCRORCIDAPIClientFactoryImpl.getInstance().createMemberClient(credentials)
                .fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            final List<WorkSummary> summaries
                = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
            final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
            final boolean result = MCRORCIDWorkHelper.findMatchingSummaries(object, summaries).findAny().isPresent();
            return new MCRORCIDPublicationStatus(true, result);
        } catch (Exception e) {
            LOGGER.error("Error while retrieving status: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    /**
     * Publishes a work in the current user's ORCID profile, or
     * updates an existing work there, given the object ID of the local MODS object.
     *
     * The request path must contain the MCRObjectID to publish.
     * The current user must have an ORCID profile and must have authorized this application
     * to add or updated works.
     *
     * Returns the new publication status as by {@link #getPublicationStatus(String)}
     */
    @GET
    @Path("publish/{objectID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish(@PathParam("objectID") String objectIDString) throws WebApplicationException {
        if (isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRObjectID objectID = MCRJerseyUtil.getID(objectIDString);
        if (!MCRMetadataManager.exists(objectID)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        if (!orcidUser.isMyPublication(objectID)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        final MCRORCIDCredentials credentials = orcidUser.getCredentials();
        if (credentials == null || credentials.getAccessToken() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        try {
            final MCRORCIDClient memberClient = MCRORCIDAPIClientFactoryImpl.getInstance()
                .createMemberClient(credentials);
            final Works works = memberClient.fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            final List<WorkSummary> summaries
                = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
            final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
            final WorkSummary summary
                = MCRORCIDWorkHelper.findMatchingSummaries(object, summaries).findFirst().orElse(null);
            final MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(object.getId());
            final Work transformedWork = MCRORCIDWorkTransformer.getInstance().transformToWork(content);
            if (summary == null) {
                memberClient.create(MCRORCIDSectionImpl.WORK, transformedWork);
            } else {
                memberClient.update(MCRORCIDSectionImpl.WORK, summary.getPutCode(), transformedWork);
            }
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Error while publishing: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    private boolean isCurrentUserGuest() {
        return MCRSystemUserInformation.getGuestInstance().getUserID()
            .equals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
    }

    class MCRORCIDPublicationStatus {

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
