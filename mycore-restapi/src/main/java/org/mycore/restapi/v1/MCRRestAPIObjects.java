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

package org.mycore.restapi.v1;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRRestAPIObjectsHelper;
import org.mycore.restapi.v1.utils.MCRRestAPIUploadHelper;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil.MCRRestAPIACLPermission;

/**
 * REST API methods to retrieve objects and derivates.
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 */
@Path("/v1/objects")
public class MCRRestAPIObjects {
    public static final String STYLE_DERIVATEDETAILS = "derivatedetails";

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String SORT_ASC = "asc";

    public static final String SORT_DESC = "desc";

    /** returns a list of mcrObjects 
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
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
     * @param format - parameter for return format, values are
     *     * xml (default value)
     *     * json
     *     
     * @return a Jersey response object
     * @throws MCRRestAPIException    
     */

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response listObjects(@Context UriInfo info, @Context HttpServletRequest request,
        @QueryParam("format") @DefaultValue("xml") String format, @QueryParam("filter") String filter,
        @QueryParam("sort") @DefaultValue("ID:asc") String sort) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/objects");
        return MCRRestAPIObjectsHelper.listObjects(info, request, format, filter, sort);
    }

    /** 
     * returns a list of derivates for a given MyCoRe Object 
     *
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     * 
     * @param mcrid - an object identifier of syntax [id] or [prefix]:[id]
     * 
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
    
     * @param sort - sortfield and sortorder combined by ':'
     *     * sortfield = ID | lastModified
     *     * sortorder = asc | desc 
     * 
     * @param format - parameter for return format, values are
     *     * xml (default value)
     *     * json
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Path("/{mcrid}/derivates")
    public Response listDerivates(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrid") String mcrid, @QueryParam("format") @DefaultValue("xml") String format,
        @QueryParam("sort") @DefaultValue("ID:asc") String sort) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/objects");
        return MCRRestAPIObjectsHelper.listDerivates(info, request, mcrid, format, sort);
    }

    /**
     * returns a single derivate object in XML Format
     * 
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     * 
     * @param id an object identifier of syntax [id] or [prefix]:[id]
     * 
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
    
     * 
     * @param style allowed values are "derivatedetails"
     * derivate details will be integrated into the output.
     * 
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("/{mcrid}")
    public Response returnMCRObject(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrid") String id, @QueryParam("style") String style) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/objects");
        return MCRRestAPIObjectsHelper.showMCRObject(id, style, info, request);
    }

    /**
     * returns a single object in XML Format
     * 
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     * 
     * @param mcrid - a object identifier of syntax [id] or [prefix]:[id]
     * @param derid - a derivate identifier of syntax [id] or [prefix]:[id]
     * 
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
     * 
     * @param style - controls the output: 
     * allowed values are "derivatedetails" to integrate derivate details into the output.
     * 
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("/{mcrid}/derivates/{derid}")
    public Response returnDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrid") String mcrid, @PathParam("derid") String derid, @QueryParam("style") String style)
        throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/objects");
        return MCRRestAPIObjectsHelper.showMCRDerivate(mcrid, derid, info, request);
    }

    /** returns a list of derivates for a given MyCoRe Object 
     *
     * @param info - the injected Jersey URIInfo object
     * @param httpRequest - the injected HTTPServletRequest object
     * @param request - the injected JAX-WS request object
     * 
     * @param mcrid - a object identifier of syntax [id] or [prefix]:[id]
     * @param derid - a derivate identifier of syntax [id] or [prefix]:[id]
     * 
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
     * @throws MCRRestAPIException
     */

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Path("/{mcrid}/derivates/{derid}/contents{path:(/.*)*}")
    public Response listContents(@Context UriInfo info, @Context HttpServletRequest httpRequest,
        @Context Request request, @PathParam("mcrid") String mcrid, @PathParam("derid") String derid,
        @PathParam("path") @DefaultValue("/") String path, @QueryParam("format") @DefaultValue("xml") String format,
        @QueryParam("depth") @DefaultValue("-1") int depth) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(httpRequest, MCRRestAPIACLPermission.READ, "/v1/objects");
        return MCRRestAPIObjectsHelper.listContents(info, httpRequest, request, mcrid, derid, format, path, depth);
    }

    /**
     *  redirects to the maindoc of the given derivate
     *  
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     *
     * @param mcrObjID - a object identifier of syntax [id] or [prefix]:[id]
     * @param mcrDerID - a derivate identifier of syntax [id] or [prefix]:[id]
     * 
     * Allowed Prefixes are "mcr" or application specific search keys
     * "mcr" is the default prefix for MyCoRe IDs.
     *      
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     * 
     */
    @GET
    @Path("/{mcrid}/derivates/{derid}/open")
    public Response listContents(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrid") String mcrObjID, @PathParam("derid") String mcrDerID) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/objects");
        try {
            String url = MCRRestAPIObjectsHelper.retrieveMaindocURL(info, mcrObjID, mcrDerID);
            if (url != null) {
                return Response.seeOther(new URI(url)).build();
            }
        } catch (IOException | URISyntaxException e) {
            throw new MCRRestAPIException(Response.Status.INTERNAL_SERVER_ERROR,
                new MCRRestAPIError(MCRRestAPIError.CODE_INTERNAL_ERROR,
                    "A problem occurred while opening maindoc from derivate " + mcrDerID, e.getMessage()));
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
     * @throws MCRRestAPIException
     * 
     */
    @POST
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8" })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadObject(@Context UriInfo info, @Context HttpServletRequest request,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetails) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.WRITE, "/v1/objects");
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
     * @throws MCRRestAPIException
     * 
     */

    @POST
    @Path("/{mcrObjID}/derivates")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8" })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @FormDataParam("label") String label,
        @FormDataParam("overwriteOnExistingLabel") @DefaultValue("false") boolean overwrite)
        throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.WRITE, "/v1/objects");
        return MCRRestAPIUploadHelper.uploadDerivate(info, request, mcrObjID, label, overwrite);
    }

    /**
     * upload a file into a given derivate
     * @param info - the injected Jersey URIInfo object
     * @param request - the injected HTTPServletRequest object
     * 
     * @param mcrObjID - a MyCoRe Object ID
     * @param mcrDerID - a MyCoRe Derivate ID
     * 
     * @param uploadedInputStream - the inputstream from HTTP Post
     * @param fileDetails - file information from HTTP Post
     * @param path - the target path inside the derivate
     * @param maindoc - true, if the file should be set as maindoc
     * @param unzip - true, if the file is a zip file, that should be extracted
     * @param md5 - the md5 sum of the uploaded file
     * @param size - the size of the uploaded file
     * @return a Jersey Response object
     * @throws MCRRestAPIException
     */
    @POST
    @Path("/{mcrid}/derivates/{derid}/contents{path:(/.*)*}")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8" })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrid") String mcrObjID, @PathParam("derid") String mcrDerID,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetails, @FormDataParam("path") String path,
        @FormDataParam("maindoc") @DefaultValue("false") boolean maindoc,
        @FormDataParam("unzip") @DefaultValue("false") boolean unzip, @FormDataParam("md5") String md5,
        @FormDataParam("size") Long size) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.WRITE, "/v1/objects");
        return MCRRestAPIUploadHelper.uploadFile(info, request, mcrObjID, mcrDerID, uploadedInputStream, fileDetails,
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
     * @throws MCRRestAPIException
     * 
     */
    @DELETE
    @Path("/{mcrObjID}/derivates/{mcrDerID}/contents")
    public Response deleteFiles(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @PathParam("mcrDerID") String mcrDerID) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.WRITE, "/v1/objects");
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
     * @throws MCRRestAPIException
     * 
     */
    @DELETE
    @Path("/{mcrObjID}/derivates/{mcrDerID}")
    public Response deleteDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @PathParam("mcrDerID") String mcrDerID) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.WRITE, "/v1/objects");
        return MCRRestAPIUploadHelper.deleteDerivate(info, request, mcrObjID, mcrDerID);
    }
}
