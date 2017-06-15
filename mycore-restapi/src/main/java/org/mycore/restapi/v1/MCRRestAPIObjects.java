/*
 * $Revision: 29635 $ $Date: 2014-04-10 10:55:06 +0200 (Do, 10 Apr 2014) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.utils.MCRRestAPIObjectsHelper;
import org.mycore.restapi.v1.utils.MCRRestAPIUploadHelper;

/**
 * REST API methods to retrieve objects and derivates.
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 */
@Path("/v1/objects")
@MCRStaticContent
public class MCRRestAPIObjects {
    public static final String STYLE_DERIVATEDETAILS = "derivatedetails";

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String SORT_ASC = "asc";

    public static final String SORT_DESC = "desc";

    /** returns a list of mcrObjects 
     *
     * Parameter
     * ---------
     * filter - parameter with filters as colon-separated key-value-pairs, pair separator is semicolon, allowed values are
     *     * project - the MyCoRe ProjectID - first Part of a MyCoRe ID
     *     * type - the MyCoRe ObjectType - middle Part of a MyCoRe ID 
     *     * lastModifiedBefore - last modified date in UTC is lesser than or equals to given value 
     *     * lastModifiedAfter - last modified date in UTC is greater than or equals to given value;
     * 
     * sort - sortfield and sortorder combined by ':'
     *     * sortfield = ID | lastModified
     *     * sortorder = asc | desc 
     * 
     * format - parameter for return format, values are
     *     * xml (default value)
     *     * json
     */

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response listObjects(@Context UriInfo info, @QueryParam("format") @DefaultValue("xml") String format,
        @QueryParam("filter") String filter, @QueryParam("sort") @DefaultValue("ID:asc") String sort) {

        return MCRRestAPIObjectsHelper.listObjects(info, format, filter, sort);
    }

    /** returns a list of derivates for a given MyCoRe Object 
     *
     * Parameter
     * ---------
     * sort - sortfield and sortorder combined by ':'
     *     * sortfield = ID | lastModified
     *     * sortorder = asc | desc 
     * 
     * format - parameter for return format, values are
     *     * xml (default value)
     *     * json
     */

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Path("/{mcrid}/derivates")
    public Response listDerivates(@Context UriInfo info, @PathParam("mcrid") String mcrID,
        @QueryParam("format") @DefaultValue("xml") String format,
        @QueryParam("sort") @DefaultValue("ID:asc") String sort) {

        return MCRRestAPIObjectsHelper.listDerivates(info, mcrID, format, sort);
    }

    /**
     * returns a single object in XML Format
     * @param id an object identifier of syntax [id] or [prefix]:[id]
     * 
     * Allowed Prefixes are "mcr"
     * "mcr" is the default prefix for MyCoRe IDs.
     * 
     * @param style allowed values are "derivatedetails"
     * derivate details will be integrated into the output.
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("/{value}")
    public Response returnMCRObject(@Context UriInfo info, @PathParam("value") String id,
        @QueryParam("style") String style) {
        return MCRRestAPIObjectsHelper.showMCRObject(id, style, info);

    }

    /**
     * returns a single object in XML Format
     * @param mcrid an object identifier of syntax [id] or [prefix]:[id]
     * 
     * Allowed Prefixes are "mcr"
     * "mcr" is the default prefix for MyCoRe IDs.
     * 
     * @param style allowed values are "derivatedetails"
     * derivate details will be integrated into the output.
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("/{mcrid}/derivates/{derid}")
    public Response returnDerivate(@Context UriInfo info, @PathParam("mcrid") String mcrid,
        @PathParam("derid") String derid, @QueryParam("style") String style) {
        try {
            return MCRRestAPIObjectsHelper.showMCRDerivate(mcrid, derid, info);
        } catch (IOException e) {
            return MCRRestAPIError.create(Response.Status.INTERNAL_SERVER_ERROR, MCRRestAPIError.CODE_INTERNAL_ERROR,
                "Unable to display derivate content", e.getMessage()).createHttpResponse();
        }
    }

    /** returns a list of derivates for a given MyCoRe Object 
    *
    * Parameter
    * ---------
    * 
    * format - parameter for return format, values are
    *     * xml (default value)
    *     * json
    */

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Path("/{mcrid}/derivates/{derid}/contents{path:(/.*)*}")
    public Response listContents(@Context UriInfo info, @Context Request request, @PathParam("mcrid") String mcrID,
        @PathParam("derid") String derID, @PathParam("path") @DefaultValue("/") String path,
        @QueryParam("format") @DefaultValue("xml") String format, @QueryParam("depth") @DefaultValue("-1") int depth) {
        try {
            return MCRRestAPIObjectsHelper.listContents(request, mcrID, derID, format, path, depth, info);
        } catch (IOException e) {
            return MCRRestAPIError.create(Response.Status.INTERNAL_SERVER_ERROR, MCRRestAPIError.CODE_INTERNAL_ERROR,
                "A problem occurred while fetching the data", e.getMessage()).createHttpResponse();
        }
    }

    /** redirects to the maindoc of the given derivate 
    *
    * No Parameter
    * 
    */
    @GET
    @Path("/{mcrid}/derivates/{derid}/open")
    public Response listContents(@Context UriInfo info, @Context Request request, @PathParam("mcrid") String mcrID,
        @PathParam("derid") String derID) {
        try {
            String url = MCRRestAPIObjectsHelper.retrieveMaindocURL(mcrID, derID, info);
            if (url != null) {
                return Response.seeOther(new URI(url)).build();
            }
        } catch (IOException | URISyntaxException e) {
            return MCRRestAPIError
                .create(Response.Status.INTERNAL_SERVER_ERROR, MCRRestAPIError.CODE_INTERNAL_ERROR,
                    "A problem occurred while opening maindoc from derivate " + derID, e.getMessage())
                .createHttpResponse();
        }
        return MCRRestAPIError.create(Response.Status.INTERNAL_SERVER_ERROR, MCRRestAPIError.CODE_INTERNAL_ERROR,
            "A problem occurred while opening maindoc from derivate " + derID, "").createHttpResponse();
    }

    //Upload API
    @POST
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8" })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadObject(@Context UriInfo info, @Context HttpServletRequest request,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetails) {
        return MCRRestAPIUploadHelper.uploadObject(info, request, uploadedInputStream, fileDetails);

    }

    @POST
    @Path("/{mcrObjID}/derivates")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8" })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @FormDataParam("label") String label) {
        return MCRRestAPIUploadHelper.uploadDerivate(info, request, mcrObjID, label);
    }

    @POST
    @Path("/{mcrObjID}/derivates/{mcrDerID}/contents")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8" })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @PathParam("mcrDerID") String mcrDerID,
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetails, @FormDataParam("path") String path,
        @DefaultValue("false") @FormDataParam("maindoc") boolean maindoc,
        @DefaultValue("false") @FormDataParam("unzip") boolean unzip, @FormDataParam("md5") String md5,
        @FormDataParam("size") Long size) {
        return MCRRestAPIUploadHelper.uploadFile(info, request, mcrObjID, mcrDerID, uploadedInputStream, fileDetails,
            path, maindoc, unzip, md5, size);
    }

    @DELETE
    @Path("/{mcrObjID}/derivates/{mcrDerID}/contents")
    public Response deleteFiles(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @PathParam("mcrDerID") String mcrDerID) {
        return MCRRestAPIUploadHelper.deleteAllFiles(info, request, mcrObjID, mcrDerID);

    }

    @DELETE
    @Path("/{mcrObjID}/derivates/{mcrDerID}")
    public Response deleteDerivate(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("mcrObjID") String mcrObjID, @PathParam("mcrDerID") String mcrDerID) {
        return MCRRestAPIUploadHelper.deleteDerivate(info, request, mcrObjID, mcrDerID);

    }

}
