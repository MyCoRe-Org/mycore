package org.mycore.frontend.classeditor.resources;

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.frontend.classeditor.access.MCRClassificationWritePermission;
import org.mycore.frontend.classeditor.access.MCRNewClassificationPermission;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;
import org.mycore.frontend.classeditor.json.MCRJSONCategoryPropName;
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.solr.MCRSolrServerFactory;
import org.xml.sax.SAXParseException;

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
    private static final Logger LOGGER = Logger.getLogger(MCRClassificationEditorResource.class);

    private static final MCRCategoryDAO CATEGORY_DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRCategLinkService CATEG_LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    @Context
    UriInfo uriInfo;

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
    @MCRRestrictedAccess(MCRNewClassificationPermission.class)
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
        if (classification == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        try {
            Document jdom = MCRCategoryTransformer.getMetaDataDocument(classification, true);
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            StringWriter wr = new StringWriter();
            out.output(jdom, wr);
            return wr.toString();
        } catch (Exception exc) {
            LOGGER.error("while export classification " + rootidStr, exc);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassification() {
        Gson gson = MCRJSONManager.instance().createGson();
        List<MCRCategory> rootCategories = new LinkedList<>(CATEGORY_DAO.getRootCategories());
        for (Iterator<MCRCategory> it = rootCategories.iterator(); it.hasNext();) {
            MCRCategory category = it.next();
            if (!MCRAccessManager.checkPermission(category.getId().getRootID(), PERMISSION_WRITE)) {
                it.remove();
            }
        }
        if (rootCategories.isEmpty() && !MCRAccessManager.checkPermission(MCRNewClassificationPermission.PERMISSION)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        Map<MCRCategoryID, Boolean> linkMap = CATEG_LINK_SERVICE.hasLinks(null);
        String json = gson.toJson(new MCRCategoryListWrapper(rootCategories, linkMap));
        return Response.ok(json).build();
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteCateg(String json) {
        MCRJSONCategory category = parseJson(json);
        if (!MCRSessionMgr.getCurrentSession().isTransactionActive()) {
            MCRSessionMgr.getCurrentSession().beginTransaction();
        }
        try {
            MCRCategoryID categoryID = category.getId();
            if (CATEGORY_DAO.exist(categoryID)) {
                if (categoryID.isRootID()
                    && !MCRAccessManager.checkPermission(categoryID.getRootID(), PERMISSION_DELETE)) {
                    throw new WebApplicationException(Status.UNAUTHORIZED);
                }
                CATEGORY_DAO.deleteCategory(categoryID);
                return Response.status(Status.GONE).build();
            } else {
                return Response.notModified().build();
            }
        } catch (MCRPersistenceException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND).build();
        } finally {
            MCRSessionMgr.getCurrentSession().commitTransaction();
        }
    }

    @POST
    @Path("save")
    @MCRRestrictedAccess(MCRClassificationWritePermission.class)
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
            for (JsonObject jsonObject : saveList) {
                String status = getStatus(jsonObject);
                boolean isAdded = isAdded(jsonObject);
                SaveElement categ = getCateg(jsonObject);
                MCRJSONCategory parsedCateg = parseJson(categ.getJson());
                if ("update".equals(status)) {
                    MCRCategoryID mcrCategoryID = parsedCateg.getId();
                    if (isAdded && MCRCategoryDAOFactory.getInstance().exist(mcrCategoryID)) {
                        // an added category already exist -> throw conflict error
                        return Response.status(Status.CONFLICT).entity(buildJsonError("duplicateID", mcrCategoryID))
                            .build();
                    }
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response importClassification(@FormDataParam("classificationFile") InputStream uploadedInputStream,
        @FormDataParam("classificationFile") FormDataContentDisposition fileDetail) {
        MCRCategory classification;
        try {
            Document jdom = MCRXMLParserFactory.getParser().parseXML(new MCRStreamContent(uploadedInputStream));
            classification = MCRXMLTransformer.getCategory(jdom);
        } catch (SAXParseException | URISyntaxException e) {
            throw new WebApplicationException(e);
        }
        if (CATEGORY_DAO.exist(classification.getId())) {
            if (!MCRAccessManager.checkPermission(classification.getId().getRootID(), PERMISSION_WRITE)) {
                return Response.status(Status.UNAUTHORIZED).build();
            }
            CATEGORY_DAO.replaceCategory(classification);
        } else {
            if (!MCRAccessManager.checkPermission(MCRNewClassificationPermission.PERMISSION)) {
                return Response.status(Status.UNAUTHORIZED).build();
            }
            CATEGORY_DAO.addCategory(null, classification);
        }
        // This is a hack to support iframe loading via ajax.
        // The benefit is to load file input form data without reloading the page.
        // Maybe its better to create a separate method importClassificationIFrame.
        // @see http://livedocs.dojotoolkit.org/dojo/io/iframe - Additional Information
        return Response.ok("<html><body><textarea>200</textarea></body></html>").build();
    }

    @GET
    @Path("link/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveLinkedObjects(@PathParam("id") String id, @QueryParam("start") Integer start,
        @QueryParam("rows") Integer rows) throws SolrServerException, UnsupportedEncodingException {
        // do solr query
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("start", start != null ? start : 0);
        params.set("rows", rows != null ? rows : 50);
        params.set("fl", "id");
        String configQuery = MCRConfiguration.instance().getString("MCR.Module-solr.linkQuery", "category.top:{0}");
        String query = MessageFormat.format(configQuery, id.replaceAll(":", "\\\\:"));
        params.set("q", query);
        QueryResponse solrResponse = solrServer.query(params);
        SolrDocumentList solrResults = solrResponse.getResults();
        // build json response
        JsonObject response = new JsonObject();
        response.addProperty("numFound", solrResults.getNumFound());
        response.addProperty("start", solrResults.getStart());
        JsonArray docList = new JsonArray();
        for (SolrDocument doc : solrResults) {
            docList.add(new JsonPrimitive((String) doc.getFieldValue("id")));
        }
        response.add("docs", docList);
        return Response.ok().entity(response.toString()).build();
    }

    protected MCRCategoryID newRootID() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return MCRCategoryID.rootID(uuid);
    }

    private MCRCategoryID newRandomUUID(String rootID) {
        if (rootID == null) {
            rootID = UUID.randomUUID().toString();
        }
        return new MCRCategoryID(rootID, UUID.randomUUID().toString());
    }

    private String getCategory(MCRCategoryID id) {
        if (!CATEGORY_DAO.exist(id)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategory category = CATEGORY_DAO.getCategory(id, 1);
        if (!(category instanceof MCRJSONCategory)) {
            category = new MCRJSONCategory(category);
        }
        Gson gson = MCRJSONManager.instance().createGson();

        String json = gson.toJson(category);
        return json;
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

    private boolean isAdded(JsonElement jsonElement) {
        JsonElement added = jsonElement.getAsJsonObject().get("added");
        return added == null ? false : jsonElement.getAsJsonObject().get("added").getAsBoolean();
    }

    private MCRJSONCategory parseJson(String json) {
        Gson gson = MCRJSONManager.instance().createGson();
        MCRJSONCategory category = gson.fromJson(json, MCRJSONCategory.class);
        return category;
    }

    protected void updateCateg(MCRJSONCategory categ) {
        if (!MCRSessionMgr.getCurrentSession().isTransactionActive()) {
            MCRSessionMgr.getCurrentSession().beginTransaction();
        }
        try {
            MCRCategoryID newParentID = categ.getParentID();
            if (newParentID != null && !CATEGORY_DAO.exist(newParentID)) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }
            if (CATEGORY_DAO.exist(categ.getId())) {
                CATEGORY_DAO.setLabels(categ.getId(), categ.getLabels());
                CATEGORY_DAO.setURI(categ.getId(), categ.getURI());
                if (newParentID != null) {
                    CATEGORY_DAO.moveCategory(categ.getId(), newParentID, categ.getPositionInParent());
                }
            } else {
                CATEGORY_DAO.addCategory(newParentID, categ.asMCRImpl(), categ.getPositionInParent());
            }
        } finally {
            MCRSessionMgr.getCurrentSession().commitTransaction();
        }
    }

    protected String buildJsonError(String errorType, MCRCategoryID mcrCategoryID) {
        Gson gson = MCRJSONManager.instance().createGson();
        JsonObject error = new JsonObject();
        error.addProperty("type", errorType);
        error.addProperty("rootid", mcrCategoryID.getRootID());
        error.addProperty("categid", mcrCategoryID.getID());
        return gson.toJson(error);
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

        @SuppressWarnings("unused")
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
            if (!jsonElement1.isJsonObject()) {
                return 1;
            }
            if (!jsonElement2.isJsonObject()) {
                return -1;
            }
            // compare level first
            JsonPrimitive depthLevel1 = jsonElement1.getAsJsonObject().getAsJsonPrimitive("depthLevel");
            JsonPrimitive depthLevel2 = jsonElement2.getAsJsonObject().getAsJsonPrimitive("depthLevel");
            if (depthLevel1 == null) {
                return 1;
            }
            if (depthLevel2 == null) {
                return -1;
            }
            if (depthLevel1.getAsInt() != depthLevel2.getAsInt()) {
                return new Integer(depthLevel1.getAsInt()).compareTo(depthLevel2.getAsInt());
            }
            // compare index            
            JsonPrimitive index1 = jsonElement1.getAsJsonObject().getAsJsonPrimitive("index");
            JsonPrimitive index2 = jsonElement2.getAsJsonObject().getAsJsonPrimitive("index");
            if (index1 == null) {
                return 1;
            }
            if (index2 == null) {
                return -1;
            }
            return new Integer(index1.getAsInt()).compareTo(index2.getAsInt());
        }
    }

}
