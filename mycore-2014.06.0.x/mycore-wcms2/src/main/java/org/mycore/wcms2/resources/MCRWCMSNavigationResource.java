package org.mycore.wcms2.resources;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.wcms2.access.MCRWCMSPermission;
import org.mycore.wcms2.datamodel.MCRNavigation;
import org.mycore.wcms2.navigation.MCRWCMSContentManager;
import org.mycore.wcms2.navigation.MCRWCMSNavigationManager;
import org.mycore.wcms2.navigation.MCRWCMSNavigationProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;

@Path("wcms/navigation")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSNavigationResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        try {
            JsonObject json = MCRWCMSNavigationManager.getNavigationAsJSON();
            return json.toString();
        } catch (Exception exc) {
            throw new WebApplicationException(exc, Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("content")
    @Produces(MediaType.APPLICATION_JSON)
    public String getContent(@QueryParam("webpagePath") String webpagePath) throws Exception {
        JsonObject json = getContentManager().getContent(webpagePath);
        return json.toString();
    }

    @POST
    @Path("save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response save(String json) throws Exception {
        JsonStreamParser jsonStreamParser = new JsonStreamParser(json);
        if (!jsonStreamParser.hasNext()) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        JsonObject saveObject = jsonStreamParser.next().getAsJsonObject();
        // get navigation
        MCRNavigation newNavigation = MCRWCMSNavigationManager.fromJSON(saveObject);
        // save navigation
        MCRWCMSNavigationManager.save(newNavigation);
        // save content
        JsonArray items = saveObject.get(MCRWCMSNavigationProvider.JSON_ITEMS).getAsJsonArray();
        getContentManager().save(items);
        return Response.ok().build();
    }

    @GET
    @Path("templates")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTemplates(@Context ServletContext servletContext) throws Exception {
        String templatePath = MCRConfiguration.instance().getString("MCR.WCMS2.templatePath", "/templates/master/");
        Set<String> resourcePaths = servletContext.getResourcePaths(templatePath);
        JsonArray returnArr = new JsonArray();
        if (resourcePaths != null) {
            for (String resourcepath : resourcePaths) {
                resourcepath = resourcepath.substring(templatePath.length(), resourcepath.length() - 1);
                returnArr.add(new JsonPrimitive(resourcepath));
            }
        }
        return returnArr.toString();
    }

    @POST
    @Path("move")
    public void move(@QueryParam("from") String from, @QueryParam("to") String to) throws Exception {
        if (from == null || to == null) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                .entity("from or to parameter not set").build());
        }
        // move it
        getContentManager().move(from, to);

        // update navigation
        MCRNavigation navigation = MCRWCMSNavigationManager.getNavigation();
        boolean hrefUpdated = MCRWCMSNavigationManager.updateHref(navigation, from, to);
        if (hrefUpdated) {
            MCRWCMSNavigationManager.save(navigation);
        }
    }

    protected MCRWCMSContentManager getContentManager() {
        return new MCRWCMSContentManager();
    }

}
