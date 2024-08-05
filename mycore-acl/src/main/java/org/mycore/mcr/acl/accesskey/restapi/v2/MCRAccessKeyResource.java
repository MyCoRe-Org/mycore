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
import org.mycore.mcr.acl.accesskey.MCRAccessKeyService;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyManagementPermissionsDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
     * @param reference the reference
     * @return permissions DTO
     */
    @Operation(summary = "Get management permission")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
        content = { @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyManagementPermissionsDto.class)), }), })
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/{" + PATH_PARAM_REFERENCE + "}/management-permissions")
    public MCRAccessKeyManagementPermissionsDto
        getManagementPermissions(@PathParam(PATH_PARAM_REFERENCE) String reference) {
        return MCRAccessKeyServiceFactory.getService().getManagementPermissionsByReference(reference);
    }

    /**
     * Creates a new access key.
     *
     * @param accessKeyDto the DTO containing the details of the access key to be created
     * @return a response indicating the outcome of the create operation
     * @throws MCRAccessException if current user is not allowed to create access key
     */
    @Operation(summary = "Create access key")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Access key created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @MCRRequireTransaction
    public Response createAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        final MCRAccessKeyDto createdAccessKeyDto
            = MCRAccessKeyServiceFactory.getService().createAccessKey(accessKeyDto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(createdAccessKeyDto.getId().toString()).build())
            .build();
    }

    /**
     * Retrieves a list of access keys, with optional pagination.
     *
     * @param offset the offset of the first access key to be retrieved
     * @param limit the maximum number of access keys to be retrieved
     * @param response the response object, used to set the total count header
     * @param reference optional reference to filter by
     * @param permission optional permission to filter by
     * @return a list of AccessKeyDto objects
     */
    @Operation(summary = "Get access keys")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(implementation = MCRAccessKeyDto.class))), }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<MCRAccessKeyDto> getAccessKeys(@DefaultValue("0") @QueryParam(QUERY_PARAM_OFFSET) int offset,
        @DefaultValue("128") @QueryParam(QUERY_PARAM_LIMIT) int limit, @Context HttpServletResponse response,
        @QueryParam(QUERY_PARAM_REFERENCE) String reference, @QueryParam(QUERY_PARAM_REFERENCE) String permission) {
        final MCRAccessKeyService service = MCRAccessKeyServiceFactory.getService();
        List<MCRAccessKeyDto> accessKeyDtos = null;
        if (reference != null && permission != null) {
            accessKeyDtos = service.getAccessKeysByReferenceAndPermission(reference, permission);
        } else if (reference != null && permission == null) {
            accessKeyDtos = service.getAccessKeysByReference(reference);
        } else if (reference != null && permission != null) {
            accessKeyDtos = service.getAccessKeysByPermission(permission);
        } else {
            accessKeyDtos = service.getAllAccessKeys();
        }
        response.setHeader(HEADER_TOTAL_COUNT, Integer.toString(accessKeyDtos.size()));
        return accessKeyDtos.stream().skip(offset).limit(limit)
            .sorted((a1, a2) -> a1.getCreated().compareTo(a2.getCreated())).toList();
    }

    /**
     * Retrieves a specific access key by id.
     *
     * @param id the id associated with the access key
     * @return the AccessKeyDto object representing the access key
     * @throws MCRAccessException if current user is not allowed to get access key
     */
    @Operation(summary = "Get an access key by id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRAccessKeyDto.class)), }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key not found",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/{" + PATH_PARAM_ID + "}")
    public MCRAccessKeyDto getAccessKeyById(@PathParam(PATH_PARAM_ID) UUID id) throws MCRAccessException {
        return MCRAccessKeyServiceFactory.getService().getAccessKeyById(id);
    }

    /**
     * Updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the DTO containing the updated details of the access key
     * @return a Response indicating the outcome of the update operation
     * @throws MCRAccessException if current user is not allowed to update access key
     */
    @Operation(summary = "Update an access key by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Access key update sucessfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key not found",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @MCRRequireTransaction
    @PUT
    @Path("/{" + PATH_PARAM_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAccessKeyById(@PathParam(PATH_PARAM_ID) UUID id, MCRAccessKeyDto accessKeyDto)
        throws MCRAccessException {
        MCRAccessKeyServiceFactory.getService().updateAccessKeyById(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Partially updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the data transfer object containing the partial updates for the access key
     * @return a Response indicating the outcome of the partial update operation
     * @throws MCRAccessException if current user is not allowed to partial update access key
     */
    @Operation(summary = "Partial update an access key by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Access key update sucessfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key not found",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @MCRRequireTransaction
    @PATCH
    @Path("/{" + PATH_PARAM_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response partialUpdateAccessKeyById(@PathParam(PATH_PARAM_ID) UUID id,
        MCRAccessKeyPartialUpdateDto accessKeyDto) throws MCRAccessException {
        MCRAccessKeyServiceFactory.getService().partialUpdateAccessKeyById(id, accessKeyDto);
        return Response.noContent().build();
    }

    /**
     * Deletes an existing access key by id.
     *
     * @param id the id of the access key
     * @return a Response indicating the outcome of the remove operation
     * @throws MCRAccessException if current user is not allowed to delete access key
     */
    @Operation(summary = "Delete an access key by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Access key deleted sucessfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key does not exist",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @MCRRequireTransaction
    @DELETE
    @Path("/{" + PATH_PARAM_ID + "}")
    public Response deleteAccessKeyById(@PathParam(PATH_PARAM_ID) UUID id) throws MCRAccessException {
        MCRAccessKeyServiceFactory.getService().deleteAccessKeyById(id);
        return Response.noContent().build();
    }

}
