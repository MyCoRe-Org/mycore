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

package org.mycore.pi.frontend.resources;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPIServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.frontend.model.MCRPIErrorJSON;
import org.mycore.pi.frontend.model.MCRPIListJSON;
import org.mycore.pi.frontend.model.MCRPIRegistrationJSON;

import com.google.gson.Gson;

@Path("pi/registration")
public class MCRPersistentIdentifierRegistrationResource {

    public static final int COUNT_LIMIT = 100;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Path("type/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listByType(@PathParam("type") String type, @DefaultValue("0") @QueryParam("from") int from,
        @DefaultValue("50") @QueryParam("size") int size) {
        Response errorResponse = validateParameters(from, size);
        if (errorResponse != null)
            return errorResponse;
        List<MCRPIRegistrationInfo> mcrpiRegistrationInfos = MCRPIManager.getInstance().getList(type,
            from, size);
        return Response.status(Response.Status.OK)
            .entity(new Gson().toJson(new MCRPIListJSON(type, from, size,
                MCRPIManager.getInstance().getCount(type), mcrpiRegistrationInfos)))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@DefaultValue("0") @QueryParam("from") int from,
        @DefaultValue("50") @QueryParam("size") int size) {
        Response errorResponse = validateParameters(from, size);
        if (errorResponse != null)
            return errorResponse;
        List<MCRPIRegistrationInfo> mcrpiRegistrationInfos = MCRPIManager.getInstance().getList(from,
            size);
        return Response.status(Response.Status.OK)
            .entity(new Gson().toJson(new MCRPIListJSON(null, from, size,
                MCRPIManager.getInstance().getCount(), mcrpiRegistrationInfos)))
            .build();
    }

    @GET
    @Path("service")
    @Produces(MediaType.TEXT_PLAIN)
    public Response listServices() {
        return Response
            .status(Response.Status.OK)
            .entity(MCRPIServiceManager
                .getInstance()
                .getServiceIDList()
                .stream()
                .collect(Collectors.joining(",")))
            .build();
    }

    @POST
    @Path("service/{serviceName}/{mycoreId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@PathParam("serviceName") String serviceName, @PathParam("mycoreId") String mycoreId,
        @DefaultValue("") @QueryParam("additional") String additional) {

        if (!MCRPIServiceManager.getInstance().getServiceIDList().contains(serviceName)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(buildErrorJSON("No Registration Service found for " + serviceName)).build();
        }

        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(serviceName);
        MCRObjectID mycoreIDObject;
        try {
            mycoreIDObject = MCRObjectID.getInstance(mycoreId);
        } catch (MCRException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(buildErrorJSON("The provided id " + mycoreId + " seems to be invalid!", e)).build();
        }

        MCRPersistentIdentifier identifier;
        MCRBase object = MCRMetadataManager.retrieve(mycoreIDObject);
        try {
            identifier = registrationService.register(object, additional, true);
        } catch (MCRPersistentIdentifierException | MCRActiveLinkException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(buildErrorJSON("Error while register new identifier!", e)).build();
        } catch (MCRAccessException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.FORBIDDEN)
                .entity(buildErrorJSON("Error while register new identifier!", e)).build();
        }

        return Response.status(Response.Status.CREATED).entity(buildIdentifierObject(identifier)).build();
    }

    private Response validateParameters(int from, int size) {
        if (from < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(buildErrorJSON("From must be positive (" + from + ")"))
                .build();
        }

        if (size <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(buildErrorJSON("Count must be larger then 0 (" + size + ")"))
                .build();
        }

        if (size > COUNT_LIMIT) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    buildErrorJSON(String.format(Locale.ROOT, "Count can't be larger then %d (%d)", COUNT_LIMIT, size)))
                .build();
        }
        return null;
    }

    private String buildErrorJSON(String message) {
        return new Gson().toJson(new MCRPIErrorJSON(message));
    }

    private String buildErrorJSON(String message, Exception e) {
        return new Gson().toJson(new MCRPIErrorJSON(message, e));
    }

    private String buildIdentifierObject(MCRPersistentIdentifier pi) {
        return new Gson().toJson(new MCRPIRegistrationJSON(pi.asString()));
    }

}
