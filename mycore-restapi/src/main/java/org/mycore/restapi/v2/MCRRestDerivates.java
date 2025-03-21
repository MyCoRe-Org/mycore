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

package org.mycore.restapi.v2;

import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_JSON_UTF_8;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;
import static org.mycore.restapi.v2.MCRRestStatusCode.ACCEPTED;
import static org.mycore.restapi.v2.MCRRestStatusCode.BAD_REQUEST;
import static org.mycore.restapi.v2.MCRRestStatusCode.CREATED;
import static org.mycore.restapi.v2.MCRRestStatusCode.FORBIDDEN;
import static org.mycore.restapi.v2.MCRRestStatusCode.NO_CONTENT;
import static org.mycore.restapi.v2.MCRRestStatusCode.UNAUTHORIZED;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRContentAbstractWriter;
import org.mycore.restapi.converter.MCRObjectIDParamConverterProvider;

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

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlElementWrapper;

@Path("/objects/{" + PARAM_MCRID + "}/derivates")
public class MCRRestDerivates {

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    Request request;

    @Context
    UriInfo uriInfo;

    @Parameter(example = "mir_mods_00004711")
    @PathParam(PARAM_MCRID)
    MCRObjectID mcrId;

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static void validateDerivateRelation(MCRObjectID mcrId, MCRObjectID derId) {
        MCRObjectID objectId = MCRMetadataManager.getObjectId(derId, 1, TimeUnit.DAYS);
        if (objectId != null && !mcrId.equals(objectId)) {
            objectId = MCRMetadataManager.getObjectId(derId, 0, TimeUnit.SECONDS);
        }
        if (mcrId.equals(objectId)) {
            return;
        }
        if (objectId == null) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_FOUND)
                .withMessage("MCRDerivate " + derId + " not found")
                .toException();
        }
        throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
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
                description = "List of derivates (file collections) attached to the given metadata object",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRMetaLinkID.class)))),
            @ApiResponse(responseCode = BAD_REQUEST, description = MCRObjectIDParamConverterProvider.MSG_INVALID),

        },
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @XmlElementWrapper(name = MCRObjectStructure.ELEMENT_DERIVATE_OBJECTS)
    public Response listDerivates()
        throws IOException {
        long modified = MCRXMLMetadataManager.getInstance().getLastModified(mcrId);
        if (modified < 0) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
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
            .entity(new GenericEntity<>(derivates) {
            })
            .lastModified(lastModified)
            .build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, APPLICATION_JSON_UTF_8 })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(
        summary = "Returns given derivate in the given object",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @Path("/{" + PARAM_DERID + "}")
    public Response getDerivate(@Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid)
        throws IOException {
        validateDerivateRelation(mcrId, derid);
        long modified = MCRXMLMetadataManager.getInstance().getLastModified(derid);
        Date lastModified = new Date(modified);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRContent mcrContent = MCRXMLMetadataManager.getInstance().retrieveContent(derid);
        return Response.ok()
            .entity(mcrContent,
                new Annotation[] { MCRParams.Factory
                    .get(MCRParam.Factory.get(MCRContentAbstractWriter.PARAM_OBJECTTYPE, derid.getTypeId())) })
            .lastModified(lastModified)
            .build();
    }

    @PUT
    @Consumes({ MediaType.APPLICATION_XML, APPLICATION_JSON_UTF_8 })
    @Operation(summary = "Creates or updates MCRDerivate with the body of this request",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = BAD_REQUEST,
                content = { @Content(mediaType = MediaType.TEXT_PLAIN) },
                description = "'Invalid body content' or 'MCRObjectID mismatch'"),
            @ApiResponse(responseCode = CREATED, description = "MCRDerivate successfully created"),
            @ApiResponse(responseCode = NO_CONTENT, description = "MCRDerivate successfully updated"),
        })
    @MCRRequireTransaction
    @Path("/{" + PARAM_DERID + "}")
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response updateDerivate(
        @Parameter(example = "mir_derivate_00004712") @PathParam(PARAM_DERID) MCRObjectID derid,
        @Parameter(required = true,
            description = "MCRObject XML",
            examples = @ExampleObject("<mycoreobject ID=\"{mcrid}\" ..>\n...\n</mycorobject>")) InputStream xmlSource)
        throws IOException {
        //check preconditions
        Response cachedResponse = MCRRestUtils.getCachedResponseIgnoreExceptions(request, derid);
        if (cachedResponse != null) {
            return cachedResponse;
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
        } catch (JDOMException | MCRException e) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_INVALID)
                .withMessage("MCRDerivate " + derid + " is not valid")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        if (!derid.equals(derivate.getId())) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
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
            throw MCRErrorResponse.ofStatusCode(Response.Status.FORBIDDEN.getStatusCode())
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
            @ApiResponse(responseCode = NO_CONTENT, description = "MCRDerivate successfully deleted"),
        })
    @MCRRequireTransaction
    @Path("/{" + PARAM_DERID + "}")
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response deleteDerivate(
        @Parameter(example = "mir_derivate_00004713") @PathParam(PARAM_DERID) MCRObjectID derid) {
        if (!MCRMetadataManager.exists(derid)) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_FOUND)
                .withMessage("MCRDerivate " + derid + " not found")
                .toException();
        }
        try {
            MCRMetadataManager.deleteMCRDerivate(derid);
            return Response.noContent().build();
        } catch (MCRAccessException e) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.FORBIDDEN.getStatusCode())
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
        responses = @ApiResponse(responseCode = CREATED,
            headers = @Header(name = HttpHeaders.LOCATION,
                schema = @Schema(
                    type = "string",
                    format = "uri"),
                description = "URL of the new derivate")),
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response createDefaultDerivate() {
        return doCreateDerivate(new DerivateMetadata());
    }

    @POST
    @Operation(
        summary = "Adds a new derivate in the given object",
        responses = @ApiResponse(responseCode = CREATED,
            description = "Derivate successfully created",
            headers = @Header(name = HttpHeaders.LOCATION,
                schema = @Schema(type = "string", format = "uri"),
                description = "URL of the new derivate")),
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
        MCRObjectID zeroId = MCRObjectID.getInstance(MCRObjectID.formatID(projectID + "_derivate", 0));
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(zeroId);

        derivate.setOrder(der.getOrder());

        derivate.getDerivate().getClassifications()
            .addAll(der.getClassifications().stream()
                .map(categId -> new MCRMetaClassification("classification", 0, null, categId))
                .toList());

        derivate.getDerivate().getTitles()
            .addAll(der.getTitles().stream()
                .map(DerivateTitle::toMetaLangText)
                .toList());

        String schema = MCRConfiguration2.getString("MCR.Metadata.Config.derivate")
            .orElse("datamodel-derivate.xml")
            .replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag(MCRObjectDerivate.ELEMENT_LINKMETA);
        linkId.setReference(mcrId, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag(MCRObjectDerivate.ELEMENT_INTERNAL);
        ifs.setSourcePath(null);
        ifs.setMainDoc(der.getMainDoc());
        derivate.getDerivate().setInternals(ifs);

        try {
            MCRMetadataManager.create(derivate);
        } catch (MCRAccessException e) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NO_PERMISSION)
                .withMessage("You may not create MCRDerivate for project " + zeroId.getProjectId() + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }

        MCRObjectID derId = derivate.getId();
        LOGGER.debug("Created new derivate with ID {}", derId);
        MCRPath rootDir = MCRPath.getPath(derId.toString(), "/");
        if (Files.notExists(rootDir)) {
            try {
                rootDir.getFileSystem().createRoot(derId.toString());
            } catch (FileSystemException e) {
                throw MCRErrorResponse.ofStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_CREATE_DIRECTORY)
                    .withMessage("Could not create root directory for MCRDerivate " + derId + ".")
                    .withDetail(e.getMessage())
                    .withCause(e)
                    .toException();
            }
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(derId.toString()).build()).build();
    }

    @PATCH
    @Operation(
        summary = "Updates the metadata (or partial metadata) of the given derivate",
        responses = @ApiResponse(responseCode = NO_CONTENT),
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED,
            schema = @Schema(implementation = DerivateMetadata.class)))
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    @Path("/{" + PARAM_DERID + "}")
    @MCRApiDraft("MCRPatchDerivate")
    public Response patchDerivate(@BeanParam DerivateMetadata der,
        @Parameter(example = "mir_derivate_00004714") @PathParam(PARAM_DERID) MCRObjectID derid) {

        LOGGER.debug(der);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derid);
        boolean modified = updateIfModified(der, derivate);
        if (modified) {
            try {
                MCRMetadataManager.update(derivate);
            } catch (MCRAccessException e) {
                throw MCRErrorResponse.ofStatusCode(Response.Status.FORBIDDEN.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NO_PERMISSION)
                    .withMessage("You may not update MCRDerivate " + derivate.getId() + ".")
                    .withDetail(e.getMessage())
                    .withCause(e)
                    .toException();
            }
        }

        return Response.noContent().build();
    }

    private static boolean updateIfModified(DerivateMetadata der, MCRDerivate derivate) {
        boolean modified = false;

        // Check if the 'order' field has been updated
        if (der.getOrder() != -1 && derivate.getOrder() != der.getOrder()) {
            modified = true;
            derivate.setOrder(der.getOrder());
        }

        // Check if the 'mainDoc' field has been updated
        if (der.getMainDoc() != null && !der.getMainDoc().equals(derivate.getDerivate().getInternals().getMainDoc())) {
            modified = true;
            derivate.getDerivate().getInternals().setMainDoc(der.getMainDoc());
        }

        // Check if the 'classifications' field has been updated
        List<MCRCategoryID> oldClassifications = derivate.getDerivate().getClassifications().stream()
            .map(x -> MCRCategoryID.ofString(x.getClassId() + ":" + x.getCategId()))
            .toList();
        if (!der.getClassifications().isEmpty()
            && (oldClassifications.size() != der.getClassifications().size()
                || !oldClassifications.containsAll(der.getClassifications()))) {
            modified = true;
            derivate.getDerivate().getClassifications().clear();
            derivate.getDerivate().getClassifications()
                .addAll(der.getClassifications().stream()
                    .map(categId -> new MCRMetaClassification("classification", 0, null, categId))
                    .toList());
        }

        // Check if the 'titles' field has been updated
        List<MCRMetaLangText> newTitles = der.getTitles().stream()
            .map(DerivateTitle::toMetaLangText)
            .toList();
        if (!newTitles.isEmpty()
            && (derivate.getDerivate().getTitleSize() != newTitles.size()
                || !derivate.getDerivate().getTitles().containsAll(newTitles))) {
            modified = true;
            derivate.getDerivate().getTitles().clear();
            derivate.getDerivate().getTitles().addAll(newTitles);
        }
        return modified;
    }

    @PUT
    @Path("/{" + PARAM_DERID + "}/try")
    @Operation(summary = "pre-flight target to test write operation on {" + PARAM_DERID + "}",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = ACCEPTED, description = "You have write permission"),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = "You do not have write permission and need to authenticate first"),
            @ApiResponse(responseCode = FORBIDDEN, description = "You do not have write permission"),
        })
    public Response testUpdateDerivate(@PathParam(PARAM_DERID) MCRObjectID id) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @DELETE
    @Path("/{" + PARAM_DERID + "}/try")
    @Operation(summary = "pre-flight target to test delete operation on {" + PARAM_DERID + "}",
        tags = MCRRestUtils.TAG_MYCORE_DERIVATE,
        responses = {
            @ApiResponse(responseCode = ACCEPTED, description = "You have delete permission"),
            @ApiResponse(responseCode = UNAUTHORIZED,
                description = "You do not have delete permission and need to authenticate first"),
            @ApiResponse(responseCode = FORBIDDEN, description = "You do not have delete permission"),
        })
    public Response testDeleteDerivate(@PathParam(PARAM_DERID) MCRObjectID id) {
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
        private String mainDoc;

        private int order = 1;

        private List<MCRCategoryID> classifications = List.of();

        private List<DerivateTitle> titles = List.of();

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
