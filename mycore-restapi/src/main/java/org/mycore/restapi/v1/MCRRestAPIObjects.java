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

package org.mycore.restapi.v1;

import static org.mycore.frontend.jersey.MCRJerseyUtil.APPLICATION_JSON_UTF_8;
import static org.mycore.frontend.jersey.MCRJerseyUtil.TEXT_XML_UTF_8;
import static org.mycore.restapi.v1.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.v1.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRRestAPIObjectsHelper;
import org.mycore.restapi.v1.utils.MCRRestAPIUploadHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST API methods to retrieve objects and derivates.
 *
 * @author Robert Stephan
 *
 */
@Path("/objects")
public class MCRRestAPIObjects {

    public static final String STYLE_DERIVATEDETAILS = "derivatedetails";

    private static final String PATH_DERIVATES = "derivates";

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String SORT_ASC = "asc";

    public static final String SORT_DESC = "desc";

    public static final String QUERY_PARAM_FORMAT = "format";

    public static final String QUERY_PARAM_SORT = "sort";

    public static final String QUERY_PARAM_FILTER = "filter";

    public static final String QUERY_PARAM_STYLE = "style";

    public static final String QUERY_PARAM_DEPTH = "depth";

    @Context
    Application app;

    /** returns a list of mcrObjects
     *
     * @param info - the injected Jersey URIInfo object
     * @param format - parameter for return format, values are
     *     * xml (default value)
     *     * json
     *
     * @param filter - parameter with filters as colon-separated key-value-pairs,
     *                 pair separator is semicolon, allowed values are
     *     * project - the MyCoRe ProjectID - first Part of a MyCoRe ID
     *     * type - the MyCoRe ObjectType - middle Part of a MyCoRe ID
     *     * lastModifiedBefore - last modified date in UTC is lesser than or equals to given value
     *     * lastModifiedAfter - last modified date in UTC is greater than or equals to given value;
     *
     * @param sort - sortfield and sortorder combined by ':'
     *     * sortfield = ID | lastModified
     *     * sortorder = asc | desc
     *
     * @return a Jersey response object
     */
    @GET
    @Produces({ TEXT_XML_UTF_8, APPLICATION_JSON_UTF_8 })
    public Response listObjects(@Context UriInfo info,
        @QueryParam(QUERY_PARAM_FORMAT) @DefaultValue(FORMAT_XML) String format,
        @QueryParam(QUERY_PARAM_FILTER) String filter,
        @QueryParam(QUERY_PARAM_SORT) @DefaultValue("ID:" + SORT_ASC) String sort) throws MCRRestAPIException {
        return MCRRestAPIObjectsHelper.listObjects(info, format, filter, sort);
    }

    /**
     * Returns a list of derivates for a given MyCoRe Object.
     *
     * @param info - the injected Jersey URIInfo object
     * @param mcrid - an object identifier of syntax [id] or [prefix]:[id]
     * <p>
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
    
     * @param format - parameter for return format, values are
     *     * xml (default value)
     *     * json
     * @param sort - sortfield and sortorder combined by ':'
     *     * sortfield = ID | lastModified
     *     * sortorder = asc | desc
     *
     * @return a Jersey Response object
     */
    @GET
    @Produces({ TEXT_XML_UTF_8, APPLICATION_JSON_UTF_8 })
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES)
    public Response listDerivates(@Context UriInfo info,
        @PathParam(PARAM_MCRID) String mcrid,
        @QueryParam(QUERY_PARAM_FORMAT) @DefaultValue(FORMAT_XML) String format,
        @QueryParam(QUERY_PARAM_SORT) @DefaultValue("ID:" + SORT_ASC) String sort) throws MCRRestAPIException {
        return MCRRestAPIObjectsHelper.listDerivates(info, mcrid, format, sort);
    }

    /**
     * Returns a single derivate object in XML Format.
     *
     * @param info - the injected Jersey URIInfo object
     * @param id an object identifier of syntax [id] or [prefix]:[id]
     * <p>
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
    
     *
     * @param style allowed values are "derivatedetails"
     * derivate details will be integrated into the output.
     *
     * @return a Jersey Response object
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("/{" + PARAM_MCRID + "}")
    public Response returnMCRObject(@Context UriInfo info,
        @PathParam(PARAM_MCRID) String id, @QueryParam(QUERY_PARAM_STYLE) String style)
        throws MCRRestAPIException {
        return MCRRestAPIObjectsHelper.showMCRObject(id, style, info, app);
    }

    /**
     * Returns a single object in XML Format.
     *
     * @param info - the injected Jersey URIInfo object
     * @param mcrid - a object identifier of syntax [id] or [prefix]:[id]
     * @param derid - a derivate identifier of syntax [id] or [prefix]:[id]
     * <p>
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
     *
     * @param style - controls the output:
     * allowed values are "derivatedetails" to integrate derivate details into the output.
     *
     * @return a Jersey Response object
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES + "/{" + PARAM_DERID + "}")
    public Response returnDerivate(@Context UriInfo info,
        @PathParam(PARAM_MCRID) String mcrid,
        @PathParam(PARAM_DERID) String derid,
        @QueryParam(QUERY_PARAM_STYLE) String style)
        throws MCRRestAPIException {
        return MCRRestAPIObjectsHelper.showMCRDerivate(mcrid, derid, info, app,
            Objects.equals(style, "derivatedetails"));
    }

    /** 
     * Returns a list of derivates for a given MyCoRe Object.
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected JAX-WS request object
     *
     * @param mcrid - a object identifier of syntax [id] or [prefix]:[id]
     * @param derid - a derivate identifier of syntax [id] or [prefix]:[id]
     * <p>
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
     *
     * @param path - the relative path to a directory inside a derivate
     * @param format - specifies the return format, allowed values are:
     *     * xml (default value)
     *     * json
     *
     * @param depth - the level of subdirectories that should be returned
     *
     * @return a Jersey Response object
     */
    @GET
    @Produces({ TEXT_XML_UTF_8, APPLICATION_JSON_UTF_8 })
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES + "/{" + PARAM_DERID + "}/contents{path:(/.*)*}")
    public Response listContents(@Context UriInfo info,
        @Context Request request, @PathParam(PARAM_MCRID) String mcrid,
        @PathParam(PARAM_DERID) String derid,
        @PathParam("path") @DefaultValue("/") String path,
        @QueryParam(QUERY_PARAM_FORMAT) @DefaultValue(FORMAT_XML) String format,
        @QueryParam(QUERY_PARAM_DEPTH) @DefaultValue("-1") int depth) throws MCRRestAPIException {
        return MCRRestAPIObjectsHelper.listContents(info, app, request, mcrid, derid, format, path, depth);
    }

    /**
     * Redirects to the maindoc of the given derivate.
     *
     * @param info - the injected Jersey URIInfo object
     * @param mcrObjID - a object identifier of syntax [id] or [prefix]:[id]
     * @param mcrDerID - a derivate identifier of syntax [id] or [prefix]:[id]
     * <p>
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
     *
     * @return a Jersey Response object
     *
     */
    @GET
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES + "/{" + PARAM_DERID + "}/open")
    public Response listContents(@Context UriInfo info,
        @PathParam(PARAM_MCRID) String mcrObjID,
        @PathParam(PARAM_DERID) String mcrDerID)
        throws MCRRestAPIException {
        try {
            String url = MCRRestAPIObjectsHelper.retrieveMaindocURL(info, mcrObjID, mcrDerID, app);
            if (url != null) {
                return Response.seeOther(new URI(url)).build();
            }
        } catch (URISyntaxException e) {
            MCRRestAPIException restAPIException = new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR,
                    "A problem occurred while opening maindoc from derivate " + mcrDerID, e.getMessage()));
            restAPIException.initCause(e);
            throw restAPIException;
        }
        throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
            new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR,
                "A problem occurred while opening maindoc from derivate " + mcrDerID, ""));
    }

    //*****************************
    //* Upload API
    //*****************************
    /**
     * create / update a MyCoRe object
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     *
     * @param uploadedInputStream - the MyCoRe Object (XML) as inputstream from HTTP Post
     * @param fileDetails - file metadata from HTTP Post
     *
     * @return a Jersey Response object
     *
     */
    @POST
    @Produces({ TEXT_XML_UTF_8 })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response uploadObject(@Context UriInfo info, @Context HttpServletRequest request,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetails) throws MCRRestAPIException {
        return MCRRestAPIUploadHelper.uploadObject(info, request, uploadedInputStream, fileDetails);
    }

    /**
     * create a new (empty) MyCoRe derivate or returns one that already exists (by label)
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     *
     * @param mcrObjID - a MyCoRe Object ID
     *
     * @param label - the label of the new derivate
     * @param overwrite - if true, return an existing derivate (with same label)
     *
     * @return a Jersey Response object
     *
     */

    @POST
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES)
    @Produces({ TEXT_XML_UTF_8 })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response uploadDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam(PARAM_MCRID) String mcrObjID, @FormDataParam("label") String label,
        @FormDataParam("classifications") String classifications,
        @FormDataParam("overwriteOnExistingLabel") @DefaultValue("false") boolean overwrite) {
        return MCRRestAPIUploadHelper.uploadDerivate(info, request, mcrObjID, label, classifications, overwrite);
    }

    /**
     * upload a file into a given derivate
     *
     * @param info                - the injected Jersey URIInfo object
     * @param mcrObjID            - a MyCoRe Object ID
     * @param mcrDerID            - a MyCoRe Derivate ID
     * @param uploadedInputStream - the inputstream from HTTP Post
     * @param path                - the target path inside the derivate
     * @param maindoc             - true, if the file should be set as maindoc
     * @param unzip               - true, if the file is a zip file, that should be extracted
     * @param md5                 - the md5 sum of the uploaded file
     * @param size                - the size of the uploaded file
     * @return a Jersey Response object
     */
    @POST
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES + "/{" + PARAM_DERID + "}/contents{path:(/.*)*}")
    @Produces({ TEXT_XML_UTF_8 })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @MCRRequireTransaction
    @MCRAccessControlExposeHeaders(HttpHeaders.LOCATION)
    public Response uploadFile(@Context UriInfo info,
        @PathParam(PARAM_MCRID) String mcrObjID,
        @PathParam(PARAM_DERID) String mcrDerID,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("path") String path,
        @FormDataParam("maindoc") @DefaultValue("false") boolean maindoc,
        @FormDataParam("unzip") @DefaultValue("false") boolean unzip, @FormDataParam("md5") String md5,
        @FormDataParam("size") Long size) throws MCRRestAPIException {
        return MCRRestAPIUploadHelper.uploadFile(info, mcrObjID, mcrDerID, uploadedInputStream,
            path, maindoc, unzip, md5, size);
    }

    /**
     * delete all file from a given derivate
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     *
     * @param mcrObjID - a MyCoRe Object ID
     * @param mcrDerID - a MyCoRe Derivate ID
     *
     * @return a Jersey Response object
     *
     */
    @DELETE
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES + "/{" + PARAM_DERID + "}/contents")
    @MCRRequireTransaction
    public Response deleteFiles(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam(PARAM_MCRID) String mcrObjID,
        @PathParam(PARAM_DERID) String mcrDerID) {
        return MCRRestAPIUploadHelper.deleteAllFiles(info, request, mcrObjID, mcrDerID);
    }

    /**
     * delete a whole derivate
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     *
     * @param mcrObjID - a MyCoRe Object ID
     * @param mcrDerID - a MyCoRe Derivate ID
     *
     * @return a Jersey Response object
     *
     */
    @DELETE
    @Path("/{" + PARAM_MCRID + "}/" + PATH_DERIVATES + "/{" + PARAM_DERID + "}")
    @MCRRequireTransaction
    public Response deleteDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam(PARAM_MCRID) String mcrObjID,
        @PathParam(PARAM_DERID) String mcrDerID) throws MCRRestAPIException {
        return MCRRestAPIUploadHelper.deleteDerivate(info, request, mcrObjID, mcrDerID);
    }

}
