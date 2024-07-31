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

package org.mycore.mcr.acl.accesskey.restapi.v2;

import java.util.List;
import java.util.UUID;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyConstants;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceImpl;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * A RESTful API for managing access keys. This API provides methods for creating, retrieving, updating,
 * partially updating, and deleting access keys.
 */
@MCRApiDraft("MCRAccessKey2")
@Path("access-keys")
public class MCRAccessKeyResource {

    private static final String PATH_PARAM_ID = "id";

    private static final String PATH_PARAM_REFERENCE = "reference";

    private static final String QUERY_PARAM_REFERENCE = "reference";

    private static final String QUERY_PARAM_OFFSET = "offset";

    private static final String QUERY_PARAM_LIMIT = "limit";

    private static final String HEADER_TOTAL_COUNT = "X-Total-Count";

    @Context
    UriInfo uriInfo;

    /**
     * Returns currents users access key permissions for objectId.
     *
     * @param objectIdString the object id
     * @return permissions DTO
     */
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/{" + PATH_PARAM_REFERENCE + "}/permissions")
    public PermissionsDto getPermissions(@PathParam(PATH_PARAM_REFERENCE) String reference) {
        final boolean manageReadAccessKeys
            = MCRAccessManager.checkPermission(reference, MCRAccessKeyConstants.PERMISSION_MANAGE_READ_ACCESS_KEYS);
        final boolean manageWriteAccessKeys
            = MCRAccessManager.checkPermission(reference, MCRAccessKeyConstants.PERMISSION_MANAGE_WRITE_ACCESS_KEYS);
        return new PermissionsDto(manageReadAccessKeys, manageWriteAccessKeys);
    }

    /**
     * Creates a new access key.
     *
     * @param accessKeyDto the DTO containing the details of the access key to be created
     * @return a response indicating the outcome of the create operation
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @MCRRequireTransaction
    public Response createAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        final MCRAccessKeyDto createdAccessKeyDto = MCRAccessKeyServiceImpl.getInstance().createAccessKey(accessKeyDto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(createdAccessKeyDto.getId().toString()).build())
            .build();
    }

    /**
     * Retrieves a list of access keys, with optional pagination.
     *
     * @param offset the offset of the first access key to be retrieved
     * @param limit the maximum number of access keys to be retrieved
     * @param response the response object, used to set the total count header
     * @param objectIdString the object ID to filter access keys by
     * @return a list of AccessKeyDto objects
     */
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<MCRAccessKeyDto> getAccessKeys(@DefaultValue("0") @QueryParam(QUERY_PARAM_OFFSET) int offset,
        @DefaultValue("128") @QueryParam(QUERY_PARAM_LIMIT) int limit, @Context HttpServletResponse response,
        @QueryParam(QUERY_PARAM_REFERENCE) String reference) {
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyServiceImpl.getInstance().getAccessKeysByReference(reference);
        response.setHeader(HEADER_TOTAL_COUNT, Integer.toString(accessKeyDtos.size()));
        return accessKeyDtos.stream().skip(offset).limit(limit)
            .sorted((a1, a2) -> a1.getCreated().compareTo(a2.getCreated())).toList();
    }

    /**
     * Retrieves a specific access key by id.
     *
     * @param id the id associated with the access key
     * @return the AccessKeyDto object representing the access key
     */
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/{" + PATH_PARAM_ID + "}")
    public MCRAccessKeyDto getAccessKey(@PathParam(PATH_PARAM_ID) UUID id) {
        return MCRAccessKeyServiceImpl.getInstance().getAccessKeyById(id);
    }

    /**
     * Updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the DTO containing the updated details of the access key
     * @return a Response indicating the outcome of the update operation
     */
    @MCRRequireTransaction
    @PUT
    @Path("/{" + PATH_PARAM_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAccessKey(@PathParam(PATH_PARAM_ID) UUID id, MCRAccessKeyDto accessKeyDto)
        throws MCRAccessException {
        MCRAccessKeyServiceImpl.getInstance().updateAccessKeyById(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Partially updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the data transfer object containing the partial updates for the access key
     * @return a Response indicating the outcome of the partial update operation
     */
    @MCRRequireTransaction
    @PATCH
    @Path("/{" + PATH_PARAM_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response partialUpdateAccessKey(@PathParam(PATH_PARAM_ID) UUID id,
        MCRAccessKeyPartialUpdateDto accessKeyDto) throws MCRAccessException {
        MCRAccessKeyServiceImpl.getInstance().partialUpdateAccessKeyById(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Deletes an existing access key by id.
     *
     * @param reference the reference associated with the access key
     * @param value the value of the access key
     * @param valueEncoding the encoding of the value, if any
     * @return a Response indicating the outcome of the remove operation
     * @throws MCRAccessException if there is an access error during the removal of the access key
     */
    @MCRRequireTransaction
    @DELETE
    @Path("/{" + PATH_PARAM_ID + "}")
    public Response deleteAccessKey(@PathParam(PATH_PARAM_ID) UUID id) throws MCRAccessException {
        MCRAccessKeyServiceImpl.getInstance().deleteAccessKeyById(id);
        return Response.noContent().build();
    }

    record PermissionsDto(@JsonProperty(value = "manageReadAccessKeys") boolean manageReadAccessKeys,
        @JsonProperty(value = "manageWriteAccessKeys") boolean manageWriteAccessKeys) {
    }

}
