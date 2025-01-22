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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.mycore.orcid2.v3.work.MCRORCIDWorkService;
import org.mycore.orcid2.v3.work.MCRORCIDWorkUtils;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.orcid.jaxb.model.v3.release.record.Work;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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
    public MCRORCIDPublicationStatus getObjectStatus(@PathParam("objectID") MCRObjectID objectID) {
        final MCRObject object = getObject(objectID);
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        if (!MCRORCIDUserUtils.checkUserHasObjectRelation(orcidUser, object)) {
            return new MCRORCIDPublicationStatus(false, false);
        }
        final Map.Entry<String, MCRORCIDCredential> entry = getCredential(orcidUser, object);
        if (entry == null) {
            return new MCRORCIDPublicationStatus(true, false);
        }
        try {
            final Work work = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(object.createXML()));
            final Set<MCRIdentifier> identifiers = MCRORCIDWorkUtils.listTrustedIdentifiers(work);
            final boolean result
                = new MCRORCIDWorkService(entry.getKey(), entry.getValue()).checkOwnWorkExists(identifiers);
            return new MCRORCIDPublicationStatus(true, result);
        } catch (Exception e) {
            LOGGER.error("Error while retrieving status: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    /**
     * Creates MCRObject in ORCID profile for given MCRORCIDCredential.
     *
     * @param objectID the MCRObjectID
     * @return the Response
     * @throws WebApplicationException if request fails
     * @see MCRORCIDWorkService#createWork
     */
    @POST
    @Path("create-work/v3/{objectID}")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @MCRRequireTransaction
    public Response createObject(@PathParam("objectID") MCRObjectID objectID) {
        final MCRObject object = getObject(objectID);
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        if (!MCRORCIDUserUtils.checkUserHasObjectRelation(orcidUser, object)) {
            throw new BadRequestException("User has no relation to the object.");
        }
        final Map.Entry<String, MCRORCIDCredential> entry = getCredential(orcidUser, object);
        if (entry == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        final String orcid = entry.getKey();
        final MCRORCIDCredential credential = entry.getValue();

        final MCRSession session = MCRSessionMgr.getCurrentSession();
        final MCRUserInformation savedUserInformation = session.getUserInformation();
        session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
        session.setUserInformation(MCRSystemUserInformation.getJanitorInstance());
        try {
            new MCRORCIDWorkService(orcid, credential).createWork(object);
        } catch (Exception e) {
            LOGGER.error("Error while creating: ", e);
            throw new WebApplicationException(Status.BAD_REQUEST);
        } finally {
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(savedUserInformation);
        }
        return Response.ok().build();
    }

    private Map.Entry<String, MCRORCIDCredential> getCredential(MCRORCIDUser orcidUser, MCRObject object) {
        final Map<String, MCRORCIDCredential> credentialMap = orcidUser.getCredentials();
        if (credentialMap.isEmpty()) {
            return null;
        }
        if (credentialMap.size() > 1) {
            // try to find matching orcid/credential
            final Set<String> orcids = credentialMap.keySet();
            orcids.retainAll(MCRORCIDUtils.getORCIDs(object));
            if (orcids.size() == 1) {
                return credentialMap.entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getKey(), orcids.iterator().next())).findFirst()
                    .orElseThrow(() -> new WebApplicationException(Status.BAD_REQUEST));
            }
            throw new WebApplicationException(Status.BAD_REQUEST);

        }
        return credentialMap.entrySet().iterator().next();
    }

    private MCRObject getObject(MCRObjectID objectID) {
        if (!MCRMetadataManager.exists(objectID)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        try {
            return MCRMetadataManager.retrieveMCRObject(objectID);
        } catch (MCRPersistenceException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
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
