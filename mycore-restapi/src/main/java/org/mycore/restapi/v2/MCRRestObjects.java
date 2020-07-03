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

import static org.mycore.common.MCRConstants.XSI_NAMESPACE;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.media.services.MCRThumbnailGenerator;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRContentAbstractWriter;
import org.mycore.restapi.v2.model.MCRRestObjectIDDate;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/objects")
@OpenAPIDefinition(tags = {
    @Tag(name = MCRRestUtils.TAG_MYCORE_OBJECT, description = "Operations on metadata objects"),
    @Tag(name = MCRRestUtils.TAG_MYCORE_DERIVATE,
        description = "Operations on derivates belonging to metadata objects"),
    @Tag(name = MCRRestUtils.TAG_MYCORE_FILE, description = "Operations on files in derivates"),
})
public class MCRRestObjects {

    private static Logger LOGGER = LogManager.getLogger();

    @Context
    Request request;

    @Context
    ServletContext context;

    @Context
    UriInfo uriInfo;

    public static final List<MCRThumbnailGenerator> THUMBNAIL_GENERATORS = Collections
        .unmodifiableList(MCRConfiguration2
            .getOrThrow("MCR.Media.Thumbnail.Generators", MCRConfiguration2::splitValue)
            .map(MCRConfiguration2::instantiateClass)
            .map(MCRThumbnailGenerator.class::cast)
            .collect(Collectors.toList()));

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    @Operation(
        summary = "Lists all objects in this repository",
        responses = @ApiResponse(
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRObjectIDDate.class)))),
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    @XmlElementWrapper(name = "mycoreobjects")
    @JacksonFeatures(serializationDisable = { SerializationFeature.WRITE_DATES_AS_TIMESTAMPS })
    public Response listObjects() throws IOException {
        Date lastModified = new Date(MCRXMLMetadataManager.instance().getLastModified());
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        List<MCRRestObjectIDDate> idDates = MCRXMLMetadataManager.instance().listObjectDates().stream()
            .filter(oid -> !oid.getId().contains("_derivate_"))
            .map(x -> new MCRRestObjectIDDate(x))
            .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<MCRRestObjectIDDate>>(idDates) {
        })
            .lastModified(lastModified)
            .build();
    }

    @POST
    @Operation(
        summary = "Create a new MyCoRe Object",
        responses = @ApiResponse(responseCode = "201",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL of the new MyCoRe Object")),
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    @Consumes(MediaType.APPLICATION_XML)
    @RequestBody(required = true,
        content = @Content(mediaType = MediaType.APPLICATION_XML))
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response createObject(String xml) {
        try {
            Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRStringContent(xml));
            Element eMCRObj = doc.getRootElement();
            if (eMCRObj.getAttributeValue("ID") != null) {
                MCRObjectID id = MCRObjectID.getInstance(eMCRObj.getAttributeValue("ID"));
                MCRObjectID newID = MCRObjectID.getNextFreeId(id.getBase());
                eMCRObj.setAttribute("ID", newID.toString());
                if (eMCRObj.getAttribute("label") == null) {
                    eMCRObj.setAttribute("label", newID.toString());
                }
                if (eMCRObj.getAttribute("version") == null) {
                    eMCRObj.setAttribute("version", MCRCoreVersion.getVersion());
                }
                eMCRObj.setAttribute("noNamespaceSchemaLocation", "datamodel-" + newID.getTypeId() + ".xsd",
                    XSI_NAMESPACE);
            } else {
                //TODO error handling
                throw new BadRequestException("Please provide an object with ID");
            }

            MCRObject mcrObj = new MCRObject(new MCRJDOMContent(doc).asByteArray(), true);
            LOGGER.debug("Create new MyCoRe Object");
            MCRMetadataManager.create(mcrObj);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(mcrObj.getId().toString()).build()).build();
        } catch (MCRPersistenceException | SAXParseException | IOException e) {
            throw new InternalServerErrorException(e);
        } catch (MCRAccessException e) {
            throw new ForbiddenException(e);
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
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

    @GET
    @Produces({ MediaType.APPLICATION_XML })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Path("/{" + PARAM_MCRID + "}/metadata")
    @Operation(
        summary = "Returns metadata section MCRObject with the given " + PARAM_MCRID + ".",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    public Response getObjectMetadata(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID id)
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
        MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(id);
        MCRContent mcrContent = new MCRJDOMContent(mcrObj.getMetadata().createXML());
        return Response.ok()
            .entity(mcrContent,
                new Annotation[] { MCRParams.Factory
                    .get(MCRParam.Factory.get(MCRContentAbstractWriter.PARAM_OBJECTTYPE, id.getTypeId())) })
            .lastModified(lastModified)
            .build();
    }

    @GET
    @Path("{" + PARAM_MCRID + "}/thumb-{size}.{ext : (jpg|jpeg|png)}")
    @Produces({ "image/png", "image/jpeg" })
    @Operation(
        summary = "Returns thumbnail of MCRObject with the given " + PARAM_MCRID + ".",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getThumbnailWithSize(@PathParam(PARAM_MCRID) String id, @PathParam("size") int size,
        @PathParam("ext") String ext) {
        return getThumbnail(id, size, ext);
    }

    @GET
    @Path("{" + PARAM_MCRID + "}/thumb.{ext : (jpg|jpeg|png)}")
    @Produces({ "image/png", "image/jpeg" })
    @Operation(
        summary = "Returns thumbnail of MCRObject with the given " + PARAM_MCRID + ".",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getThumbnail(@PathParam(PARAM_MCRID) String id, @PathParam("ext") String ext) {
        int defaultSize = MCRConfiguration2.getOrThrow("MCR.Media.Thumbnail.DefaultSize", Integer::parseInt);
        return getThumbnail(id, defaultSize, ext);
    }

    private Response getThumbnail(String id, int size, String ext) {
        List<MCRPath> mainDocs = MCRMetadataManager.getDerivateIds(MCRObjectID.getInstance(id), 1, TimeUnit.MINUTES)
            .stream()
            .filter(d -> MCRAccessManager.checkDerivateContentPermission(d, MCRAccessManager.PERMISSION_READ))
            .map(d -> {
                String nameOfMainFile = MCRMetadataManager.retrieveMCRDerivate(d).getDerivate().getInternals()
                    .getMainDoc();
                return nameOfMainFile.isEmpty() ? null : MCRPath.getPath(d.toString(), '/' + nameOfMainFile);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (mainDocs.isEmpty()) {
            throw new NotFoundException();
        }

        for (MCRPath mainDoc : mainDocs) {
            Optional<MCRThumbnailGenerator> thumbnailGenerator = THUMBNAIL_GENERATORS.stream()
                .filter(g -> g.matchesFileType(context.getMimeType(mainDoc.getFileName().toString()), mainDoc))
                .findFirst();
            if (thumbnailGenerator.isPresent()) {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(mainDoc);
                    Date lastModifiedDate = new Date(lastModified.toMillis());
                    Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModifiedDate);
                    return cachedResponse.orElseGet(() -> {
                        Optional<BufferedImage> thumbnail = null;
                        try {
                            thumbnail = thumbnailGenerator.get().getThumbnail(mainDoc, size);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return thumbnail.map(b -> {
                            String type = "image/png";
                            if ("jpg".equals(ext) || "jpeg".equals(ext)) {
                                type = "image/jpeg";
                            }
                            return Response.ok(b)
                                .lastModified(lastModifiedDate)
                                .type(type)
                                .build();
                        }).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
                    });
                } catch (FileNotFoundException e) {
                    continue; //try another mainDoc if present
                } catch (IOException e) {
                    throw new InternalServerErrorException(e);
                } catch (UncheckedIOException e) {
                    throw new InternalServerErrorException(e.getCause());
                }
            }
        }

        throw new NotFoundException();
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Path("/{" + PARAM_MCRID + "}/versions")
    @Operation(
        summary = "Returns MCRObject with the given " + PARAM_MCRID + ".",
        responses = @ApiResponse(content = @Content(
            array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = MCRMetadataVersion.class)))),
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    @JacksonFeatures(serializationDisable = { SerializationFeature.WRITE_DATES_AS_TIMESTAMPS })
    @XmlElementWrapper(name = "versions")
    public Response getObjectVersions(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID id)
        throws IOException {
        long modified = MCRXMLMetadataManager.instance().getLastModified(id);
        if (modified < 0) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
                .withMessage("MCRObject " + id + " not found")
                .toException();
        }
        Date lastModified = new Date(modified);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        List<MCRMetadataVersion> versions = MCRXMLMetadataManager.instance().getVersionedMetaData(id)
            .listVersions();
        return Response.ok()
            .entity(new GenericEntity<>(versions, TypeUtils.parameterize(List.class, MCRMetadataVersion.class)))
            .lastModified(lastModified)
            .build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1000, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1000, unit = TimeUnit.DAYS)) //will never expire actually
    @Path("/{" + PARAM_MCRID + "}/versions/{revision}")
    @Operation(
        summary = "Returns MCRObject with the given " + PARAM_MCRID + " and revision.",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    public Response getObjectVersion(@Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID id,
        @PathParam("revision") long revision)
        throws IOException {
        MCRContent mcrContent = MCRXMLMetadataManager.instance().retrieveContent(id, revision);
        if (mcrContent == null) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_REVISION_NOT_FOUND)
                .withMessage("MCRObject " + id + " has no revision " + revision + ".")
                .toException();
        }
        long modified = mcrContent.lastModified();
        Date lastModified = new Date(modified);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        LogManager.getLogger().info("OK: {}", mcrContent.getETag());
        return Response.ok()
            .entity(mcrContent,
                new Annotation[] { MCRParams.Factory
                    .get(MCRParam.Factory.get(MCRContentAbstractWriter.PARAM_OBJECTTYPE, id.getTypeId())) })
            .lastModified(lastModified)
            .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_XML)
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
        //check preconditions
        try {
            long lastModified = MCRXMLMetadataManager.instance().getLastModified(id);
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
        MCRStreamContent inputContent = new MCRStreamContent(xmlSource, null, MCRObject.ROOT_NAME);
        MCRObject updatedObject;
        try {
            updatedObject = new MCRObject(inputContent.asXML());
            updatedObject.validate();
        } catch (JDOMException | SAXException | MCRException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_INVALID)
                .withMessage("MCRObject " + id + " is not valid")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        if (!id.equals(updatedObject.getId())) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_ID_MISMATCH)
                .withMessage("MCRObject " + id + " cannot be overwritten by " + updatedObject.getId() + ".")
                .toException();
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
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NO_PERMISSION)
                .withMessage("You may not modify or create MCRObject " + id + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/{" + PARAM_MCRID + "}/metadata")
    @Operation(summary = "Updates the metadata section of a MCRObject with the body of this request",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "400",
                content = { @Content(mediaType = MediaType.TEXT_PLAIN) },
                description = "'Invalid body content' or 'MCRObjectID mismatch'"),
            @ApiResponse(responseCode = "204", description = "MCRObject metadata successfully updated"),
        })
    @MCRRequireTransaction
    public Response updateObjectMetadata(@PathParam(PARAM_MCRID) MCRObjectID id,
        @Parameter(required = true,
            description = "MCRObject XML",
            examples = @ExampleObject("<metadata>\n...\n</metadata>")) InputStream xmlSource)
        throws IOException {
        //check preconditions
        try {
            long lastModified = MCRXMLMetadataManager.instance().getLastModified(id);
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
        MCRStreamContent inputContent = new MCRStreamContent(xmlSource, null);
        MCRObject updatedObject;
        try {
            updatedObject = MCRMetadataManager.retrieveMCRObject(id);
            updatedObject.getMetadata().setFromDOM(inputContent.asXML().getRootElement().detach());
            updatedObject.validate();
        } catch (JDOMException | SAXException | MCRException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_INVALID)
                .withMessage("MCRObject " + id + " is not valid")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        try {
            MCRMetadataManager.update(updatedObject);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (MCRAccessException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NO_PERMISSION)
                .withMessage("You may not modify or create metadata of MCRObject " + id + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
    }

    @DELETE
    @Path("/{" + PARAM_MCRID + "}")
    @Operation(summary = "Deletes MCRObject {" + PARAM_MCRID + "}",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "204", description = "MCRObject successfully deleted"),
            @ApiResponse(responseCode = "409",
                description = "MCRObject could not be deleted as it is referenced.",
                content = @Content(schema = @Schema(
                    description = "Map<String, <Collection<String>> of source (key) to targets (value)",
                    implementation = Map.class))),
        })
    @MCRRequireTransaction
    public Response deleteObject(@PathParam(PARAM_MCRID) MCRObjectID id) {
        //check preconditions
        if (!MCRMetadataManager.exists(id)) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
                .withMessage("MCRObject " + id + " not found")
                .toException();
        }
        try {
            MCRMetadataManager.deleteMCRObject(id);
        } catch (MCRActiveLinkException e) {
            Map<String, Collection<String>> activeLinks = e.getActiveLinks();
            throw MCRErrorResponse.fromStatus(Response.Status.CONFLICT.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_STILL_LINKED)
                .withMessage("MCRObject " + id + " is still linked by other objects.")
                .withDetail(activeLinks.toString())
                .withCause(e)
                .toException();
        } catch (MCRAccessException e) {
            //usually handled before
            throw MCRErrorResponse.fromStatus(Response.Status.FORBIDDEN.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NO_PERMISSION)
                .withMessage("You may not delete MCRObject " + id + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/{" + PARAM_MCRID + "}/try")
    @Operation(summary = "pre-flight target to test write operation on {" + PARAM_MCRID + "}",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "202", description = "You have write permission"),
            @ApiResponse(responseCode = "401",
                description = "You do not have write permission and need to authenticate first"),
            @ApiResponse(responseCode = "403", description = "You do not have write permission"),
        })
    public Response testUpdateObject(@PathParam(PARAM_MCRID) MCRObjectID id)
        throws IOException {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @DELETE
    @Path("/{" + PARAM_MCRID + "}/try")
    @Operation(summary = "pre-flight target to test delete operation on {" + PARAM_MCRID + "}",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "202", description = "You have delete permission"),
            @ApiResponse(responseCode = "401",
                description = "You do not have delete permission and need to authenticate first"),
            @ApiResponse(responseCode = "403", description = "You do not have delete permission"),
        })
    public Response testDeleteObject(@PathParam(PARAM_MCRID) MCRObjectID id)
        throws IOException {
        return Response.status(Response.Status.ACCEPTED).build();
    }

}
