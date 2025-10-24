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

package org.mycore.orcid2.v3.rest.resources;

import java.util.Optional;
import java.util.Set;

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
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.rest.resources.MCRORCIDRestConstants;
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
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for managing ORCID-related objects and their status in user profiles.
 */
@Path("orcid")
public class MCRORCIDObjectResource {

    /**
     * Retrieves the ORCID put code information for a specific publication (work) associated with the given ORCID iD
     * and object ID using the ORCID Member API.
     *
     * <p>Example JSON response:</p>
     * <pre>
     * {
     *   "own": 123456,
     *   "other": [789012, 345678],
     * }
     * </pre>
     *
     * @param orcid the ORCID iD of the user
     * @param objectId the object ID of the object
     * @return an {@link MCRORCIDPutCodeInfo} object
     * @throws NotFoundException if the specified object ID does not exist
     * @throws ForbiddenException if the user does not have a valid access token for the specified ORCID iD
     * @throws InternalServerErrorException if there is an error retrieving the object from persistence
     * @throws BadRequestException if there is an error during the process
     *
     * @see MCRORCIDWorkService#getPutCodeInfo
     */
    @GET
    @Path("v1/member/{" + MCRORCIDRestConstants.PATH_PARAM_ORCID + "}/works/object/{"
        + MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public MCRORCIDPutCodeInfo getWorkInfoByMemberApi(@PathParam(MCRORCIDRestConstants.PATH_PARAM_ORCID) String orcid,
        @PathParam(MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID) MCRObjectID objectId) {
        return getWorkInfo(orcid, getCredentialForUser(orcid), getObjectOrThrow(objectId));
    }

    /**
     * Retrieves the ORCID put code information for a specific publication (work) associated with the given ORCID iD
     * and object ID using the ORCID Public API.
     *
     * <p>Example JSON response:</p>
     * <pre>
     * {
     *   "own": 123456,
     *   "other": [789012, 345678],
     * }
     * </pre>
     *
     * @param orcid the ORCID iD of the user
     * @param objectId the object ID of the object
     * @return an {@link MCRORCIDPutCodeInfo} object
     * @throws NotFoundException if the specified object ID does not exist
     * @throws InternalServerErrorException if there is an error retrieving the object from persistence
     * @throws BadRequestException if there is an error during the process
     *
     * @see MCRORCIDWorkService#getPutCodeInfo
     */
    @GET
    @Path("v1/public/{" + MCRORCIDRestConstants.PATH_PARAM_ORCID + "}/works/object/{"
        + MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public MCRORCIDPutCodeInfo getWorkInfoByPublicApi(@PathParam(MCRORCIDRestConstants.PATH_PARAM_ORCID) String orcid,
        @PathParam(MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID) MCRObjectID objectId) {
        return getWorkInfo(orcid, new MCRORCIDCredential(), getObjectOrThrow(objectId));
    }

    /**
     * Creates a new object (work) in the ORCID profile of the specified user using the ORCID Member API.
     *
     * This method allows a logged-in user to create a new work entry
     * in their ORCID profile, provided that the user has authorized access to their
     * ORCID account.
     *
     * @param orcid the ORCID iD of the user whose profile will be updated
     * @param objectId the ID of the object to be created, represented as an {@link MCRObjectID}
     * @return a {@link Response} object with status 200 OK if the work is successfully created
     * @throws ForbiddenException if the user does not have a valid access token for the specified ORCID iD
     * @throws NotFoundException if the specified object ID does not exist
     * @throws InternalServerErrorException if there is an error retrieving the object from persistence
     * @throws BadRequestException if there is an error during the process or user has no relation to the object
     *
     * @see MCRORCIDWorkService#createWork
     */
    @POST
    @Path("v1/member/{" + MCRORCIDRestConstants.PATH_PARAM_ORCID + "}/works/object/{"
        + MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID + "}")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @MCRRequireTransaction
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response createObject(@PathParam(MCRORCIDRestConstants.PATH_PARAM_ORCID) String orcid,
        @PathParam(MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID) MCRObjectID objectId) {
        final MCRORCIDCredential credential = getCredentialForUser(orcid);
        final MCRObject object = checkUserRelationAndRetrieveObject(objectId);

        final MCRSession session = MCRSessionMgr.getCurrentSession();
        final MCRUserInformation savedUserInformation = session.getUserInformation();
        session.setUserInformation(MCRSystemUserInformation.GUEST);
        session.setUserInformation(MCRSystemUserInformation.JANITOR);
        try {
            new MCRORCIDWorkService(orcid, credential).createWork(object);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create ORCID work.", e);
        } finally {
            session.setUserInformation(MCRSystemUserInformation.GUEST);
            session.setUserInformation(savedUserInformation);
        }
        return Response.ok().build();
    }

    /**
     * Updates an existing object (work) in the user's ORCID profile using the ORCID Member API.
     *
     * @param orcid the ORCID iD of the user
     * @param objectId the ID of the object to be updated
     * @return a Response with status 200 OK if the work was successfully updated
     * @throws ForbiddenException if the user does not have valid permissions for the specified ORCID iD
     * @throws NotFoundException if the specified object does not exist
     * @throws InternalServerErrorException if there is an error retrieving or updating the object
     * @throws BadRequestException if there is an issue with the request data or the connection to the ORCID API
     *
     * @see MCRORCIDWorkService#updateWork
     */
    @PUT
    @Path("v1/member/{" + MCRORCIDRestConstants.PATH_PARAM_ORCID + "}/works/object/{"
        + MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID + "}")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @MCRRequireTransaction
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response updateObject(@PathParam(MCRORCIDRestConstants.PATH_PARAM_ORCID) String orcid,
        @PathParam(MCRORCIDRestConstants.PATH_PARAM_OBJECT_ID) MCRObjectID objectId) {
        final MCRORCIDCredential credential = getCredentialForUser(orcid);
        final MCRObject object = checkUserRelationAndRetrieveObject(objectId);

        final MCRSession session = MCRSessionMgr.getCurrentSession();
        final MCRUserInformation savedUserInformation = session.getUserInformation();
        session.setUserInformation(MCRSystemUserInformation.GUEST);
        session.setUserInformation(MCRSystemUserInformation.JANITOR);
        try {
            new MCRORCIDWorkService(orcid, credential).updateWork(object);
        } catch (Exception e) {
            throw new BadRequestException("Failed to update ORCID work.", e);
        } finally {
            session.setUserInformation(MCRSystemUserInformation.GUEST);
            session.setUserInformation(savedUserInformation);
        }
        return Response.ok().build();
    }

    private MCRObject checkUserRelationAndRetrieveObject(MCRObjectID objectId) {
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final MCRObject object = getObjectOrThrow(objectId);
        if (!MCRORCIDUserUtils.checkUserHasObjectRelation(orcidUser, object)) {
            throw new BadRequestException("User has no relation to the object.");
        }
        return object;
    }

    private MCRORCIDCredential getCredentialForUser(String orcid) {
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        return Optional.ofNullable(orcidUser.getCredentialByORCID(orcid))
                .filter(c -> c.getAccessToken() != null)
                .orElseThrow(ForbiddenException::new);
    }

    private MCRObject getObjectOrThrow(MCRObjectID objectID) {
        if (!MCRMetadataManager.exists(objectID)) {
            throw new NotFoundException("Object not found: " + objectID);
        }
        try {
            return MCRMetadataManager.retrieveMCRObject(objectID);
        } catch (MCRPersistenceException e) {
            throw new InternalServerErrorException("Failed to retrieve object from persistence.", e);
        }
    }

    private MCRORCIDPutCodeInfo getWorkInfo(String orcid, MCRORCIDCredential credential, MCRObject object) {
        try {
            final Work work = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(object.createXML()));
            final Set<MCRIdentifier> identifiers = MCRORCIDWorkUtils.listTrustedIdentifiers(work);
            return new MCRORCIDWorkService(orcid, credential).getPutCodeInfo(identifiers);
        } catch (MCRORCIDException e) {
            throw new BadRequestException("Failed to retrieve ORCID put code info.", e);
        }
    }

}
