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

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_CLASSID;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.model.MCRClass;
import org.mycore.datamodel.classifications2.model.MCRClassCategory;
import org.mycore.datamodel.classifications2.model.MCRClassURL;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.converter.MCRDetailLevel;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlElementWrapper;

@Path("/classifications")
@OpenAPIDefinition(
    tags = @Tag(name = MCRRestUtils.TAG_MYCORE_CLASSIFICATION, description = "Operations on classifications"))
public class MCRRestClassifications {

    private static final String PARAM_CATEGID = "categid";

    @Context
    ContainerRequestContext request;

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    @Operation(
        summary = "Lists all classifications in this repository",
        responses = @ApiResponse(
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRClass.class)))),
        tags = MCRRestUtils.TAG_MYCORE_CLASSIFICATION)
    @XmlElementWrapper(name = "classifications")
    public Response listClassifications() {
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        Date lastModified = new Date(categoryDAO.getLastModified());
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(), lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        GenericEntity<List<MCRClass>> entity = new GenericEntity<>(
            categoryDAO.getRootCategories()
                .stream()
                .map(MCRRestClassifications::convertToClass)
                .collect(Collectors.toList())) {
        };
        return Response.ok(entity)
            .lastModified(lastModified)
            .build();
    }

    private static MCRClass convertToClass(MCRCategory cat) {
        MCRClass mcrClass = new MCRClass();
        mcrClass.setID(cat.getId().getRootID());
        mcrClass.getLabel().addAll(cat.getLabels().stream().map(MCRLabel::clone).collect(Collectors.toList()));
        Optional.ofNullable(cat.getURI())
            .map(MCRClassURL::getInstance)
            .ifPresent(mcrClass::setUrl);
        return mcrClass;
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Path("/{" + PARAM_CLASSID + "}")
    @Operation(
        summary = "Returns Classification with the given " + PARAM_CLASSID + ".",
        responses = @ApiResponse(
            content = @Content(schema = @Schema(implementation = MCRClass.class))),
        tags = MCRRestUtils.TAG_MYCORE_CLASSIFICATION)
    public Response getClassification(@PathParam(PARAM_CLASSID) String classId) {
        return getClassification(classId, dao -> dao.getCategory(MCRCategoryID.rootID(classId), -1));
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Path("/{" + PARAM_CLASSID + "}/{" + PARAM_CATEGID + "}")
    @Operation(summary = "Returns Classification with the given " + PARAM_CLASSID + " and " + PARAM_CATEGID + ".",
        responses = @ApiResponse(content = {
            @Content(schema = @Schema(implementation = MCRClass.class)),
            @Content(schema = @Schema(implementation = MCRClassCategory.class))
        },
            description = "If media type parameter " + MCRDetailLevel.MEDIA_TYPE_PARAMETER
                + " is 'summary' an MCRClassCategory is returned. "
                + "In other cases MCRClass with different detail level."),
        tags = MCRRestUtils.TAG_MYCORE_CLASSIFICATION)

    public Response getClassification(@PathParam(PARAM_CLASSID) String classId,
        @PathParam(PARAM_CATEGID) String categId) {

        MCRDetailLevel detailLevel = request.getAcceptableMediaTypes()
            .stream()
            .flatMap(m -> m.getParameters().entrySet().stream()
                .filter(e -> MCRDetailLevel.MEDIA_TYPE_PARAMETER.equals(e.getKey())))
            .map(Map.Entry::getValue)
            .findFirst()
            .map(MCRDetailLevel::valueOf).orElse(MCRDetailLevel.normal);

        MCRCategoryID categoryID = new MCRCategoryID(classId, categId);
        switch (detailLevel) {
            case detailed:
                return getClassification(classId, dao -> dao.getRootCategory(categoryID, -1));
            case summary:
                return getClassification(classId, dao -> dao.getCategory(categoryID, 0));
            case normal: //default case
            default:
                return getClassification(classId, dao -> dao.getRootCategory(categoryID, 0));
        }
    }

    private Response getClassification(String classId, Function<MCRCategoryDAO, MCRCategory> categorySupplier) {
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        Date lastModified = getLastModifiedDate(classId, categoryDAO);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(), lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRCategory classification = categorySupplier.apply(categoryDAO);
        if (classification == null) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRCLASS_NOT_FOUND)
                .withMessage("Could not find classification or category in " + classId + ".")
                .toException();
        }
        return Response.ok()
            .entity(classification.isClassification() ? MCRClass.getClassification(classification)
                : MCRClassCategory.getInstance(classification))
            .lastModified(lastModified)
            .build();
    }

    private static Date getLastModifiedDate(@PathParam(PARAM_CLASSID) String classId, MCRCategoryDAO categoryDAO) {
        long categoryLastModified = categoryDAO.getLastModified(classId);
        return new Date(categoryLastModified > 0 ? categoryLastModified : categoryDAO.getLastModified());
    }

    @PUT
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Path("/{" + PARAM_CLASSID + "}")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(
        summary = "Creates Classification with the given " + PARAM_CLASSID + ".",
        responses = {
            @ApiResponse(responseCode = "400",
                content = { @Content(mediaType = MediaType.TEXT_PLAIN) },
                description = "'MCRCategoryID mismatch'"),
            @ApiResponse(responseCode = "201", description = "Classification successfully created"),
            @ApiResponse(responseCode = "204", description = "Classification successfully updated"),
        },
        tags = MCRRestUtils.TAG_MYCORE_CLASSIFICATION)
    @MCRRequireTransaction
    public Response createClassification(@PathParam(PARAM_CLASSID) String classId, MCRClass mcrClass) {
        if (!classId.equals(mcrClass.getID())) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRCLASS_ID_MISMATCH)
                .withMessage("Classification " + classId + " cannot be overwritten by " + mcrClass.getID() + ".")
                .toException();
        }
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        Response.Status status;
        if (!categoryDAO.exist(MCRCategoryID.rootID(classId))) {
            categoryDAO.addCategory(null, mcrClass.toCategory());
            status = Response.Status.CREATED;
        } else {
            Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(),
                getLastModifiedDate(classId, categoryDAO));
            if (cachedResponse.isPresent()) {
                return cachedResponse.get();
            }
            categoryDAO.replaceCategory(mcrClass.toCategory());
            status = Response.Status.NO_CONTENT;
        }
        Date lastModifiedDate = getLastModifiedDate(classId, categoryDAO);
        return Response.status(status).lastModified(lastModifiedDate).build();
    }

}
