package org.mycore.wcms2.resources;

import java.io.File;
import java.io.IOException;
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
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.wcms2.MCRWCMSUtil;
import org.mycore.wcms2.access.MCRWCMSPermission;
import org.mycore.wcms2.datamodel.MCRNavigation;
import org.mycore.wcms2.navigation.MCRWCMSDefaultNavigationProvider;
import org.mycore.wcms2.navigation.MCRWCMSNavigationProvider;
import org.mycore.wcms2.navigation.MCRWCMSContentManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;

@Path("wcms/navigation")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSNavigationResource {

    private static final Logger LOGGER = Logger.getLogger(MCRWCMSNavigationResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        try {
            JsonObject json = getNavigationProvider().toJSON(getNavigation());
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
        MCRNavigation newNavigation = getNavigationProvider().fromJSON(saveObject);
        // save navigation
        MCRWCMSUtil.save(newNavigation, getNavigationFile());
        // save content
        JsonArray items = saveObject.get(MCRWCMSNavigationProvider.JSON_ITEMS).getAsJsonArray();
        getContentManager().save(items);
        return Response.ok().build();
    }

    @GET
    @Path("templates")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTemplates(@Context ServletContext servletContext) throws Exception {
        String templatePath = getConfig().getString("MCR.WCMS2.templatePath", "/templates/master/");
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

    protected File getNavigationFile() {
        String pathToNavigation = getConfig().getString("MCR.navigationFile");
        return new File(pathToNavigation);
    }

    protected MCRNavigation getNavigation() throws IOException, ParserConfigurationException, SAXException, JAXBException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = docBuilder.parse(getNavigationFile());
        if (doc.getElementsByTagName("menu").getLength() == 0) {
            NodeList nodeList = doc.getFirstChild().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    ((org.w3c.dom.Element) nodeList.item(i)).setAttribute("id", nodeList.item(i).getNodeName());
                    doc.renameNode(nodeList.item(i), null, "menu");
                }
            }
        }
        return MCRWCMSUtil.load(doc);
    }

    protected MCRWCMSNavigationProvider getNavigationProvider() {
        Object navProvider = getConfig().getInstanceOf("MCR.WCMS2.navigationProvider", MCRWCMSDefaultNavigationProvider.class.getName());
        if (!(navProvider instanceof MCRWCMSNavigationProvider)) {
            LOGGER.error("MCR.WCMS2.navigationProvider is not an instance of NavigationProvider");
            return null;
        }
        return (MCRWCMSNavigationProvider) navProvider;
    }

    protected MCRWCMSContentManager getContentManager() {
        return new MCRWCMSContentManager();
    }

    protected static MCRConfiguration getConfig() {
        return MCRConfiguration.instance();
    }

}
