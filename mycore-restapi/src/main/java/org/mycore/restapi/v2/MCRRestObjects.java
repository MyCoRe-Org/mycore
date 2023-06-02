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
import java.net.URI;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.objectinfo.MCRObjectQuery;
import org.mycore.datamodel.objectinfo.MCRObjectQueryResolver;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.media.services.MCRThumbnailGenerator;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.restapi.annotations.MCRParam;
import org.mycore.restapi.annotations.MCRParams;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRContentAbstractWriter;
import org.mycore.restapi.v2.model.MCRRestObjectIDDate;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jakarta.rs.annotation.JacksonFeatures;

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
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlElementWrapper;

@Path("/objects")
@OpenAPIDefinition(tags = {
    @Tag(name = MCRRestUtils.TAG_MYCORE_OBJECT, description = "Operations on metadata objects"),
    @Tag(name = MCRRestUtils.TAG_MYCORE_DERIVATE,
        description = "Operations on derivates belonging to metadata objects"),
    @Tag(name = MCRRestUtils.TAG_MYCORE_FILE, description = "Operations on files in derivates"),
})
public class MCRRestObjects {

    public static final String PARAM_AFTER_ID = "after_id";

    public static final String PARAM_OFFSET = "offset";

    public static final String PARAM_LIMIT = "limit";

    public static final String PARAM_TYPE = "type";

    public static final String PARAM_PROJECT = "project";

    public static final String PARAM_NUMBER_GREATER = "number_greater";

    public static final String PARAM_NUMBER_LESS = "number_less";

    public static final String PARAM_CREATED_AFTER = "created_after";

    public static final String PARAM_CREATED_BEFORE = "created_before";

    public static final String PARAM_MODIFIED_AFTER = "modified_after";

    public static final String PARAM_MODIFIED_BEFORE = "modified_before";

    public static final String PARAM_DELETED_AFTER = "deleted_after";

    public static final String PARAM_DELETED_BEFORE = "deleted_before";

    public static final String PARAM_CREATED_BY = "created_by";

    public static final String PARAM_MODIFIED_BY = "modified_by";

    public static final String PARAM_DELETED_BY = "deleted_by";

    public static final String PARAM_SORT_ORDER = "sort_order";

    public static final String PARAM_SORT_BY = "sort_by";

    public static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";

    public static final List<MCRThumbnailGenerator> THUMBNAIL_GENERATORS = Collections
        .unmodifiableList(MCRConfiguration2
            .getOrThrow("MCR.Media.Thumbnail.Generators", MCRConfiguration2::splitValue)
            .map(MCRConfiguration2::instantiateClass)
            .map(MCRThumbnailGenerator.class::cast)
            .collect(Collectors.toList()));

    private static final String PARAM_CATEGORIES = "category";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PAGE_SIZE_MAX = MCRConfiguration2.getInt("MCR.RestAPI.V2.ListObjects.PageSize.Max")
        .orElseThrow();

    private static final int PAGE_SIZE_DEFAULT = MCRConfiguration2
        .getInt("MCR.RestAPI.V2.ListObjects.PageSize.Default")
        .orElse(1000);

    @Context
    Request request;

    @Context
    ServletContext context;

    @Context
    UriInfo uriInfo;

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    @Operation(
        summary = "Lists all objects in this repository",
        parameters = {
            @Parameter(
                name = PARAM_AFTER_ID,
                description = "the id after which the results should be listed. Do not use after_id and offset " +
                    "together."),
            @Parameter(
                name = PARAM_OFFSET,
                description = "dictates the number of rows to skip from the beginning of the returned data before " +
                    "presenting the results. Do not use after_id and offset together."),
            @Parameter(
                name = PARAM_LIMIT,
                description = "limits the number of result returned"),
            @Parameter(
                name = PARAM_TYPE,
                description = "objects with have the type in the id"),
            @Parameter(
                name = PARAM_PROJECT,
                description = "only objects that have the project in the id"),
            @Parameter(
                name = PARAM_NUMBER_GREATER,
                description = "only objects which id have a number greater than this"),
            @Parameter(
                name = PARAM_NUMBER_LESS,
                description = "only objects which id have a number less than this"),
            @Parameter(
                name = PARAM_CREATED_AFTER,
                description = "objects created after this date"),
            @Parameter(
                name = PARAM_CREATED_BEFORE,
                description = "objects created before this date"),
            @Parameter(
                name = PARAM_MODIFIED_AFTER,
                description = "objects last modified after this date"),
            @Parameter(
                name = PARAM_MODIFIED_BEFORE,
                description = "objects last modified before this date"),
            @Parameter(
                name = PARAM_MODIFIED_AFTER,
                description = "objects last modified after this date"),
            @Parameter(
                name = PARAM_MODIFIED_BEFORE,
                description = "objects last modified before this date"),
            @Parameter(
                name = PARAM_SORT_ORDER,
                description = "sort results 'asc' or 'desc'"),
            @Parameter(
                name = PARAM_SORT_BY,
                description = "sort objects by 'id', 'created' (default) or 'modified'")
        },
        responses = @ApiResponse(
            description = "List of all or matched metadata object IDs with time of last modification",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRObjectIDDate.class)))),
        tags = MCRRestUtils.TAG_MYCORE_OBJECT)
    @XmlElementWrapper(name = "mycoreobjects")
    @JacksonFeatures(serializationDisable = { SerializationFeature.WRITE_DATES_AS_TIMESTAMPS })
    @MCRAccessControlExposeHeaders({HEADER_X_TOTAL_COUNT, HttpHeaders.LINK})
    public Response listObjects(
        @QueryParam(PARAM_AFTER_ID) MCRObjectID afterID,
        @QueryParam(PARAM_OFFSET) Integer offset,
        @QueryParam(PARAM_LIMIT) Integer limit,
        @QueryParam(PARAM_TYPE) String type,
        @QueryParam(PARAM_PROJECT) String project,
        @QueryParam(PARAM_NUMBER_GREATER) Integer numberGreater,
        @QueryParam(PARAM_NUMBER_LESS) Integer numberLess,
        @QueryParam(PARAM_CREATED_AFTER) Date createdAfter,
        @QueryParam(PARAM_CREATED_BEFORE) Date createdBefore,
        @QueryParam(PARAM_MODIFIED_AFTER) Date modifiedAfter,
        @QueryParam(PARAM_MODIFIED_BEFORE) Date modifiedBefore,
        @QueryParam(PARAM_DELETED_AFTER) Date deletedAfter,
        @QueryParam(PARAM_DELETED_BEFORE) Date deletedBefore,
        @QueryParam(PARAM_CREATED_BY) String createdBy,
        @QueryParam(PARAM_MODIFIED_BY) String modifiedBy,
        @QueryParam(PARAM_DELETED_BY) String deletedBy,
        @QueryParam(PARAM_SORT_BY) @DefaultValue("id") MCRObjectQuery.SortBy sortBy,
        @QueryParam(PARAM_SORT_ORDER) @DefaultValue("asc") MCRObjectQuery.SortOrder sortOrder,
        @QueryParam(PARAM_CATEGORIES) List<String> categories) {

        MCRObjectQuery query = new MCRObjectQuery();
        int limitInt = Optional.ofNullable(limit)
            .map(l -> Integer.min(l, PAGE_SIZE_MAX))
            .orElse(PAGE_SIZE_DEFAULT);

        query.limit(limitInt);

        Optional.ofNullable(offset).ifPresent(query::offset);
        Optional.ofNullable(afterID).ifPresent(query::afterId);

        Optional.ofNullable(type).ifPresent(query::type);
        Optional.ofNullable(project).ifPresent(query::project);

        Optional.ofNullable(modifiedAfter).map(Date::toInstant).ifPresent(query::modifiedAfter);
        Optional.ofNullable(modifiedBefore).map(Date::toInstant).ifPresent(query::modifiedBefore);

        Optional.ofNullable(createdAfter).map(Date::toInstant).ifPresent(query::createdAfter);
        Optional.ofNullable(createdBefore).map(Date::toInstant).ifPresent(query::createdBefore);

        Optional.ofNullable(deletedAfter).map(Date::toInstant).ifPresent(query::deletedAfter);
        Optional.ofNullable(deletedBefore).map(Date::toInstant).ifPresent(query::deletedBefore);

        Optional.ofNullable(createdBy).ifPresent(query::createdBy);
        Optional.ofNullable(modifiedBy).ifPresent(query::modifiedBy);
        Optional.ofNullable(deletedBy).ifPresent(query::deletedBy);

        Optional.ofNullable(numberGreater).ifPresent(query::numberGreater);
        Optional.ofNullable(numberLess).ifPresent(query::numberLess);

        List<String> includeCategories = query.getIncludeCategories();
        categories.stream()
            .filter(Predicate.not(String::isBlank))
            .forEach(includeCategories::add);

        query.sort(sortBy, sortOrder);

        MCRObjectQueryResolver queryResolver = MCRObjectQueryResolver.getInstance();

        List<MCRObjectIDDate> idDates = limitInt == 0 ? Collections.emptyList() : queryResolver.getIdDates(query);

        List<MCRRestObjectIDDate> restIdDate = idDates.stream().map(MCRRestObjectIDDate::new)
            .collect(Collectors.toList());

        int count = queryResolver.count(query);
        UriBuilder nextBuilder = null;
        if (query.afterId() != null && idDates.size() == limitInt) {
            nextBuilder = uriInfo.getRequestUriBuilder();
            nextBuilder.replaceQueryParam(PARAM_AFTER_ID, idDates.get(idDates.size() - 1).getId());
        } else {
            if (query.offset() + query.limit() < count) {
                nextBuilder = uriInfo.getRequestUriBuilder();
                nextBuilder.replaceQueryParam(PARAM_OFFSET, String.valueOf(query.offset() + limitInt));
            }
        }

        Response.ResponseBuilder responseBuilder = Response.ok(new GenericEntity<>(restIdDate) {
        })
            .header(HEADER_X_TOTAL_COUNT, count);

        if (nextBuilder != null) {
            responseBuilder.link("next", nextBuilder.toString());
        }

        return responseBuilder.build();
    }

    @POST
    @Operation(
        summary = "Create a new MyCoRe Object",
        responses = @ApiResponse(responseCode = "201",
            description = "Metadata object successfully created",
            headers = @Header(name = HttpHeaders.LOCATION,
                schema = @Schema(type = "string", format = "uri"),
                description = "URL of the new MyCoRe Object")),
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
        } catch (MCRPersistenceException | JDOMException | IOException e) {
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
                        Optional<BufferedImage> thumbnail;
                        try {
                            thumbnail = thumbnailGenerator.get().getThumbnail(mainDoc, size);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return thumbnail.map(b -> {
                            String type = "image/png";
                            if (Objects.equals(ext, "jpg") || Objects.equals(ext, "jpeg")) {
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
        responses = @ApiResponse(
            description = "List of available versions for this metadata object",
            content = @Content(
                array = @ArraySchema(uniqueItems = true,
                    schema = @Schema(implementation = MCRAbstractMetadataVersion.class)))),
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
        List<? extends MCRAbstractMetadataVersion<?>> versions = MCRXMLMetadataManager.instance().listRevisions(id);
        return Response.ok()
            .entity(new GenericEntity<>(versions, TypeUtils.parameterize(List.class, MCRAbstractMetadataVersion.class)))
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
        @PathParam("revision") String revision)
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
        } catch (JDOMException | MCRException e) {
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
        } catch (JDOMException | MCRException e) {
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
    public Response testUpdateObject(@PathParam(PARAM_MCRID) MCRObjectID id) {
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
    public Response testDeleteObject(@PathParam(PARAM_MCRID) MCRObjectID id) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/{" + PARAM_MCRID + "}/service/state")
    @Operation(summary = "change state of object {" + PARAM_MCRID + "}",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "204", description = "operation was successful"),
            @ApiResponse(responseCode = "400", description = "Invalid state"),
            @ApiResponse(responseCode = "404", description = "object is not found"),
        })
    @MCRRequireTransaction
    @MCRApiDraft("MCRObjectState")
    public Response setState(@PathParam(PARAM_MCRID) MCRObjectID id, String state) {
        //check preconditions
        if (!MCRMetadataManager.exists(id)) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
                .withMessage("MCRObject " + id + " not found")
                .toException();
        }
        if (!state.isEmpty()) {
            MCRCategoryID categState = new MCRCategoryID(
                MCRConfiguration2.getString("MCR.Metadata.Service.State.Classification.ID").orElse("state"), state);
            if (!MCRCategoryDAOFactory.getInstance().exist(categState)) {
                throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCROBJECT_INVALID_STATE)
                    .withMessage("Category " + categState + " not found")
                    .toException();
            }
        }
        final MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(id);
        if (state.isEmpty()) {
            mcrObject.getService().removeState();
        } else {
            mcrObject.getService().setState(state);
        }
        try {
            MCRMetadataManager.update(mcrObject);
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

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/{" + PARAM_MCRID + "}/service/state")
    @Operation(summary = "get state of object {" + PARAM_MCRID + "}",
        tags = MCRRestUtils.TAG_MYCORE_OBJECT,
        responses = {
            @ApiResponse(responseCode = "307", description = "redirect to state category"),
            @ApiResponse(responseCode = "204", description = "no state is set"),
            @ApiResponse(responseCode = "404", description = "object is not found"),
        })
    @MCRApiDraft("MCRObjectState")
    public Response getState(@PathParam(PARAM_MCRID) MCRObjectID id) {
        //check preconditions
        if (!MCRMetadataManager.exists(id)) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND)
                .withMessage("MCRObject " + id + " not found")
                .toException();
        }
        final MCRCategoryID state = MCRMetadataManager.retrieveMCRObject(id).getService().getState();
        if (state == null) {
            return Response.noContent().build();
        }
        return Response.temporaryRedirect(
            uriInfo.resolve(URI.create("classifications/" + state.getRootID() + "/" + state.getID())))
            .build();
    }

}
