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

package org.mycore.mcr.acl.accesskey.restapi.v2;

import static org.mycore.restapi.v2.MCRRestStatusCode.BAD_REQUEST;
import static org.mycore.restapi.v2.MCRRestStatusCode.CREATED;
import static org.mycore.restapi.v2.MCRRestStatusCode.NOT_FOUND;
import static org.mycore.restapi.v2.MCRRestStatusCode.NO_CONTENT;
import static org.mycore.restapi.v2.MCRRestStatusCode.OK;
import static org.mycore.restapi.v2.MCRRestStatusCode.UNAUTHORIZED;

import java.util.List;
import java.util.UUID;

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.restapi.v2.access.MCRAccessKeyRestAccessCheckStrategy;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.v2.MCRRestSchemaType;
import org.mycore.restapi.v2.annotation.MCRRestAccessCheck;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    private static final String DESCRIPTION_UNAUTHORIZED = "Unauthorized";

    private static final String DESCRIPTION_ACCESS_KEY_NOT_FOUND = "Access key not found";

    private static final String DESCRIPTION_INVALID_INPUT = "Invalid input data";

    private static final String DESCRIPTION_ACCESS_KEY_ID = "The access key id";

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletResponse response;

    /**
     * Retrieves a list of access keys, with optional pagination and filters.
     *
     * @param offset the offset of the first access key to be retrieved
     * @param limit the maximum number of access keys to be retrieved
     * @param reference optional reference to filter by
     * @param permissions optional permissions to filter by
     * @return a list of AccessKeyDto objects
     */
    @Operation(summary = "Get all access keys", description = "Retrieve a list of all access keys.")
    @ApiResponses({
        @ApiResponse(responseCode = OK,
            content = {
                @Content(mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = MCRAccessKeyDto.class))),
            },
            headers = {
                @Header(name = MCRAccessKeyRestConstants.HEADER_TOTAL_COUNT,
                    schema = @Schema(type = MCRRestSchemaType.INTEGER))
            }),
        @ApiResponse(responseCode = UNAUTHORIZED, description = DESCRIPTION_UNAUTHORIZED,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRAccessKeyRestAccessCheckStrategy.class)
    public List<MCRAccessKeyDto> findAccessKeys(
        @Parameter(in = ParameterIn.QUERY, description = "The offset for pagination (default is 0)",
            required = false, schema = @Schema(defaultValue = "0"))
        @DefaultValue("0") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_OFFSET) int offset,
        @Parameter(in = ParameterIn.QUERY,
            description = "The number of results to return, defaults to 128 if not provided", required = false,
            schema = @Schema(defaultValue = "128"))
        @DefaultValue("128") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_LIMIT) int limit,
        @Parameter(in = ParameterIn.QUERY, description = "The reference filter (default is all)", required = false,
            schema = @Schema(defaultValue = ""))
        @DefaultValue("") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_REFERENCE) String reference,
        @Parameter(in = ParameterIn.QUERY, description = "The permissions filter (default is all)", required = false,
            schema = @Schema(defaultValue = ""))
        @DefaultValue("") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_PERMISSIONS) String permissions) {
        return MCRAccessKeyRestHelper.findAccessKeys(reference, permissions, offset, limit, response);
    }

    /**
     * Retrieves a specific access key by id.
     *
     * @param id the id associated with the access key
     * @return the AccessKeyDto object representing the access key
     */
    @Operation(summary = "Get an access key by id", description = "Retrieves a specific access key by id.")
    @ApiResponses({
        @ApiResponse(responseCode = OK,
            content = {
                @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MCRAccessKeyDto.class))
            }),
        @ApiResponse(responseCode = UNAUTHORIZED, description = DESCRIPTION_UNAUTHORIZED,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = NOT_FOUND, description = DESCRIPTION_ACCESS_KEY_NOT_FOUND,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
    })
    @GET
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRAccessKeyRestAccessCheckStrategy.class)
    public MCRAccessKeyDto findAccessKey(
        @Parameter(in = ParameterIn.PATH, description = DESCRIPTION_ACCESS_KEY_ID, required = true,
            schema = @Schema(type = MCRRestSchemaType.STRING, implementation = UUID.class))
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id) {
        return MCRAccessKeyRestHelper.findAccessKey(id);
    }

    /**
     * Creates a new access key.
     *
     * @param accessKeyDto the DTO containing the details of the access key to be created
     * @return a response indicating the outcome of the create operation
     */
    @Operation(summary = "Create access key", description = "Creates a new access key.")
    @ApiResponses({
        @ApiResponse(responseCode = CREATED, description = "Access key successfully created",
            headers = @Header(name = HttpHeaders.LOCATION,
                schema = @Schema(type = MCRRestSchemaType.STRING, format = "uri"),
                description = "Location of the new access key")),
        @ApiResponse(responseCode = BAD_REQUEST, description = DESCRIPTION_INVALID_INPUT,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = UNAUTHORIZED, description = DESCRIPTION_UNAUTHORIZED,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
    })
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRequireTransaction
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRAccessKeyRestAccessCheckStrategy.class)
    public Response addAccessKey(
        @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyDto.class))) MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyRestHelper.addAccessKey(accessKeyDto, uriInfo);
    }

    /**
     * Updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the DTO containing the updated details of the access key
     * @return a Response indicating the outcome of the update operation
     */
    @Operation(summary = "Update an access key by id", description = "Updates an existing access key by id.")
    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT, description = "Access key update successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = DESCRIPTION_INVALID_INPUT,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = UNAUTHORIZED, description = DESCRIPTION_UNAUTHORIZED,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = NOT_FOUND, description = DESCRIPTION_ACCESS_KEY_NOT_FOUND,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
    })
    @PUT
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRAccessKeyRestAccessCheckStrategy.class)
    @MCRRequireTransaction
    public Response updateAccessKey(
        @Parameter(in = ParameterIn.PATH, description = DESCRIPTION_ACCESS_KEY_ID, required = true,
            schema = @Schema(type = MCRRestSchemaType.STRING, implementation = UUID.class))
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id,
        @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyDto.class))) MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyRestHelper.updateAccessKey(id, accessKeyDto);
    }

    /**
     * Partially updates an existing access key by id.
     *
     * @param id the id associated with the access key
     * @param accessKeyDto the data transfer object containing the partial updates for the access key
     * @return a Response indicating the outcome of the partial update operation
     */
    @Operation(summary = "Partial update an access key by id",
        description = "Partially updates an existing access key by id.")
    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT, description = "Access key update sucessfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = DESCRIPTION_INVALID_INPUT,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = UNAUTHORIZED, description = DESCRIPTION_UNAUTHORIZED,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = NOT_FOUND, description = DESCRIPTION_ACCESS_KEY_NOT_FOUND,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
    })
    @PATCH
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRAccessKeyRestAccessCheckStrategy.class)
    @MCRRequireTransaction
    public Response partialUpdateAccessKey(
        @Parameter(in = ParameterIn.PATH, description = DESCRIPTION_ACCESS_KEY_ID, required = true,
            schema = @Schema(type = MCRRestSchemaType.STRING, implementation = UUID.class))
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id,
        @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
            implementation = MCRAccessKeyPartialUpdateDto.class))) MCRAccessKeyPartialUpdateDto accessKeyDto) {
        return MCRAccessKeyRestHelper.partialUpdateAccessKey(id, accessKeyDto);
    }

    /**
     * Removes an existing access key by id.
     *
     * @param id the id of the access key
     * @return a Response indicating the outcome of the remove operation
     */
    @Operation(summary = "Delete an access key by id")
    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT, description = "Access key deleted successfully"),
        @ApiResponse(responseCode = UNAUTHORIZED, description = DESCRIPTION_UNAUTHORIZED,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        @ApiResponse(responseCode = NOT_FOUND, description = DESCRIPTION_ACCESS_KEY_NOT_FOUND,
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
    })
    @DELETE
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @MCRRestRequiredPermission(MCRAccessKeyRestConstants.PERMISSION_MANAGE_ACCESS_KEY)
    @MCRRestAccessCheck(strategy = MCRAccessKeyRestAccessCheckStrategy.class)
    @MCRRequireTransaction
    public Response removeAccessKey(
        @Parameter(in = ParameterIn.PATH, description = DESCRIPTION_ACCESS_KEY_ID, required = true,
            schema = @Schema(type = MCRRestSchemaType.STRING, implementation = UUID.class))
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id) {
        return MCRAccessKeyRestHelper.removeAccessKey(id);
    }

}
