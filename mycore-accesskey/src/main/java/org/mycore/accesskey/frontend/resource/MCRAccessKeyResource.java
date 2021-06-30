/*
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

package org.mycore.accesskey.frontend.resource;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.accesskey.MCRAccessKeyManager;
import org.mycore.accesskey.MCRAccessKeyTransformer;
import org.mycore.accesskey.backend.MCRAccessKey;
import org.mycore.accesskey.frontend.resource.annotation.MCRRequireAccessKeyAuthorization;
import org.mycore.accesskey.frontend.resource.model.MCRAccessKeyInformation;
import org.mycore.restapi.annotations.MCRRequireTransaction;

@Path("/accesskeys")
public class MCRAccessKeyResource {

    private static final String VALUE = "value";

    @GET
    @Path("/{" + PARAM_MCRID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    public Response getAccessKeys(@PathParam(PARAM_MCRID) final MCRObjectID objectId,
        @DefaultValue("0") @QueryParam("offset") long offset,
        @DefaultValue("128") @QueryParam("limit") long limit) {
        final long fromIndex = offset;
        List<MCRAccessKey> accessKeys = MCRAccessKeyManager.getAccessKeys(objectId);
        final int totalAccessKeyCount = accessKeys.size();
        if (offset < 0 || limit <= 0 || fromIndex >= Integer.MAX_VALUE || totalAccessKeyCount == 0 
            || (int) fromIndex >= totalAccessKeyCount) {
            accessKeys = new ArrayList<MCRAccessKey>();
        } else {
            long toIndex = offset + limit;
            if (toIndex > Integer.MAX_VALUE) {
                toIndex = Integer.MAX_VALUE;
            }
            if ((int) toIndex >= totalAccessKeyCount) {
                toIndex = (long) totalAccessKeyCount;
            }
            accessKeys = accessKeys.subList((int) fromIndex, (int) toIndex);
        }
        return Response.ok(new MCRAccessKeyInformation(accessKeys, totalAccessKeyCount)).build();
    }

    @POST
    @Path("/{" + PARAM_MCRID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response addAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, final String accessKeyJson) {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        accessKey.setObjectId(objectId);
        MCRAccessKeyManager.addAccessKey(accessKey);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{" + PARAM_MCRID + "}/{" + VALUE + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response deleteAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam(VALUE) final String valueEncoded) {
        final String value = new String(Base64.getUrlDecoder().decode(valueEncoded));
        MCRAccessKeyManager.deleteAccessKey(objectId, value);
        return Response.noContent().build();
    }
    
    @PUT
    @Path("/{" + PARAM_MCRID + "}/{" + VALUE + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response updateAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam(VALUE) final String valueEncoded, final String accessKeyJson) {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        final String value = new String(Base64.getUrlDecoder().decode(valueEncoded));
        MCRAccessKeyManager.updateAccessKey(objectId, value, accessKey);
        return Response.noContent().build();
    }
}
