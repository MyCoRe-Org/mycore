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

import static org.mycore.mcr.acl.accesskey.restapi.v2.MCRRestAccessKeyHelper.PARAM_SECRET;
import static org.mycore.mcr.acl.accesskey.restapi.v2.MCRRestAccessKeyHelper.QUERY_PARAM_SECRET_ENCODING;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;
import static org.mycore.restapi.v2.MCRRestStatusCode.BAD_REQUEST;
import static org.mycore.restapi.v2.MCRRestStatusCode.CREATED;
import static org.mycore.restapi.v2.MCRRestStatusCode.NOT_FOUND;
import static org.mycore.restapi.v2.MCRRestStatusCode.NO_CONTENT;
import static org.mycore.restapi.v2.MCRRestStatusCode.OK;
import static org.mycore.restapi.v2.MCRRestStatusCode.UNAUTHORIZED;
import static org.mycore.restapi.v2.MCRRestUtils.TAG_MYCORE_OBJECT;

import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.mycore.restapi.v2.MCRRestDescription;
import org.mycore.restapi.v2.MCRRestSchemaType;
import org.mycore.restapi.v2.annotation.MCRRestRequiredPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@Path("/objects/{" + PARAM_MCRID + "}/accesskeys")
@Tag(name = TAG_MYCORE_OBJECT)
public class MCRRestObjectAccessKeys {

    private static final String OBJECT_NOT_FOUND = "Object or access key does not exist";

    @Context
    UriInfo uriInfo;

    @GET
    @Operation(
        summary = "Lists all access keys for an object",
        responses = {
            @ApiResponse(responseCode = OK,
                description = "List of access keys attached to this metadata object",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = MCRAccessKey.class))) }),
            @ApiResponse(responseCode = BAD_REQUEST,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = MCRRestDescription.UNAUTHORIZED,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = NOT_FOUND,
                description = OBJECT_NOT_FOUND,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    public Response listAccessKeysForObject(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        @DefaultValue("0") @QueryParam("offset") final int offset,
        @DefaultValue("128") @QueryParam("limit") final int limit) {
        return MCRRestAccessKeyHelper.doListAccessKeys(objectId, offset, limit);
    }

    @GET
    @Path("/{" + PARAM_SECRET + "}")
    @Operation(
        summary = "Gets access key for an object",
        responses = {
            @ApiResponse(responseCode = OK,
                description = "Information about a specific access key",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MCRAccessKey.class))),
            @ApiResponse(responseCode = BAD_REQUEST,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = MCRRestDescription.UNAUTHORIZED,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = NOT_FOUND,
                description = OBJECT_NOT_FOUND,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    public Response getAccessKeyFromObject(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        @PathParam(PARAM_SECRET) final String secret,
        @QueryParam(QUERY_PARAM_SECRET_ENCODING) final String secretEncoding) {
        return MCRRestAccessKeyHelper.doGetAccessKey(objectId, secret, secretEncoding);
    }

    @POST
    @Operation(
        summary = "Creates an access key for an object",
        responses = {
            @ApiResponse(responseCode = CREATED,
                description = "Access key was successfully created",
                headers = @Header(name = HttpHeaders.LOCATION,
                    schema = @Schema(type = MCRRestSchemaType.STRING, format = "uri"),
                    description = "Location of the new access keyO")),
            @ApiResponse(responseCode = BAD_REQUEST,
                description = MCRRestDescription.BAD_REQUEST,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = MCRRestDescription.UNAUTHORIZED,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = NOT_FOUND,
                description = OBJECT_NOT_FOUND,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKey.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    @MCRRequireTransaction
    public Response createAccessKeyForObject(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        final String accessKeyJson) {
        return MCRRestAccessKeyHelper.doCreateAccessKey(objectId, accessKeyJson, uriInfo);
    }

    @PUT
    @Path("/{" + PARAM_SECRET + "}")
    @Operation(
        summary = "Updates an access key for an object",
        responses = {
            @ApiResponse(responseCode = NO_CONTENT,
                description = "Access key was successfully updated"),
            @ApiResponse(responseCode = BAD_REQUEST,
                description = MCRRestDescription.BAD_REQUEST,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = MCRRestDescription.UNAUTHORIZED,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = NOT_FOUND,
                description = OBJECT_NOT_FOUND,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKey.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    @MCRRequireTransaction
    public Response updateAccessKeyFromObject(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        @PathParam(PARAM_SECRET) final String secret, final String accessKeyJson,
        @QueryParam(QUERY_PARAM_SECRET_ENCODING) final String secretEncoding) {
        return MCRRestAccessKeyHelper.doUpdateAccessKey(objectId, secret, accessKeyJson, secretEncoding);
    }

    @DELETE
    @Path("/{" + PARAM_SECRET + "}")
    @Operation(
        summary = "Deletes an access key from an object",
        responses = {
            @ApiResponse(responseCode = NO_CONTENT,
                description = "Access key was successfully deleted"),
            @ApiResponse(responseCode = BAD_REQUEST,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = MCRRestDescription.UNAUTHORIZED,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = NOT_FOUND,
                description = NOT_FOUND,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRAccessManager.PERMISSION_WRITE)
    @MCRRequireTransaction
    public Response removeAccessKeyFromObject(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        @PathParam(PARAM_SECRET) final String secret,
        @QueryParam(QUERY_PARAM_SECRET_ENCODING) final String secretEncoding) {
        return MCRRestAccessKeyHelper.doRemoveAccessKey(objectId, secret, secretEncoding);
    }

}
