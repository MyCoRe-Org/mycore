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

import static org.mycore.mcr.acl.accesskey.restapi.v2.MCRRestAccessKeyHelper.PARAM_SECRET;
import static org.mycore.mcr.acl.accesskey.restapi.v2.MCRRestAccessKeyHelper.QUERY_PARAM_SECRET_ENCODING;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;
import static org.mycore.restapi.v2.MCRRestUtils.TAG_MYCORE_DERIVATE;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.mycore.restapi.v2.access.MCRRestAPIACLPermission;
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
@Path("/objects/{" + PARAM_MCRID + "}/derivates/{" + PARAM_DERID + "}/accesskeys")
@Tag(name = TAG_MYCORE_DERIVATE)
public class MCRRestDerivateAccessKeys {

    @Context
    UriInfo uriInfo;

    @GET
    @Operation(
        summary = "Lists all access keys for a derivate",
        responses = {
            @ApiResponse(responseCode = "200", content = { @Content(mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(implementation = MCRAccessKey.class))) }),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID, // 400
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Derivate or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRRestAPIACLPermission.WRITE)
    public Response listAccessKeysForDerivate(@PathParam(PARAM_DERID) final MCRObjectID derivateId,
        @DefaultValue("0") @QueryParam("offset") final int offset,
        @DefaultValue("128") @QueryParam("limit") final int limit) {
        return MCRRestAccessKeyHelper.doListAccessKeys(derivateId, offset, limit);
    }

    @GET
    @Path("/{" + PARAM_SECRET + "}")
    @Operation(
        summary = "Gets access key for a derivate",
        responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRAccessKey.class))),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID, // 400
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Derivate or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRRestAPIACLPermission.WRITE)
    public Response getAccessKeyFromDerivate(@PathParam(PARAM_DERID) final MCRObjectID derivateId,
        @PathParam(PARAM_SECRET) final String secret,
        @QueryParam(QUERY_PARAM_SECRET_ENCODING) final String secretEncoding) {
        return MCRRestAccessKeyHelper.doGetAccessKey(derivateId, secret, secretEncoding);
    }

    @POST
    @Operation(
        summary = "Creates an access key for a derivate",
        responses = {
            @ApiResponse(responseCode = "201", description = "Access key was successfully created",
                headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "400", description = "Invalid ID or invalid access key",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Derivate does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKey.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRRestAPIACLPermission.WRITE)
    @MCRRequireTransaction
    public Response createAccessKeyForDerivate(@PathParam(PARAM_DERID) final MCRObjectID derivateId,
        final String accessKeyJson) {
        return MCRRestAccessKeyHelper.doCreateAccessKey(derivateId, accessKeyJson, uriInfo);
    }

    @PUT
    @Path("/{" + PARAM_SECRET + "}")
    @Operation(
        summary = "Updates an access key for a derivate",
        responses = {
            @ApiResponse(responseCode = "204", description = "Access key was successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or invalid access key",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Derivate or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKey.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRRestAPIACLPermission.WRITE)
    @MCRRequireTransaction
    public Response updateAccessKeyFromDerivate(@PathParam(PARAM_DERID) final MCRObjectID derivateId,
        @PathParam(PARAM_SECRET) final String encodedSecret, final String accessKeyJson,
        @QueryParam(QUERY_PARAM_SECRET_ENCODING) final String secretEncoding) {
        return MCRRestAccessKeyHelper.doUpdateAccessKey(derivateId, encodedSecret, accessKeyJson, secretEncoding);
    }

    @DELETE
    @Path("/{" + PARAM_SECRET + "}")
    @Operation(
        summary = "Deletes an access key from a derivate",
        responses = {
            @ApiResponse(responseCode = "204", description = "Access key was successfully deleted"),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID, // 400
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "Derivate or access key does not exist",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestRequiredPermission(MCRRestAPIACLPermission.WRITE)
    @MCRRequireTransaction
    public Response removeAccessKeyFromDerivate(@PathParam(PARAM_DERID) final MCRObjectID derivateId,
        @PathParam(PARAM_SECRET) final String secret,
        @QueryParam(QUERY_PARAM_SECRET_ENCODING) final String secretEncoding) {
        return MCRRestAccessKeyHelper.doRemoveAccessKey(derivateId, secret, secretEncoding);
    }
}
