/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.classeditor.resources;

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRClassificationUtils;
import org.mycore.frontend.classeditor.access.MCRClassificationWritePermission;
import org.mycore.frontend.classeditor.access.MCRNewClassificationPermission;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;
import org.mycore.frontend.classeditor.json.MCRJSONCategoryHelper;
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.classification.MCRSolrClassificationUtil;
import org.mycore.solr.search.MCRSolrSearchUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

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
    private static final MCRCategoryDAO CATEGORY_DAO = MCRCategoryDAOFactory.obtainInstance();

    private static final MCRCategLinkService CATEG_LINK_SERVICE = MCRCategLinkServiceFactory.obtainInstance();

    protected static final MCRSolrAuthenticationManager SOLR_AUTHENTICATION_MANAGER =
        MCRSolrAuthenticationManager.obtainInstance();

    @Context
    UriInfo uriInfo;

    /**
     * @param rootidStr
     *            rootID.categID
     */
    @GET
    @Path("{rootidStr}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("rootidStr") String rootidStr) {
        if (rootidStr == null || Objects.equals(rootidStr, "")) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategoryID id = new MCRCategoryID(rootidStr);
        return getCategory(id);
    }

    /**
     * @param rootidStr
     *            rootID.categID
     */
    @GET
    @Path("{rootidStr}/{categidStr}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("rootidStr") String rootidStr, @PathParam("categidStr") String categidStr) {

        if (rootidStr == null || Objects.equals(rootidStr, "") || categidStr == null
            || Objects.equals(categidStr, "")) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategoryID id = new MCRCategoryID(rootidStr, categidStr);
        return getCategory(id);
    }

    @GET
    @Path("newID/{rootID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String newIDJson(@PathParam("rootID") String rootID) {
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        return gson.toJson(newRandomUUID(rootID));
    }

    @GET
    @Path("newID")
    @MCRRestrictedAccess(MCRNewClassificationPermission.class)
    @Produces(MediaType.APPLICATION_JSON)
    public String newRootIDJson() {
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        return gson.toJson(newRootID());
    }

    @GET
    @Path("export/{rootidStr}")
    @Produces(MediaType.APPLICATION_XML)
    public String export(@PathParam("rootidStr") String rootidStr) {
        if (rootidStr == null || Objects.equals(rootidStr, "")) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        String classAsString = MCRClassificationUtils.asString(rootidStr);
        if (classAsString == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return classAsString;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassification() {
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        List<MCRCategory> rootCategories = new ArrayList<>(CATEGORY_DAO.getRootCategories());
        rootCategories.removeIf(
            category -> !MCRAccessManager.checkPermission(category.getId().getRootID(), PERMISSION_WRITE));
        if (rootCategories.isEmpty()
            && !MCRAccessManager.checkPermission(MCRClassificationUtils.CREATE_CLASS_PERMISSION)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        Map<MCRCategoryID, Boolean> linkMap = CATEG_LINK_SERVICE.checkForLinks(null);
        String json = gson.toJson(new MCRCategoryListWrapper(rootCategories, linkMap));
        return Response.ok(json).build();
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteCateg(String json) {
        MCRJSONCategory category = parseJson(json);
        DeleteOp deleteOp = new DeleteOp(category);
        deleteOp.run();
        return deleteOp.getResponse();
    }

    @POST
    @Path("save")
    @MCRRestrictedAccess(MCRClassificationWritePermission.class)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(String json) {
        JsonStreamParser jsonStreamParser = new JsonStreamParser(json);
        if (jsonStreamParser.hasNext()) {
            JsonArray saveObjArray = jsonStreamParser.next().getAsJsonArray();
            List<JsonObject> saveList = new ArrayList<>();
            for (JsonElement jsonElement : saveObjArray) {
                saveList.add(jsonElement.getAsJsonObject());
            }
            saveList.sort(new IndexComperator());
            for (JsonObject jsonObject : saveList) {
                String status = getStatus(jsonObject);
                SaveElement categ = getCateg(jsonObject);
                MCRJSONCategory parsedCateg = parseJson(categ.getJson());
                if (Objects.equals(status, "update")) {
                    new UpdateOp(parsedCateg, jsonObject).run();
                } else if (Objects.equals(status, "delete")) {
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
    public Response importClassification(@FormDataParam("classificationFile") InputStream uploadedInputStream) {
        try {
            MCRClassificationUtils.fromStream(uploadedInputStream);
        } catch (MCRAccessException accessExc) {
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception exc) {
            throw new WebApplicationException(exc);
        }
        // This is a hack to support iframe loading via ajax.
        // The benefit is to load file input form data without reloading the page.
        // Maybe its better to create a separate method importClassificationIFrame.
        // @see http://livedocs.dojotoolkit.org/dojo/io/iframe - Additional Information
        return Response.ok("<html><body><textarea>200</textarea></body></html>").build();
    }

    @GET
    @Path("filter/{text}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response filter(@PathParam("text") String text) {
        SolrClient solrClient = MCRSolrClassificationUtil.getCore().getClient();
        ModifiableSolrParams p = new ModifiableSolrParams();
        p.set("q", "*" + text + "*");
        p.set("fl", "id,ancestors");

        JsonArray docList = new JsonArray();
        MCRSolrSearchUtils.stream(solrClient, p).flatMap(document -> {
            List<String> ids = new ArrayList<>();
            ids.add(document.getFirstValue("id").toString());
            Collection<Object> fieldValues = document.getFieldValues("ancestors");
            if (fieldValues != null) {
                for (Object anc : fieldValues) {
                    ids.add(anc.toString());
                }
            }
            return ids.stream();
        }).distinct().map(JsonPrimitive::new).forEach(docList::add);
        return Response.ok().entity(docList.toString()).build();
    }

    @GET
    @Path("link/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveLinkedObjects(@PathParam("id") String id, @QueryParam("start") Integer start,
        @QueryParam("rows") Integer rows) throws SolrServerException, IOException {
        // do solr query
        SolrClient solrClient = MCRSolrCoreManager.getMainSolrClient();
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("start", start != null ? start : 0);
        params.set("rows", rows != null ? rows : 50);
        params.set("fl", "id");
        String configQuery = MCRConfiguration2.getString("MCR.Solr.linkQuery").orElse("category.top:{0}");
        String query = new MessageFormat(configQuery, Locale.ROOT).format(new String[] { id.replaceAll(":", "\\\\:") });
        params.set("q", query);
        QueryRequest queryRequest = new QueryRequest(params);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(queryRequest, MCRSolrAuthenticationLevel.SEARCH);
        QueryResponse solrResponse = queryRequest.process(solrClient);

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
        return new MCRCategoryID(uuid);
    }

    private MCRCategoryID newRandomUUID(String rootID) {
        String newRootID = rootID;
        if (rootID == null) {
            newRootID = UUID.randomUUID().toString();
        }
        return new MCRCategoryID(newRootID, UUID.randomUUID().toString());
    }

    private String getCategory(MCRCategoryID id) {
        if (!CATEGORY_DAO.exist(id)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        MCRCategory category = CATEGORY_DAO.getCategory(id, 1);
        if (!(category instanceof MCRJSONCategory)) {
            category = new MCRJSONCategory(category);
        }
        Gson gson = MCRJSONManager.obtainInstance().createGson();

        return gson.toJson(category);
    }

    private SaveElement getCateg(JsonElement jsonElement) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject categ = jsonObject.get("item").getAsJsonObject();
        JsonElement parentID = jsonObject.get("parentId");
        JsonElement position = jsonObject.get("index");
        boolean hasParent = false;

        if (parentID != null && !parentID.toString().contains("_placeboid_") && position != null) {
            categ.add(MCRJSONCategoryHelper.PROP_PARENTID, parentID);
            categ.add(MCRJSONCategoryHelper.PROP_POSITION, position);
            hasParent = true;
        }

        return new SaveElement(categ.toString(), hasParent);
    }

    private String getStatus(JsonElement jsonElement) {
        return jsonElement.getAsJsonObject().get("state").getAsString();
    }

    private boolean isAdded(JsonElement jsonElement) {
        JsonElement added = jsonElement.getAsJsonObject().get("added");
        return added != null && jsonElement.getAsJsonObject().get("added").getAsBoolean();
    }

    private MCRJSONCategory parseJson(String json) {
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        return gson.fromJson(json, MCRJSONCategory.class);
    }

    protected String buildJsonError(String errorType, MCRCategoryID mcrCategoryID) {
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        JsonObject error = new JsonObject();
        error.addProperty("type", errorType);
        error.addProperty("rootid", mcrCategoryID.getRootID());
        error.addProperty("categid", mcrCategoryID.getId());
        return gson.toJson(error);
    }

    private static class SaveElement {
        private String categJson;

        private boolean hasParent;

        SaveElement(String categJson, boolean hasParent) {
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

    private static final class IndexComperator implements Comparator<JsonElement> {
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
                return Integer.compare(depthLevel1.getAsInt(), depthLevel2.getAsInt());
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
            return Integer.compare(index1.getAsInt(), index2.getAsInt());
        }
    }

    interface OperationInSession {
        void run();
    }

    private static class DeleteOp implements OperationInSession {

        private MCRJSONCategory category;

        private Response response;

        DeleteOp(MCRJSONCategory category) {
            this.category = category;
        }

        @Override
        public void run() {
            MCRCategoryID categoryID = category.getId();
            if (CATEGORY_DAO.exist(categoryID)) {
                if (categoryID.isRootID()
                    && !MCRAccessManager.checkPermission(categoryID.getRootID(), PERMISSION_DELETE)) {
                    throw new WebApplicationException(Status.UNAUTHORIZED);
                }
                CATEGORY_DAO.deleteCategory(categoryID);
                setResponse(Response.status(Status.GONE).build());
            } else {
                setResponse(Response.notModified().build());
            }
        }

        public Response getResponse() {
            return response;
        }

        private void setResponse(Response response) {
            this.response = response;
        }

    }

    private class UpdateOp implements OperationInSession {

        private MCRJSONCategory category;

        private JsonObject jsonObject;

        UpdateOp(MCRJSONCategory category, JsonObject jsonObject) {
            this.category = category;
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            MCRCategoryID mcrCategoryID = category.getId();
            boolean isAdded = isAdded(jsonObject);
            if (isAdded && MCRCategoryDAOFactory.obtainInstance().exist(mcrCategoryID)) {
                // an added category already exist -> throw conflict error
                throw new WebApplicationException(
                    Response.status(Status.CONFLICT).entity(buildJsonError("duplicateID", mcrCategoryID)).build());
            }

            MCRCategoryID newParentID = category.getParentID();
            if (newParentID != null && !CATEGORY_DAO.exist(newParentID)) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }
            if (CATEGORY_DAO.exist(category.getId())) {
                CATEGORY_DAO.setLabels(category.getId(), category.getLabels());
                CATEGORY_DAO.setURI(category.getId(), category.getURI());
                if (newParentID != null) {
                    CATEGORY_DAO.moveCategory(category.getId(), newParentID, category.getPositionInParent());
                }
            } else {
                CATEGORY_DAO.addCategory(newParentID, category.asMCRImpl(), category.getPositionInParent());
            }
        }

    }
}
