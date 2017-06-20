package org.mycore.pi.frontend.resources;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.pi.MCRPersistentIdentifierManager;

import com.google.gson.Gson;

@Path("pi/resolve")
public class MCRPersistentIdentifierResolvingResource {

    @GET
    @Path("{identifier:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolve(@PathParam("identifier") String identifier) {
        HashMap<String, List<String>> resolveMap = new HashMap<>();
        MCRPersistentIdentifierManager.getInstance().getResolvers().forEach(resolver -> {
            MCRPersistentIdentifierManager
                .getInstance().get(identifier)
                .forEach(mcrPersistentIdentifier -> {
                    resolveMap.put(resolver.getName(),
                        resolver.resolveSuppress(mcrPersistentIdentifier).collect(Collectors.toList()));
                });
        });
        return Response.ok().entity(new Gson().toJson(resolveMap)).build();
    }
}
