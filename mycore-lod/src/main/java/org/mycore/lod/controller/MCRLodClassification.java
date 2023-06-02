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

package org.mycore.lod.controller;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.model.MCRClass;
import org.mycore.datamodel.classifications2.model.MCRClassCategory;
import org.mycore.datamodel.classifications2.utils.MCRSkosTransformer;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.lod.MCRJerseyLodApp;
import org.mycore.restapi.converter.MCRDetailLevel;
import org.mycore.restapi.v2.MCRErrorResponse;
import org.mycore.restapi.v2.MCRRestUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Linked Open Data: Classification End point
 * 
 * @author Robert Stephan
 */
@Path("/classification")
public class MCRLodClassification {

    /** error code for error response */
    public static final String ERROR_MCRCLASS_NOT_FOUND = "MCRCLASS_NOT_FOUND";

    /** error code for error response */
    public static final String ERROR_MCRCLASS_ID_MISMATCH = "MCRCLASS_ID_MISMATCH";

    /** error code for error response */
    public static final String ERROR_MCRCLASS_TRANSFORMATION = "MCRCLASS_TRANSFORMATION";

    /** parameter key in request url paths */
    private static final String PARAM_CLASSID = "classid";

    /** parameter key in request url paths */
    private static final String PARAM_CATEGID = "categid";

    @Context
    ContainerRequestContext request;

    /**
     * return the list of available classifications as Linked Open Data 
     * 
     * TODO Is there a reasonable response on the base path of an LOD URI,
     * or remove this endpoint completely?
     * 
     * @return a jersey response with the list of classifications
     */
    @GET
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response outputLODClassificationRoot() {
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        Date lastModified = new Date(categoryDAO.getLastModified());
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(), lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        try {
            Document classRdfxml = createClassList();
            String rdfxmlString = new MCRJDOMContent(classRdfxml).asString();
            List<String> mimeTypes = request.getAcceptableMediaTypes().parallelStream().map(MediaType::toString)
                .toList();
            URI uri = request.getUriInfo().getBaseUri();
            return MCRJerseyLodApp.returnLinkedData(rdfxmlString, uri, mimeTypes);
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(ERROR_MCRCLASS_TRANSFORMATION)
                .withMessage("Could create classification list.")
                .toException();
        }
    }

    /**
     * return a classification (with its categories on the first hierarchy level as linked open data)
     * @param classId - the classification ID
     * @return the Response with the classification as linked open data 
     */
    @GET
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Path("/{" + PARAM_CLASSID + "}")
    public Response getClassification(@PathParam(PARAM_CLASSID) String classId) {
        List<MediaType> mediaTypes = request.getAcceptableMediaTypes();
        return getClassification(MCRCategoryID.rootID(classId),
            dao -> dao.getCategory(MCRCategoryID.rootID(classId), 1), mediaTypes,
            request.getUriInfo().getBaseUri());
    }

    /**
     * return a category and its children on the first hierarchy level as linked open data
     * 
     * @param classId - the class ID
     * @param categId - the category ID
     * @return the Response with the category as linked open data
     */
    @GET
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

        MCRCategoryID categoryId = new MCRCategoryID(classId, categId);
        return getClassification(categoryId, dao -> dao.getRootCategory(categoryId, 0),
            request.getAcceptableMediaTypes(),
            request.getUriInfo().getBaseUri());
    }

    private Response getClassification(MCRCategoryID categId, Function<MCRCategoryDAO, MCRCategory> categorySupplier,
        List<MediaType> acceptMediaTypes, URI uri) {
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        String classId = categId.getRootID();
        Date lastModified = getLastModifiedDate(classId, categoryDAO);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(), lastModified);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        MCRCategory classification = categorySupplier.apply(categoryDAO);
        if (classification == null) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(ERROR_MCRCLASS_NOT_FOUND)
                .withMessage("Could not find classification or category in " + classId + ".")
                .toException();
        }
        try {
            Document classRdfXml = MCRSkosTransformer.getSkosInRDFXML(classification, categId);
            String rdfxmlString = new MCRJDOMContent(classRdfXml).asString();
            List<String> mimeTypes = acceptMediaTypes.parallelStream().map(MediaType::toString).toList();
            return MCRJerseyLodApp.returnLinkedData(rdfxmlString, uri, mimeTypes);
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(ERROR_MCRCLASS_TRANSFORMATION)
                .withMessage("Could not find classification or category in " + classId + ".")
                .toException();
        }
    }

    private Document createClassList() {
        Element eBag = new Element("Bag", MCRConstants.RDF_NAMESPACE);
        MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.getInstance();
        for (MCRCategory categ : categoryDAO.getRootCategories()) {
            eBag.addContent(new Element("li", MCRConstants.RDF_NAMESPACE)
                .setAttribute("resource",
                    MCRFrontendUtil.getBaseURL() + "open-data/classification/" + categ.getId().toString(),
                    MCRConstants.RDF_NAMESPACE));
        }
        return new Document(eBag);
    }

    private static Date getLastModifiedDate(@PathParam(PARAM_CLASSID) String classId, MCRCategoryDAO categoryDAO) {
        long categoryLastModified = categoryDAO.getLastModified(classId);
        return new Date(categoryLastModified > 0 ? categoryLastModified : categoryDAO.getLastModified());
    }

}
