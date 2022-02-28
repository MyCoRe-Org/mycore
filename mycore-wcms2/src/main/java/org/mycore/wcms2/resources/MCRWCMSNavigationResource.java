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

package org.mycore.wcms2.resources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.wcms2.access.MCRWCMSPermission;
import org.mycore.wcms2.datamodel.MCRNavigation;
import org.mycore.wcms2.navigation.MCRWCMSContentManager;
import org.mycore.wcms2.navigation.MCRWCMSNavigationProvider;
import org.mycore.wcms2.navigation.MCRWCMSNavigationUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("wcms/navigation")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSNavigationResource {

    private static final XPathExpression<Element> TEMPLATE_PATH;

    static {
        TEMPLATE_PATH = XPathFactory.instance().compile("*[@template]", Filters.element());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        try {
            JsonObject json = MCRWCMSNavigationUtils.getNavigationAsJSON();
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
        MCRNavigation newNavigation = MCRWCMSNavigationUtils.fromJSON(saveObject);
        // save navigation
        MCRWCMSNavigationUtils.save(newNavigation);
        // save content
        JsonArray items = saveObject.get(MCRWCMSNavigationProvider.JSON_ITEMS).getAsJsonArray();
        getContentManager().save(items);
        return Response.ok().build();
    }

    /**
     * Returns a json object containing all available templates. 
     */
    @GET
    @Path("templates")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTemplates(@Context ServletContext servletContext) throws Exception {
        // templates of navigation.xml
        Document xml = MCRWCMSNavigationUtils.getNavigationAsXML();
        List<Element> elementList = TEMPLATE_PATH.evaluate(xml);
        HashSet<String> entries = elementList.stream()
            .map(e -> e.getAttributeValue("template"))
            .collect(Collectors.toCollection(HashSet::new));

        // templates by folder
        String templatePath = MCRConfiguration2.getString("MCR.WCMS2.templatePath").orElse("/templates/master/");
        Set<String> resourcePaths = servletContext.getResourcePaths(templatePath);
        if (resourcePaths != null) {
            for (String resourcepath : resourcePaths) {
                String newResourcepath = resourcepath.substring(templatePath.length(), resourcepath.length() - 1);
                entries.add(newResourcepath);
            }
        }

        // create returning json
        JsonArray returnArr = new JsonArray();
        for (String entry : entries) {
            returnArr.add(new JsonPrimitive(entry));
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
        MCRNavigation navigation = MCRWCMSNavigationUtils.getNavigation();
        boolean hrefUpdated = MCRWCMSNavigationUtils.updateHref(navigation, from, to);
        if (hrefUpdated) {
            MCRWCMSNavigationUtils.save(navigation);
        }
    }

    protected MCRWCMSContentManager getContentManager() {
        return new MCRWCMSContentManager();
    }

}
