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

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.restapi.v2.access.MCRRestAccessKeyAccessCheckStrategy;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.v2.annotation.MCRRestAccessCheck;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * A RESTful API for managing access keys. This API provides methods for creating, retrieving, updating,
 * partially updating, and deleting access keys.
 */
@MCRApiDraft("MCRAccessKey2")
@Path("access-keys")
public class MCRAccessKeyRestResource {

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletResponse response;

    /**
     * Retrieves a list of access keys, with optional pagination.
     *
     * @param offset the offset of the first access key to be retrieved
     * @param limit the maximum number of access keys to be retrieved
     * @param reference optional reference to filter by
     * @param permissions optional permissions to filter by
     * @return a list of AccessKeyDto objects
     */
    @Operation(summary = "Get access keys")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(implementation = MCRAccessKeyDto.class))), }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRRestAccessKeyAccessCheckStrategy.class)
    public List<MCRAccessKeyDto> findAccessKeys(
        @DefaultValue("0") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_OFFSET) int offset,
        @DefaultValue("128") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_LIMIT) int limit,
        @DefaultValue("") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_REFERENCE) String reference,
        @DefaultValue("") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_PERMISSIONS) String permissions) {
        return MCRAccessKeyRestHelper.findAccessKeys(reference, permissions, offset, limit, response);
    }

    /**
     * Retrieves a specific access key by id.
     *
     * @param id the id associated with the access key
     * @return the AccessKeyDto object representing the access key
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
    @GET
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRRestAccessKeyAccessCheckStrategy.class)
    public MCRAccessKeyDto findAccessKey(@PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id) {
        return MCRAccessKeyRestHelper.findAccessKey(id);
    }

    /**
     * Creates a new access key.
     *
     * @param accessKeyDto the DTO containing the details of the access key to be created
     * @return a response indicating the outcome of the create operation
     */
    @Operation(summary = "Create access key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Access key was successfully created",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(type = "string", format = "uri"),
                description = "Location of the new access key")),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRequireTransaction
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRRestAccessKeyAccessCheckStrategy.class)
    public Response addAccessKey(MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyRestHelper.addAccessKey(accessKeyDto, uriInfo);
    }

    /**
     * Updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the DTO containing the updated details of the access key
     * @return a Response indicating the outcome of the update operation
     */
    @Operation(summary = "Update an access key by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Access key update sucessfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key not found",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyDto.class)))
    @PUT
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRRestAccessKeyAccessCheckStrategy.class)
    @MCRRequireTransaction
    public Response updateAccessKey(@PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id,
        MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyRestHelper.updateAccessKey(id, accessKeyDto);
    }

    /**
     * Partially updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the data transfer object containing the partial updates for the access key
     * @return a Response indicating the outcome of the partial update operation
     */
    @Operation(summary = "Partial update an access key by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Access key update sucessfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key not found",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyPartialUpdateDto.class)))
    @PATCH
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRRestAccessKeyAccessCheckStrategy.class)
    @MCRRequireTransaction
    public Response partialUpdateAccessKey(@PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id,
        MCRAccessKeyPartialUpdateDto accessKeyDto) {
        return MCRAccessKeyRestHelper.partialUpdateAccessKey(id, accessKeyDto);
    }

    /**
     * Removes an existing access key by id.
     *
     * @param id the id of the access key
     * @return a Response indicating the outcome of the remove operation
     */
    @Operation(summary = "Delete an access key by id")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Access key deleted sucessfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = "404", description = "Access key does not exist",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @DELETE
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRRestAccessKeyAccessCheckStrategy.class)
    @MCRRequireTransaction
    public Response removeAccessKey(@PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id) {
        return MCRAccessKeyRestHelper.removeAccessKey(id);
    }

}
