package org.mycore.frontend.classeditor.resources;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;
import org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName;
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * This class is responsible for CRUD-operations of MCRCategories. It accepts
 * JSON objects of the form <code>
 * [{    "ID":{"rootID":"abcd","categID":"1234"}
 *      "label":[
 *          {"lang":"de","text":"Rubriken Test 2 fuer MyCoRe","descriptions":"test de"},
 *          {"lang":"en","text":"Rubric test 2 for MyCoRe","descriptions":"test en"}
 *      ],
 *      "parentID":{"rootID":"abcd","categID":"parent"}
 *      "children:"URL"
 * 
 * }
 * ...
 * ]
 * </code>
 * 
 * @author chi
 * 
 */
@Path("classifications")
public class MCRClassificationEditorResource {
    static Logger LOGGER = Logger.getLogger(MCRClassificationEditorResource.class);

    private MCRCategoryDAO categoryDAO = null;

    @Context
    UriInfo uriInfo;

    private MCRCategLinkService linkService;

    /**
     * @param rootidStr
     *            rootID.categID
     * @return
     */
    @GET
    @Path("{rootidStr}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("rootidStr") String rootidStr) {
        if (rootidStr == null || "".equals(rootidStr)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategoryID id = MCRCategoryID.rootID(rootidStr);
        return getCategory(id);
    }

    /**
     * @param rootidStr
     *            rootID.categID
     * @return
     */
    @GET
    @Path("{rootidStr}/{categidStr}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("rootidStr") String rootidStr, @PathParam("categidStr") String categidStr) {

        if (rootidStr == null || "".equals(rootidStr) || categidStr == null || "".equals(categidStr)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategoryID id = new MCRCategoryID(rootidStr, categidStr);
        return getCategory(id);
    }

    @GET
    @Path("newID/{rootID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String newIDJson(@PathParam("rootID") String rootID) {
        Gson gson = MCRJSONManager.instance().createGson();
        return gson.toJson(newRandomUUID(rootID));
    }

    @GET
    @Path("newID")
    @Produces(MediaType.APPLICATION_JSON)
    public String newRootIDJson() {
        Gson gson = MCRJSONManager.instance().createGson();
        return gson.toJson(newRootID());
    }

    @GET
    @Path("export/{rootidStr}")
    @Produces(MediaType.APPLICATION_XML)
    public String export(@PathParam("rootidStr") String rootidStr) {
        if (rootidStr == null || "".equals(rootidStr)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        MCRCategoryID classId = MCRCategoryID.rootID(rootidStr);
        MCRCategory classification = MCRCategoryDAOFactory.getInstance().getRootCategory(classId, -1);
        if(classification == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        try {
            Document jdom = MCRCategoryTransformer.getMetaDataDocument(classification, true);
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            StringWriter wr = new StringWriter();
            out.output(jdom, wr);
            return wr.toString();
        } catch(Exception exc) {
            LOGGER.error("while export classification " + rootidStr, exc);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getClassification() {
        Gson gson = MCRJSONManager.instance().createGson();
        List<MCRCategory> rootCategories = getCategoryDAO().getRootCategories();
        Map<MCRCategoryID, Boolean> linkMap = getLinkService().hasLinks(null);
        String json = gson.toJson(new MCRCategoryListWrapper(rootCategories, linkMap));
        return json;
    }

    @DELETE
    @RolesAllowed("")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteCateg(String json) {
        MCRJSONCategory category = parseJson(json);

        try {
            if (getCategoryDAO().exist(category.getId())) {
                getCategoryDAO().deleteCategory(category.getId());
                return Response.status(Status.GONE).build();
            } else {
                return Response.notModified().build();
            }
        } catch (MCRPersistenceException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("save")
    @RolesAllowed("")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(String json) {      
        JsonStreamParser jsonStreamParser = new JsonStreamParser(json);
        if (jsonStreamParser.hasNext()) {
            JsonArray saveObjArray = jsonStreamParser.next().getAsJsonArray();
            List<JsonObject> saveList = new ArrayList<JsonObject>();
            for (JsonElement jsonElement : saveObjArray) {
                saveList.add(jsonElement.getAsJsonObject());
            }
            Collections.sort(saveList, new IndexComperator());
            for(JsonObject jsonObject : saveList) {
                String status = getStatus(jsonObject);
                SaveElement categ = getCateg(jsonObject);
                MCRJSONCategory parsedCateg = parseJson(categ.getJson());
                if ("update".equals(status)) {
                    updateCateg(parsedCateg);
                } else if ("delete".equals(status)) {
                    deleteCateg(categ.getJson());
                } else {
                    return Response.status(Status.BAD_REQUEST).build();
                }
            }
//            Status.CONFLICT
            return Response.status(Status.OK).build();
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("import")
    @RolesAllowed("")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public String importClassification(
            @FormDataParam("classificationFile") InputStream uploadedInputStream,
            @FormDataParam("classificationFile") FormDataContentDisposition fileDetail) {
        try {
            Document jdom = MCRXMLParserFactory.getParser().parseXML(new MCRStreamContent(uploadedInputStream));
            MCRCategory classification = MCRXMLTransformer.getCategory(jdom);
            MCRCategoryDAOFactory.getInstance().addCategory(null, classification);
        } catch(Exception exc) {
            LOGGER.error("while import classification", exc);
            return "<html><body><textarea>error</textarea></body></html>";
        }
        // This is a hack to support iframe loading via ajax.
        // The benefit is to load file input form data without reloading the page.
        // Maybe its better to create a separate method importClassificationIFrame.
        // @see http://livedocs.dojotoolkit.org/dojo/io/iframe - Additional Information
        return "<html><body><textarea>200</textarea></body></html>";
    }

    protected MCRCategoryDAO getCategoryDAO() {
        if (categoryDAO == null) {
            categoryDAO = MCRCategoryDAOFactory.getInstance();
        }
        return categoryDAO;
    }
    
    protected MCRCategoryID newRootID() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return MCRCategoryID.rootID(uuid);
    }

    private URI buildGetURI(MCRCategoryID categoryID) {
        UriBuilder uriBuilder = UriBuilder.fromUri(uriInfo.getBaseUri());
        uriBuilder.path(this.getClass());
        uriBuilder.path(categoryID.getRootID());
        String categID = categoryID.getID();
        if (categID != null && !"".equals(categID)) {
            uriBuilder.path(categID);
        }

        return uriBuilder.build();
    }

    private MCRCategoryID newRandomUUID(String rootID) {
        if (rootID == null) {
            rootID = UUID.randomUUID().toString();
        }
        return new MCRCategoryID(rootID, UUID.randomUUID().toString());
    }

    private String getCategory(MCRCategoryID id) {
        if (!getCategoryDAO().exist(id)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategory category = getCategoryDAO().getCategory(id, 1);
        if (!(category instanceof MCRJSONCategory)) {
            category = new MCRJSONCategory(category);
        }
        Gson gson = MCRJSONManager.instance().createGson();

        String json = gson.toJson(category);
        return json;
    }
    
    protected MCRCategLinkService getLinkService() {
        if (linkService == null) {
            try {
                linkService = (MCRCategLinkService) MCRConfiguration.instance().getInstanceOf("Category.Link.Service");
            } catch (MCRConfigurationException e) {
                linkService = MCRCategLinkServiceFactory.getInstance();
            }
        }
        return linkService;
    }

    private SaveElement getCateg(JsonElement jsonElement) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject categ = jsonObject.get("item").getAsJsonObject();
        JsonElement parentID = jsonObject.get("parentId");
        JsonElement position = jsonObject.get("index");
        boolean hasParent = false;

        if (parentID != null && !parentID.toString().contains("_placeboid_") && position != null) {
            categ.add(MCRJSONCategoryPropName.PARENTID, parentID);
            categ.add(MCRJSONCategoryPropName.POSITION, position);
            hasParent = true;
        }

        return new SaveElement(categ.toString(), hasParent);
    }

    private String getStatus(JsonElement jsonElement) {
        return jsonElement.getAsJsonObject().get("state").getAsString();
    }

    private MCRJSONCategory parseJson(String json) {
        Gson gson = MCRJSONManager.instance().createGson();
        MCRJSONCategory category = gson.fromJson(json, MCRJSONCategory.class);
        return category;
    }

    protected Response updateCateg(MCRJSONCategory categ) {
        MCRCategoryID newParentID = categ.getParentID();
        if (newParentID != null && !getCategoryDAO().exist(newParentID)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        Response response = null;
        if (getCategoryDAO().exist(categ.getId())) {
            Set<MCRLabel> labels = categ.getLabels();
            for (MCRLabel mcrLabel : labels) {
                getCategoryDAO().setLabel(categ.getId(), mcrLabel);
            }
            getCategoryDAO().setURI(categ.getId(), categ.getURI());
            if (newParentID != null) {
                getCategoryDAO().moveCategory(categ.getId(), newParentID, categ.getPositionInParent());
            }
            response = Response.status(Status.OK).build();
        } else {
            getCategoryDAO().addCategory(newParentID, categ.asMCRImpl());
            URI uri = buildGetURI(categ.getId());
            response = Response.created(uri).build();
        }
        return response;
    }

    private static class SaveElement {
        private String categJson;

        private boolean hasParent;

        public SaveElement(String categJson, boolean hasParent) {
            this.setCategJson(categJson);
            this.setHasParent(hasParent);
        }

        private void setHasParent(boolean hasParent) {
            this.hasParent = hasParent;
        }

        public boolean hasParent() {
            return hasParent;
        }

        private void setCategJson(String categJson) {
            this.categJson = categJson;
        }

        public String getJson() {
            return categJson;
        }
    }

    private static class IndexComperator implements Comparator<JsonElement> {
        @Override
        public int compare(JsonElement jsonElement1, JsonElement jsonElement2) {
            if(!jsonElement1.isJsonObject()) {
                return 1;
            }
            if(!jsonElement2.isJsonObject()) {
                return -1;
            }
            // compare level first
            JsonPrimitive depthLevel1 = jsonElement1.getAsJsonObject().getAsJsonPrimitive("depthLevel");
            JsonPrimitive depthLevel2 = jsonElement2.getAsJsonObject().getAsJsonPrimitive("depthLevel");
            if(depthLevel1 == null) {
                return 1;
            }
            if(depthLevel2 == null) {
                return -1;
            }
            if(depthLevel1.getAsInt() != depthLevel2.getAsInt()) {
                return new Integer(depthLevel1.getAsInt()).compareTo(depthLevel2.getAsInt());
            }
            // compare index            
            JsonPrimitive index1 = jsonElement1.getAsJsonObject().getAsJsonPrimitive("index");
            JsonPrimitive index2 = jsonElement2.getAsJsonObject().getAsJsonPrimitive("index");
            if(index1 == null) {
                return 1;
            }
            if(index2 == null) {
                return -1;
            }
            return new Integer(index1.getAsInt()).compareTo(index2.getAsInt());
        }
    }

}
