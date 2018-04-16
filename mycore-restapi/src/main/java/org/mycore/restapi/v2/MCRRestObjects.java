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

package org.mycore.restapi.v2;

import static org.mycore.restapi.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRContentAbstractWriter;
import org.xml.sax.SAXException;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v2/objects")
@OpenAPIDefinition(tags = {
    @Tag(name = MCRRestUtils.TAG_MYCORE_OBJECT, description = "Operations on metadata objects"),
    @Tag(name = MCRRestUtils.TAG_MYCORE_DERIVATE,
        description = "Operations on derivates belonging to metadata objects"),
    @Tag(name = MCRRestUtils.TAG_MYCORE_FILE, description = "Operations on files in derivates"),
})
public class MCRRestObjects {

    @Context
    Request request;

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    @Operation(
        summary = "Lists all objects in this repository",
        responses = @ApiResponse(
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRObjectIDDate.class)))),
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    public Response listObjects() throws IOException {
        Date lastModified = new Date(MCRXMLMetadataManager.instance().getLastModified());
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        List<MCRObjectIDDate> idDates = MCRXMLMetadataManager.instance().listObjectDates().stream()
            .filter(oid -> !oid.getId().contains("_derivate_"))
            .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<MCRObjectIDDate>>(idDates) {
        })
            .lastModified(lastModified)
            .build();
    }

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Path("/{" + PARAM_MCRID + "}")
    @Operation(
        summary = "Returns MCRObject with the given " + PARAM_MCRID + ".",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    public Response getObject(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID id)
        throws IOException {
        long modified = MCRXMLMetadataManager.instance().getLastModified(id);
        if (modified < 0) {
            throw new NotFoundException("MCRObject " + id + " not found");
        }
        Date lastModified = new Date(modified);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRContent mcrContent = MCRXMLMetadataManager.instance().retrieveContent(id);
        return Response.ok()
            .entity(mcrContent,
                new Annotation[] { MCRParams.Factory
                    .get(MCRParam.Factory.get(MCRContentAbstractWriter.PARAM_OBJECTTYPE, id.getTypeId())) })
            .lastModified(lastModified)
            .build();
    }

    @PUT
    @Consumes(MediaType.TEXT_XML + ";charset=UTF-8")
    @Path("/{" + PARAM_MCRID + "}")
    @Operation(summary = "Creates or updates MCRObject with the body of this request",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "400",
                content = { @Content(mediaType = MediaType.TEXT_PLAIN) },
                description = "'Invalid body content' or 'MCRObjectID mismatch'"),
            @ApiResponse(responseCode = "201", description = "MCRObject successfully created"),
            @ApiResponse(responseCode = "204", description = "MCRObject successfully updated"),
        })
    @MCRRequireTransaction
    public Response updateObject(@PathParam(PARAM_MCRID) MCRObjectID id,
        @Parameter(required = true,
            description = "MCRObject XML",
            examples = @ExampleObject("<mycoreobject ID=\"{mcrid}\" ..>\n...\n</mycorobject>")) InputStream xmlSource)
        throws IOException {
        MCRStreamContent inputContent = new MCRStreamContent(xmlSource, null, MCRObject.ROOT_NAME);
        MCRObject updatedObject = null;
        try {
            updatedObject = new MCRObject(inputContent.asXML());
            updatedObject.validate();
        } catch (JDOMException | SAXException | MCRException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid body content")
                .build(), e);
        }
        if (!id.equals(updatedObject.getId())) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                .entity("MCRObjectID mismatch")
                .build());
        }
        try {
            if (MCRMetadataManager.exists(id)) {
                MCRMetadataManager.update(updatedObject);
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                MCRMetadataManager.create(updatedObject);
                return Response.status(Response.Status.CREATED).build();
            }
        } catch (MCRAccessException e) {
            throw new ForbiddenException(e);
        }
    }

}
