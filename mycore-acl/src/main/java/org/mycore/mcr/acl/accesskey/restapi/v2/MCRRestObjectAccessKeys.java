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

import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.restapi.v2.dto.MCRAccessKeyInformation;
import org.mycore.mcr.acl.accesskey.restapi.v2.dto.MCRRestAccessKeyDto;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.mycore.restapi.v2.MCRRestAuthorizationFilter;
import org.mycore.restapi.v2.MCRRestUtils;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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

@MCRApiDraft("MCRAccessKey")
@Path("/objects/{" + MCRRestAuthorizationFilter.PARAM_MCRID + "}/accesskeys")
@Tag(name = MCRRestUtils.TAG_MYCORE_OBJECT)
public class MCRRestObjectAccessKeys {

    @Context
    private UriInfo uriInfo;

    @Operation(summary = "Lists all access keys for an object",
        responses = {
            @ApiResponse(responseCode = "200", description = "Information about a access keys",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MCRAccessKeyInformation.class))),
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
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    public MCRAccessKeyInformation listAccessKeysForObject(
        @PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("128") @QueryParam("limit") int limit) {
        return MCRRestAccessKeyHelper.doListAccessKeys(objectId, offset, limit);
    }

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
    @Path("/{" + MCRRestAccessKeyHelper.PARAM_SECRET + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    public MCRRestAccessKeyDto getAccessKeyFromObject(
        @PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRRestAccessKeyHelper.PARAM_SECRET) String secret,
        @QueryParam(MCRRestAccessKeyHelper.QUERY_PARAM_SECRET_ENCODING) String secretEncoding) {
        return MCRRestAccessKeyHelper.doGetAccessKey(objectId, secret, secretEncoding);
    }

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
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    @MCRRequireTransaction
    public Response createAccessKeyForObject(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        MCRRestAccessKeyDto restAccessKeyDto) {
        return MCRRestAccessKeyHelper.doCreateAccessKey(objectId, restAccessKeyDto, uriInfo);
    }

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
    @Path("/{" + MCRRestAccessKeyHelper.PARAM_SECRET + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    @MCRRequireTransaction
    public Response updateAccessKeyFromObject(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRRestAccessKeyHelper.PARAM_SECRET) String secret, MCRRestAccessKeyDto restAccessKeyDto,
        @QueryParam(MCRRestAccessKeyHelper.QUERY_PARAM_SECRET_ENCODING) String secretEncoding) {
        return MCRRestAccessKeyHelper.doUpdateAccessKey(objectId, secret, restAccessKeyDto, secretEncoding);
    }

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
    @Path("/{" + MCRRestAccessKeyHelper.PARAM_SECRET + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    @MCRRequireTransaction
    public Response removeAccessKeyFromObject(@PathParam(MCRRestAuthorizationFilter.PARAM_MCRID) MCRObjectID objectId,
        @PathParam(MCRRestAccessKeyHelper.PARAM_SECRET) String secret,
        @QueryParam(MCRRestAccessKeyHelper.QUERY_PARAM_SECRET_ENCODING) String secretEncoding) {
        return MCRRestAccessKeyHelper.doRemoveAccessKey(objectId, secret, secretEncoding);
    }
}
