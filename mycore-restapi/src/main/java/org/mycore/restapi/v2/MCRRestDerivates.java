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

import static org.mycore.restapi.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRContentAbstractWriter;
import org.mycore.restapi.converter.MCRMetaDefaultListXMLWriter;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/v2/objects/{" + PARAM_MCRID + "}/derivates")
public class MCRRestDerivates {
    @Context
    Request request;

    @Context
    UriInfo uriInfo;

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(
        summary = "Lists all derivates in the given object",
        responses = {
            @ApiResponse(
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRMetaLinkID.class)))),
            @ApiResponse(responseCode = "" + MCRObjectIDParamConverterProvider.CODE_INVALID,
                description = MCRObjectIDParamConverterProvider.MSG_INVALID),

        },
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    public Response listDerivates(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID id)
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
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(id);
        List<MCRMetaLinkID> derivates = obj.getStructure().getDerivates();
        GenericEntity<List<MCRMetaLinkID>> entity = new GenericEntity<List<MCRMetaLinkID>>(derivates) {
        };
        return Response.ok()
            .entity(entity,
                new Annotation[] { MCRParams.Factory.get(
                    MCRParam.Factory.get(
                        MCRMetaDefaultListXMLWriter.PARAM_XMLWRAPPER, "derobjects"))
                })
            .lastModified(lastModified)
            .build();
    }

    private void validateDerivateRelation(MCRObjectID mcrId, MCRObjectID derId) {
        MCRObjectID objectId = MCRMetadataManager.getObjectId(derId, 1, TimeUnit.DAYS);
        if (objectId != null && !mcrId.equals(objectId)) {
            objectId = MCRMetadataManager.getObjectId(derId, 0, TimeUnit.SECONDS);
        }
        if (mcrId.equals(objectId)) {
            return;
        }
        throw new NotFoundException(objectId == null ? "MCRDerivate " + derId + " not found"
            : "MCRDerivate " + derId + " not found in MCRObject " + mcrId);
    }

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(
        summary = "Returns given derivate in the given object",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @Path("/{" + PARAM_DERID + "}")
    public Response getDerivate(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId,
        @Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid)
        throws IOException {
        validateDerivateRelation(mcrId, derid);
        long modified = MCRXMLMetadataManager.instance().getLastModified(derid);
        Date lastModified = new Date(modified);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRContent mcrContent = MCRXMLMetadataManager.instance().retrieveContent(derid);
        return Response.ok()
            .entity(mcrContent,
                new Annotation[] { MCRParams.Factory
                    .get(MCRParam.Factory.get(MCRContentAbstractWriter.PARAM_OBJECTTYPE, derid.getTypeId())) })
            .lastModified(lastModified)
            .build();
    }

    @PUT
    @Consumes({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Operation(summary = "Creates or updates MCRDerivate with the body of this request",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = "400",
                content = { @Content(mediaType = MediaType.TEXT_PLAIN) },
                description = "'Invalid body content' or 'MCRObjectID mismatch'"),
            @ApiResponse(responseCode = "201", description = "MCRDerivate successfully created"),
            @ApiResponse(responseCode = "204", description = "MCRDerivate successfully updated"),

        })
    @MCRRequireTransaction
    @Path("/{" + PARAM_DERID + "}")
    public Response updateDerivate(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId,
        @Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid,
        @Parameter(required = true,
            description = "MCRObject XML",
            examples = @ExampleObject("<mycoreobject ID=\"{mcrid}\" ..>\n...\n</mycorobject>")) InputStream xmlSource)
        throws IOException {
        boolean create = true;
        if (MCRMetadataManager.exists(derid)) {
            validateDerivateRelation(mcrId, derid);
            create = false;
        }
        MCRStreamContent inputContent = new MCRStreamContent(xmlSource, null, MCRDerivate.ROOT_NAME);
        MCRDerivate derivate = null;
        try {
            derivate = new MCRDerivate(inputContent.asXML());
            derivate.validate();
        } catch (JDOMException | SAXException | MCRException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid body content")
                .build(), e);
        }
        if (!derid.equals(derivate.getId())) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                .entity("MCRObjectID mismatch")
                .build());
        }
        try {
            if (create) {
                MCRMetadataManager.create(derivate);
                return Response.status(Response.Status.CREATED).build();
            } else {
                MCRMetadataManager.update(derivate);
                return Response.status(Response.Status.NO_CONTENT).build();
            }
        } catch (MCRAccessException e) {
            throw new ForbiddenException(e);
        }
    }

    @POST
    @Operation(
        summary = "Adds a new derivate (with defaults for 'display-enabled', 'main-doc', 'label') in the given object",
        responses = @ApiResponse(responseCode = "201",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL of the new derivate")),
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @MCRRequireTransaction
    public Response createDefaultDerivate(
        @Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId) {
        return doCreateDerivate(mcrId, new DerivateMetadata());
    }

    @POST
    @Operation(
        summary = "Adds a new derivate in the given object",
        responses = @ApiResponse(responseCode = "201",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL of the new derivate")),
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
            schema = @Schema(implementation = DerivateMetadata.class)))
    @MCRRequireTransaction
    public Response createDerivate(
        @Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId,
        @BeanParam DerivateMetadata der) {
        return doCreateDerivate(mcrId, der);
    }

    private Response doCreateDerivate(
        @Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId,
        @BeanParam DerivateMetadata der) {
        LogManager.getLogger().info(der);
        String projectID = mcrId.getProjectId();
        MCRObjectID derId = MCRObjectID.getNextFreeId(projectID + "_derivate");
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derId);
        derivate.setLabel(Optional.ofNullable(der.getLabel()).orElse("data object from " + mcrId));
        derivate.getDerivate().setDisplayEnabled(der.isDisplayEnabled());

        String schema = MCRConfiguration2.getString("MCR.Metadata.Config.derivate")
            .orElse("datamodel-derivate.xml")
            .replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(mcrId, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        ifs.setMainDoc(der.getMainDoc());
        derivate.getDerivate().setInternals(ifs);

        LogManager.getLogger().debug("Creating new derivate with ID {}", derId);
        try {
            MCRMetadataManager.create(derivate);
        } catch (MCRAccessException e) {
            throw new ForbiddenException(e);
        }
        MCRPath rootDir = MCRPath.getPath(derId.toString(), "/");
        if (Files.notExists(rootDir)) {
            try {
                rootDir.getFileSystem().createRoot(derId.toString());
            } catch (FileSystemException e) {
                throw new InternalServerErrorException(e);
            }
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(derId.toString()).build()).build();
    }

    public static class DerivateMetadata {
        private String label;

        private boolean displayEnabled = true;

        private String mainDoc;

        public String getLabel() {
            return label;
        }

        @FormDataParam("label")
        public void setLabel(String label) {
            this.label = label;
        }

        public boolean isDisplayEnabled() {
            return displayEnabled;
        }

        @FormDataParam("display-enabled")
        @JsonProperty("display-enabled")
        public void setDisplayEnabled(boolean displayEnabled) {
            this.displayEnabled = displayEnabled;
        }

        public String getMainDoc() {
            return mainDoc;
        }

        @FormDataParam("main-doc")
        @JsonProperty("main-doc")
        public void setMainDoc(String mainDoc) {
            this.mainDoc = mainDoc;
        }

        @Override
        public String toString() {
            return "DerivateMetadata{" +
                "label='" + label + '\'' +
                ", displayEnabled=" + displayEnabled +
                ", mainDoc='" + mainDoc + '\'' +
                '}';
        }
    }
}
