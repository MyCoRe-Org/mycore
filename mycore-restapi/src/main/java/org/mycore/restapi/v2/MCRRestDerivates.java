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

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
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
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRContentAbstractWriter;
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

@Path("/objects/{" + PARAM_MCRID + "}/derivates")
public class MCRRestDerivates {

    public static final Logger LOGGER = LogManager.getLogger();

    @Context
    Request request;

    @Context
    UriInfo uriInfo;

    @Parameter(example = "mir_mods_00004711")
    @PathParam(PARAM_MCRID)
    MCRObjectID mcrId;

    private static void validateDerivateRelation(MCRObjectID mcrId, MCRObjectID derId) {
        MCRObjectID objectId = MCRMetadataManager.getObjectId(derId, 1, TimeUnit.DAYS);
        if (objectId != null && !mcrId.equals(objectId)) {
            objectId = MCRMetadataManager.getObjectId(derId, 0, TimeUnit.SECONDS);
        }
        if (mcrId.equals(objectId)) {
            return;
        }
        if (objectId == null) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_FOUND)
                .withMessage("MCRDerivate " + derId + " not found")
                .toException();
        }
        throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_FOUND_IN_OBJECT)
            .withMessage("MCRDerivate " + derId + " not found in object " + mcrId + ".")
            .toException();
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
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
    @XmlElementWrapper(name = "derobjects")
    public Response listDerivates()
        throws IOException {
        long modified = MCRXMLMetadataManager.instance().getLastModified(mcrId);
        if (modified < 0) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
                .withMessage("MCRObject " + mcrId + " not found")
                .toException();
        }
        Date lastModified = new Date(modified);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(mcrId);
        List<MCRMetaEnrichedLinkID> derivates = obj.getStructure().getDerivates();
        return Response.ok()
            .entity(new GenericEntity<List<MCRMetaEnrichedLinkID>>(derivates) {
            })
            .lastModified(lastModified)
            .build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(
        summary = "Returns given derivate in the given object",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @Path("/{" + PARAM_DERID + "}")
    public Response getDerivate(@Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid)
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
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
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
    public Response updateDerivate(
        @Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid,
        @Parameter(required = true,
            description = "MCRObject XML",
            examples = @ExampleObject("<mycoreobject ID=\"{mcrid}\" ..>\n...\n</mycorobject>")) InputStream xmlSource)
        throws IOException {
        //check preconditions
        try {
            long lastModified = MCRXMLMetadataManager.instance().getLastModified(derid);
            if (lastModified >= 0) {
                Date lmDate = new Date(lastModified);
                Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lmDate);
                if (cachedResponse.isPresent()) {
                    return cachedResponse.get();
                }
            }
        } catch (Exception e) {
            //ignore errors as PUT is idempotent
        }
        boolean create = true;
        if (MCRMetadataManager.exists(derid)) {
            validateDerivateRelation(mcrId, derid);
            create = false;
        }
        MCRStreamContent inputContent = new MCRStreamContent(xmlSource, null, MCRDerivate.ROOT_NAME);
        MCRDerivate derivate;
        try {
            derivate = new MCRDerivate(inputContent.asXML());
            derivate.validate();
        } catch (JDOMException | SAXException | MCRException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_INVALID)
                .withMessage("MCRDerivate " + derid + " is not valid")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        if (!derid.equals(derivate.getId())) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_ID_MISMATCH)
                .withMessage("MCRDerivate " + derid + " cannot be overwritten by " + derivate.getId() + ".")
                .toException();
        }
        try {
            if (create) {
                MCRMetadataManager.create(derivate);
                MCRPath rootDir = MCRPath.getPath(derid.toString(), "/");
                if (Files.notExists(rootDir)) {
                    rootDir.getFileSystem().createRoot(derid.toString());
                }
                return Response.status(Response.Status.CREATED).build();
            } else {
                MCRMetadataManager.update(derivate);
                return Response.status(Response.Status.NO_CONTENT).build();
            }
        } catch (MCRAccessException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NO_PERMISSION)
                .withMessage("You may not modify or create MCRDerivate " + derid + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
    }

    @DELETE
    @Operation(summary = "Deletes MCRDerivate {" + PARAM_DERID + "}",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = "204", description = "MCRDerivate successfully deleted"),
        })
    @MCRRequireTransaction
    @Path("/{" + PARAM_DERID + "}")
    public Response deleteDerivate(
        @Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid) {
        if (!MCRMetadataManager.exists(derid)) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_FOUND)
                .withMessage("MCRDerivate " + derid + " not found")
                .toException();
        }
        try {
            MCRMetadataManager.deleteMCRDerivate(derid);
            return Response.noContent().build();
        } catch (MCRAccessException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NO_PERMISSION)
                .withMessage("You may not delete MCRDerivate " + derid + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
    }

    @POST
    @Operation(
        summary = "Adds a new derivate (with defaults for 'display-enabled', 'main-doc', 'label') in the given object",
        responses = @ApiResponse(responseCode = "201",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL of the new derivate")),
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response createDefaultDerivate() {
        return doCreateDerivate(new DerivateMetadata());
    }

    @POST
    @Operation(
        summary = "Adds a new derivate in the given object",
        responses = @ApiResponse(responseCode = "201",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL of the new derivate")),
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED,
            schema = @Schema(implementation = DerivateMetadata.class)))
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response createDerivate(@BeanParam DerivateMetadata der) {
        return doCreateDerivate(der);
    }

    private Response doCreateDerivate(@BeanParam DerivateMetadata der) {
        LOGGER.debug(der);
        String projectID = mcrId.getProjectId();
        MCRObjectID derId = MCRObjectID.getNextFreeId(projectID + "_derivate");
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derId);

        derivate.setOrder(der.getOrder());

        derivate.getDerivate().getClassifications()
            .addAll(der.getClassifications().stream()
                .map(categId -> new MCRMetaClassification("classification", 0, null, categId))
                .collect(Collectors.toList()));

        derivate.getDerivate().getTitles()
            .addAll(der.getTitles().stream()
                .map(DerivateTitle::toMetaLangText)
                .collect(Collectors.toList()));

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

        LOGGER.debug("Creating new derivate with ID {}", derId);
        try {
            MCRMetadataManager.create(derivate);
        } catch (MCRAccessException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NO_PERMISSION)
                .withMessage("You may not create MCRDerivate " + derId + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        MCRPath rootDir = MCRPath.getPath(derId.toString(), "/");
        if (Files.notExists(rootDir)) {
            try {
                rootDir.getFileSystem().createRoot(derId.toString());
            } catch (FileSystemException e) {
                throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_CREATE_DIRECTORY)
                    .withMessage("Could not create root directory for MCRDerivate " + derId + ".")
                    .withDetail(e.getMessage())
                    .withCause(e)
                    .toException();
            }
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(derId.toString()).build()).build();
    }

    @PUT
    @Path("/{" + PARAM_DERID + "}/try")
    @Operation(summary = "pre-flight target to test write operation on {" + PARAM_DERID + "}",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = "202", description = "You have write permission"),
            @ApiResponse(responseCode = "401",
                description = "You do not have write permission and need to authenticate first"),
            @ApiResponse(responseCode = "403", description = "You do not have write permission"),
        })
    public Response testUpdateDerivate(@PathParam(PARAM_DERID) MCRObjectID id)
        throws IOException {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @DELETE
    @Path("/{" + PARAM_DERID + "}/try")
    @Operation(summary = "pre-flight target to test delete operation on {" + PARAM_DERID + "}",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = "202", description = "You have delete permission"),
            @ApiResponse(responseCode = "401",
                description = "You do not have delete permission and need to authenticate first"),
            @ApiResponse(responseCode = "403", description = "You do not have delete permission"),
        })
    public Response testDeleteDerivate(@PathParam(PARAM_DERID) MCRObjectID id)
        throws IOException {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    public static class DerivateTitle {
        private String lang;

        private String text;

        //Jersey can use this method without further configuration
        public static DerivateTitle fromString(String value) {
            final DerivateTitle derivateTitle = new DerivateTitle();
            if (value.length() >= 4 && value.charAt(0) == '(') {
                int pos = value.indexOf(')');
                if (pos > 1) {
                    derivateTitle.setLang(value.substring(1, pos));
                    derivateTitle.setText(value.substring(pos + 1));
                    return derivateTitle;
                }
            }
            derivateTitle.setText(value);
            return derivateTitle;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public MCRMetaLangText toMetaLangText() {
            return new MCRMetaLangText("title", getLang(), null, 0, null, getText());
        }
    }

    public static class DerivateMetadata {
        private boolean displayEnabled = true;

        private String mainDoc;

        private int order = 1;

        private List<MCRCategoryID> classifications = List.of();

        private List<DerivateTitle> titles = List.of();

        boolean isDisplayEnabled() {
            return displayEnabled;
        }

        @JsonProperty("display")
        @FormParam("display")
        @DefaultValue("true")
        public void setDisplayEnabled(boolean displayEnabled) {
            this.displayEnabled = displayEnabled;
        }

        String getMainDoc() {
            return mainDoc;
        }

        @FormParam("maindoc")
        @JsonProperty("maindoc")
        public void setMainDoc(String mainDoc) {
            this.mainDoc = mainDoc;
        }

        public int getOrder() {
            return order;
        }

        @JsonProperty
        @FormParam("order")
        @DefaultValue("1")
        public void setOrder(int order) {
            this.order = order;
        }

        public List<MCRCategoryID> getClassifications() {
            return classifications;
        }

        @JsonProperty
        @FormParam("classification")
        public void setClassifications(List<MCRCategoryID> classifications) {
            this.classifications = classifications;
        }

        public List<DerivateTitle> getTitles() {
            return titles;
        }

        @FormParam("title")
        public void setTitles(List<DerivateTitle> titles) {
            this.titles = titles;
        }

    }
}
