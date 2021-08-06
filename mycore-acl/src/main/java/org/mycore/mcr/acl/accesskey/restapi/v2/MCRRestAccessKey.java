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

package org.mycore.mcr.acl.accesskey.restapi.v2;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTransformer;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.restapi.v2.annotation.MCRRequireAccessKeyAuthorization;
import org.mycore.mcr.acl.accesskey.restapi.v2.model.MCRAccessKeyInformation;
import org.mycore.restapi.annotations.MCRRequireTransaction;

@Path("/objects/{" + PARAM_MCRID + "}/accesskeys")
public class MCRRestAccessKey {

    private static final String VALUE = "value";

    @GET
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response addAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, final String accessKeyJson) {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        accessKey.setObjectId(objectId);
        accessKey.setCreator(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setCreation(new Date());
        MCRAccessKeyManager.addAccessKey(accessKey);
        return Response.ok(MCRAccessKeyManager.getAccessKeyByValue(objectId, accessKey.getValue())).build();
    }

    @DELETE
    @Path("/{" + VALUE + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response deleteAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam(VALUE) final String value) throws IOException {
        MCRAccessKeyManager.deleteAccessKey(objectId, value);
        return Response.noContent().build();
    }
    
    @PUT
    @Path("/{" + VALUE + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRequireAccessKeyAuthorization
    @MCRRequireTransaction
    public Response updateAccessKey(@PathParam(PARAM_MCRID) final MCRObjectID objectId, 
        @PathParam(VALUE) final String value, final String accessKeyJson) throws IOException {
        final MCRAccessKey accessKey = MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
        accessKey.setObjectId(objectId);
        accessKey.setValue(value);
        accessKey.setLastChanger(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setLastChange(new Date());
        MCRAccessKeyManager.updateAccessKey(accessKey);
        return Response.ok(MCRAccessKeyManager.getAccessKeyByValue(objectId, value)).build();
    }
}
