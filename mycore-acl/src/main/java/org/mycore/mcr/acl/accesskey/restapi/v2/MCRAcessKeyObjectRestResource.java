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

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.mycore.restapi.v2.MCRRestAuthorizationFilter;
import org.mycore.restapi.v2.MCRRestUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * A RESTful API for managing object access keys.
 */
@MCRApiDraft("MCRAccessKey2")
@Path("/objects/{" + MCRRestAuthorizationFilter.PARAM_MCRID + "}/access-keys")
@Tag(name = MCRRestUtils.TAG_MYCORE_OBJECT)
public class MCRAcessKeyObjectRestResource {

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletResponse response;

    /**
     * Returns all access keys for object by id and permission.
     *
     * @param objectId the object id
     * @param permission the permission
     * @param offset the optional offset
     * @param limit the optional list
     * @return list of all access keys for object
     */
    @Operation(summary = "Lists all access keys for an object",
        responses = {
            @ApiResponse(responseCode = "200",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = MCRAccessKeyDto.class))), }),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Object or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // TODO check permission, use MCR-3164
    public List<MCRAccessKeyDto> getAccessKeys(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @DefaultValue("") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_PERMISSION) String permission,
        @DefaultValue("0") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_OFFSET) int offset,
        @DefaultValue("128") @QueryParam(MCRAccessKeyRestConstants.QUERY_PARAM_LIMIT) int limit) {
        return MCRAccessKeyRestHelper.getAccessKeys(objectId.toString(), permission, offset, limit, response);
    }

    /**
     * Returns specific access key by id for object.
     *
     * @param objectId the object id
     * @param id the access key id
     * @return the access key
     */
    @Operation(summary = "Gets access key for an object",
        responses = {
            @ApiResponse(responseCode = "200", description = "Information about a specific access key",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MCRAccessKeyDto.class))),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Object or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @GET
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    // TODO check permission, use MCR-3164
    public MCRAccessKeyDto getAccessKeyById(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id) {
        return MCRAccessKeyRestHelper.getAccessKeyById(id);
    }

    /**
     * Creates access key for object id.
     *
     * @param objectId the object id
     * @param accessKeyDto the access key DTO
     * @return the response
     */
    @Operation(summary = "Creates an access key for an object",
        responses = {
            @ApiResponse(responseCode = "201", description = "Access key was successfully created",
                headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(type = "string", format = "uri"),
                    description = "Location of the new access keyO")),
            @ApiResponse(responseCode = "400", description = "Invalid ID or invalid access key",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Object does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyDto.class)))
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // TODO check permission, use MCR-3164
    @MCRRequireTransaction
    public Response createAccessKey(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyRestHelper.createAccessKey(accessKeyDto, uriInfo);
    }

    /**
     * Updates access key by id.
     *
     * @param objectId the reference
     * @param id the access key id
     * @param accessKeyDto the access key DTO
     * @return the response
     */
    @Operation(summary = "Updates an access key for an object",
        responses = { @ApiResponse(responseCode = "204", description = "Access key was successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or invalid access key",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Object or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyDto.class)))
    @PUT
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // TODO check permission, use MCR-3164
    @MCRRequireTransaction
    public Response updateAccessKeyById(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id, MCRAccessKeyDto accessKeyDto) {
        return MCRAccessKeyRestHelper.updateAccessKeyById(id, accessKeyDto);
    }

    /**
     * Partial updates access key by id.
     *
     * @param objectId the reference
     * @param id the access key id
     * @param accessKeyDto the update DTO
     * @return the response
     */
    @Operation(summary = "Partial updates an access key for an object",
        responses = { @ApiResponse(responseCode = "204", description = "Access key was successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or invalid access key",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Object or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKeyDto.class)))
    @PATCH
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // TODO check permission, use MCR-3164
    @MCRRequireTransaction
    public Response partialUpdateAccessKeyById(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id,
        MCRAccessKeyPartialUpdateDto accessKeyDto) {
        return MCRAccessKeyRestHelper.partialUpdateAccessKeyById(id, accessKeyDto);
    }

    /**
     * Deletes access key by id.
     *
     * @param objectId the reference
     * @param id the access key id
     * @return the response
     */
    @Operation(summary = "Deletes an access key from an object",
        responses = { @ApiResponse(responseCode = "204", description = "Access key was successfully deleted"),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Object or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }), })
    @DELETE
    @Path("/{" + MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    // TODO check permission, use MCR-3164
    @MCRRequireTransaction
    public Response deleteAccessKeyById(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRAccessKeyRestConstants.PATH_PARAM_ACCESS_KEY_ID) UUID id) {
        return MCRAccessKeyRestHelper.deleteAccessKeyById(id);
    }
}
