/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey.restapi.v2;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTransformer;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.restapi.v2.annotation.MCRRequireAccessKeyAuthorization;
import org.mycore.mcr.acl.accesskey.restapi.v2.model.MCRAccessKeyInformation;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.mycore.restapi.v2.MCRErrorResponse;

@MCRApiDraft("MCRAccessKey")
@Path("/objects/{" + PARAM_MCRID + "}/accesskeys")
public class MCRRestAccessKey {

    private static final String VALUE = "value";

    @Context
    UriInfo uriInfo;

    @GET
    @Operation(
        summary = "Lists all access keys for the given object",
        responses = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(implementation = MCRAccessKey.class)))}),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    public Response getAccessKeys(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        @DefaultValue("0") @QueryParam("offset") int offset,
        @DefaultValue("128") @QueryParam("limit") int limit) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException();
        }
        final List<MCRAccessKey> accessKeys = MCRAccessKeyManager.getAccessKeys(objectId);
        final List<MCRAccessKey> accessKeysResult = accessKeys.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
        return Response.ok(new MCRAccessKeyInformation(accessKeysResult, accessKeys.size())).build();
    }

    @GET
    @Path("/{" + VALUE + "}")
    @Operation(
        summary = "Get access key for the given object with id",
        responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MCRAccessKey.class))),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    public Response getAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam("VALUE") final String value) {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException();
        }
        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyByValue(objectId, value);
        if (accessKey != null) {
            return Response.ok(accessKey).build();
        }
        throw new MCRAccessKeyNotFoundException("Key does not exists.");
    }

    @POST
    @Operation(
        summary = "Create MCRAccessKey",
        responses = {
            @ApiResponse(responseCode = "201", description = "MCRAccessKey successfully created",
                headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "400", description = "Invalid type or invalid value",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKey.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response addAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, final String accessKeyJson) {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException();
        }
        accessKey.setObjectId(objectId);
        accessKey.setCreator(null); //to prevent the client from setting, this is done by the KeyManager
        accessKey.setCreation(null);
        accessKey.setLastChanger(null);
        accessKey.setLastChange(null);
        MCRAccessKeyManager.addAccessKey(accessKey);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(accessKey.getValue()).build()).build();
    }
    
    @PUT
    @Path("/{" + VALUE + "}")
    @Operation(
        summary = "Update MCRAccessKey",
        responses = {
            @ApiResponse(responseCode = "204", description = "MCRAccessKey successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request. Check type, values.",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "ObjectID or MCRAccessKey doesn't exists",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = MCRAccessKey.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response updateAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam(VALUE) final String value, final String accessKeyJson) throws IOException {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException();
        }
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        accessKey.setObjectId(objectId);
        accessKey.setValue(value);
        MCRAccessKeyManager.updateAccessKey(accessKey);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{" + VALUE + "}")
    @Operation(
        summary = "Deletes MCRAccessKey",
        responses = {
            @ApiResponse(responseCode = "204", description = "MCRAccessKey successfully deleted"),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID,
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "401",
                description = "You do not have create permission and need to authenticate first",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
            @ApiResponse(responseCode = "404", description = "ObjectID or MCRAccessKey doesn't exists",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON) }),
        })
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response deleteAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam(VALUE) final String value) throws IOException {
        if (!MCRMetadataManager.exists(objectId)) {
            throw getUnknownObjectException();
        }
        MCRAccessKeyManager.deleteAccessKey(objectId, value);
        return Response.noContent().build();
    }

    private WebApplicationException getUnknownObjectException() {
        return MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withMessage("Object doesn't exists!")
            .withErrorCode("objectNotFound")
            .toException();
    }

}
