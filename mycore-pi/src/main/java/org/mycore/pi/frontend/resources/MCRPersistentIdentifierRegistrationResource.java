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

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPIRegistrationServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.frontend.model.MCRPIErrorJSON;
import org.mycore.pi.frontend.model.MCRPIListJSON;
import org.mycore.pi.frontend.model.MCRPIRegistrationJSON;

import com.google.gson.Gson;

@Path("pi/registration")
public class MCRPersistentIdentifierRegistrationResource {

    public static final int COUNT_LIMIT = 100;

    @GET
    @Path("type/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listByType(@PathParam("type") String type, @DefaultValue("0") @QueryParam("from") int from, @DefaultValue("50") @QueryParam("size") int size) {
        Response errorResponse = validateParameters(from, size);
        if (errorResponse != null) return errorResponse;
        List<MCRPIRegistrationInfo> mcrpiRegistrationInfos = MCRPersistentIdentifierManager.getList(type, from, size);
        return Response.status(Response.Status.OK)
                .entity(new Gson().toJson(new MCRPIListJSON(type, from, size, MCRPersistentIdentifierManager.getCount(type), mcrpiRegistrationInfos)))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@DefaultValue("0") @QueryParam("from") int from, @DefaultValue("50") @QueryParam("size") int size) {
        Response errorResponse = validateParameters(from, size);
        if (errorResponse != null) return errorResponse;
        List<MCRPIRegistrationInfo> mcrpiRegistrationInfos = MCRPersistentIdentifierManager.getList(from, size);
        return Response.status(Response.Status.OK)
                .entity(new Gson().toJson(new MCRPIListJSON(null, from, size, MCRPersistentIdentifierManager.getCount(), mcrpiRegistrationInfos)))
                .build();
    }

    @GET
    @Path("service")
    @Produces(MediaType.TEXT_PLAIN)
    public Response listServices() {
        return Response
                .status(Response.Status.OK)
                .entity(MCRPIRegistrationServiceManager.getInstance().getServiceList().stream().collect(Collectors.joining(",")))
                .build();
    }

    @POST
    @Path("service/{serviceName}/{mycoreId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@PathParam("serviceName") String serviceName, @PathParam("mycoreId") String mycoreId, @DefaultValue("") @QueryParam("additional") String additional) {

        if (!MCRPIRegistrationServiceManager.getInstance().getServiceList().contains(serviceName)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(buildErrorJSON("No Registration Service found for " + serviceName)).build();
        }

        MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = MCRPIRegistrationServiceManager.getInstance().getRegistrationService(serviceName);
        MCRObjectID mycoreIDObject;
        try {
            mycoreIDObject = MCRObjectID.getInstance(mycoreId);
        } catch (MCRException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(buildErrorJSON("The provided id " + mycoreId + " seems to be invalid!", e)).build();
        }

        MCRPersistentIdentifier identifier;
        MCRObject object = MCRMetadataManager.retrieveMCRObject(mycoreIDObject);
        try {
            identifier = registrationService.fullRegister(object, additional);
        } catch (MCRPersistentIdentifierException|MCRActiveLinkException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(buildErrorJSON("Error while register new identifier!", e)).build();
        } catch (MCRAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(buildErrorJSON("Error while register new identifier!", e)).build();
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
                    .entity(buildErrorJSON(String.format(Locale.ROOT, "Count can't be larger then %d (%d)", COUNT_LIMIT, size)))
                    .build();
        }
        return null;
    }

    private String buildErrorJSON(String message) {
        return new Gson().toJson(new MCRPIErrorJSON(message));
    }

    private String buildErrorJSON(String message, Exception e) {
        return new Gson().toJson(new MCRPIErrorJSON(message + e.toString()));
    }

    private String buildIdentifierObject(MCRPersistentIdentifier pi) {
        return new Gson().toJson(new MCRPIRegistrationJSON(pi.asString()));
    }


}
