package org.mycore.frontend.classeditor.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRJSONManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.frontend.classeditor.MCRCategoryIDTypeAdapter;
import org.mycore.frontend.classeditor.MCRCategoryListTypeAdapter;
import org.mycore.frontend.classeditor.MCRCategoryTypeAdapter;
import org.mycore.frontend.classeditor.MCRLabelSetTypeAdapter;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;
import org.mycore.frontend.classeditor.mocks.CategoryDAOMock;
import org.mycore.frontend.classeditor.mocks.CategoryLinkServiceMock;
import org.mycore.frontend.classeditor.mocks.LinkTableStoreMock;
import org.mycore.frontend.classeditor.utils.MCRCategUtils;
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;
import org.mycore.frontend.jersey.MCRJerseyResourceTest;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;

public class MCRClassificationEditorResourceTest extends MCRJerseyResourceTest {
    private CategoryDAOMock categDAO;

    static Logger LOGGER = Logger.getLogger(MCRClassificationEditorResourceTest.class);

    @BeforeClass
    public static void setup() {
        System.out.println("\n\n\n");
        System.setProperty("MCR.Configuration.File", "config/test.properties");
        Properties mcrProperties = MCRConfiguration.instance().getProperties();
        mcrProperties.setProperty("MCR.Metadata.Type.jpclassi", "true");
        mcrProperties.setProperty("MCR.Metadata.Store.BaseDir", "/tmp");
        mcrProperties.setProperty("MCR.Metadata.Store.SVNBase", "/tmp/versions");
        mcrProperties.setProperty("MCR.IFS2.Store.jportal_jpclassi.ForceXML", "true");
        mcrProperties.setProperty("MCR.IFS2.Store.jportal_jpclassi.BaseDir", "ram:///tmp");
        mcrProperties.setProperty("MCR.IFS2.Store.jportal_jpclassi.SlotLayout", "4-2-2");
        mcrProperties.setProperty("MCR.IFS2.Store.jportal_jpclassi.SVNRepositoryURL", "ram:///tmp");
        mcrProperties.setProperty("MCR.EventHandler.MCRObject.2.Class",
            "org.mycore.datamodel.common.MCRXMLMetadataEventHandler");
        mcrProperties.setProperty("MCR.Persistence.LinkTable.Store.Class", LinkTableStoreMock.class.getName());
        mcrProperties.setProperty("MCR.Category.DAO", CategoryDAOMock.class.getName());
        mcrProperties.setProperty("ClassificationResouce.useSession", "false");
        mcrProperties.setProperty("MCR.Category.LinkService", CategoryLinkServiceMock.class.getName());
        mcrProperties.setProperty("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
    }

    @Before
    public void init() {
        MCRJSONManager mg = MCRJSONManager.instance();
        mg.registerAdapter(new MCRCategoryTypeAdapter());
        mg.registerAdapter(new MCRCategoryIDTypeAdapter());
        mg.registerAdapter(new MCRLabelSetTypeAdapter());
        mg.registerAdapter(new MCRCategoryListTypeAdapter());

        try {
            MCRStoreManager.createStore("jportal_jpclassi", MCRMetadataStore.class);
        } catch (InstantiationException e) {
            LOGGER.error("while creating store jportal_jpclassi", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("while creating store jportal_jpclassi", e);
        }

        try {
            /* its important to set the daomock via this method because the factory could be called
             * by a previous test. In this case a class cast exception occur because MCRCategoryDAOImpl
             * was loaded. */
            MCRCategoryDAOFactory.set(CategoryDAOMock.class);
            categDAO = (CategoryDAOMock) MCRCategoryDAOFactory.getInstance();
            categDAO.init();
        } catch (Exception exc) {
            assertTrue(false);
        }
    }

    @After
    public void cleanUp() {
        MCRStoreManager.removeStore("jportal_jpclassi");
    }

    @Test
    public void getRootCategories() throws Exception {
        ClientResponse jsonResponse = resource().path("/classifications").type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
        String categoryJsonStr = jsonResponse.getEntity(String.class);
        MCRCategoryListWrapper categListWrapper = MCRJSONManager.instance().createGson()
            .fromJson(categoryJsonStr, MCRCategoryListWrapper.class);
        List<MCRCategory> categList = categListWrapper.getList();

        assertEquals("Wrong number of root categories.", 2, categList.size());
    }

    @Test
    public void getSingleCategory() throws Exception {
        Set<MCRCategoryID> ids = categDAO.getIds();
        Collection<MCRCategory> categs = categDAO.getCategs();

        for (MCRCategory mcrCategory : categs) {
            MCRCategoryID id = mcrCategory.getId();
            String path = id.getRootID();
            String categID = id.getID();
            if (categID != null && !"".equals(categID)) {
                path = path + "/" + categID;
            }

            ClientResponse jsonResponse = resource().path("/classifications/" + path).type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
            String categoryJsonStr = jsonResponse.getEntity(String.class);
            MCRJSONCategory retrievedCateg = MCRJSONManager.instance().createGson()
                .fromJson(categoryJsonStr, MCRJSONCategory.class);
            String errorMsg = MessageFormat.format("We want to retrieve the category {0} but it was {1}", id,
                retrievedCateg.getId());
            assertTrue(errorMsg, id.equals(retrievedCateg.getId()));
        }
    }

    //    @Test
    //    public void newRootCategory() throws Exception {
    //        ClientResponse response = resource().path("/classifications/new").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, categJsonStr());
    //        assertEquals("could not create rubric: ", Status.CREATED.getStatusCode(), response.getClientResponseStatus().getStatusCode());
    //        String jsonResponse = resource().uri(response.getLocation()).type(MediaType.APPLICATION_JSON).get(String.class);
    //        JsonParser parser = new JsonParser();
    //        JsonElement jsonElement = parser.parse(jsonResponse);
    //        jsonElement.getAsJsonObject().remove(MCRJSONCategoryPropName.ID);
    //        assertEquals(categJsonStr(), jsonElement.toString());
    //    }

    //    @Test
    //    public void newSubCategory() throws Exception {
    //        Set<MCRCategoryID> ids = categDAO.getIds();
    //        MCRCategoryID id = ids.iterator().next();
    //        MCRCategory category = categDAO.getCategory(id, 1);
    //        int childNum = category.getChildren().size();
    //        
    //        String idStr = id.getRootID();
    //        if(!id.isRootID()){
    //            idStr = idStr + "/" + id.getID();
    //        }
    //
    //        ClientResponse response = resource().path("/classifications/" + idStr + "/new").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, categJsonStr());
    //        assertEquals("could not create rubric: ", Status.CREATED.getStatusCode(), response.getClientResponseStatus().getStatusCode());
    //        assertEquals(childNum++, categDAO.getCategory(id, 1).getChildren().size());
    //        
    //        String jsonResponse = resource().uri(response.getLocation()).type(MediaType.APPLICATION_JSON).get(String.class);
    //        JsonParser parser = new JsonParser();
    //        JsonElement jsonElement = parser.parse(jsonResponse);
    //        jsonElement.getAsJsonObject().remove(MCRJSONCategoryPropName.ID);
    //        assertEquals(categJsonStr(), jsonElement.toString());
    //    }

    //    @Test
    //    public void createClassification() throws Exception {
    //        String serializedRubric = rubricJsonStr();
    //        URI rubricLocation = assertCreateRubric(serializedRubric);
    //        assertRootCategs();
    //        
    //        assertGetRubric(rubricLocation, serializedRubric);
    //        assertUpdateRubric(rubricLocation);
    //        URI subRubricLocation = assertCreateSubRubric(rubricLocation);
    //        String rootID = getQueryMap(rubricLocation).get("rootID");
    //        String responseStr = resource().path("/classifications/children").queryParam("rootID", rootID).type(MediaType.APPLICATION_JSON).get(String.class);
    //        assertDeleteRubric(rubricLocation, subRubricLocation);
    //    }

    @Test
    public void saveClassification() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_OneClassification.xml"));
        String json = doc.getRootElement().getText();
        ClientResponse response = resource().path("/classifications/save").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClassiWithSub() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_ClassiSub.xml"));
        String json = doc.getRootElement().getText();
        ClientResponse response = resource().path("/classifications/save").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClass2ndSub() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_Classi2Sub.xml"));
        String json = doc.getRootElement().getText();
        ClientResponse response = resource().path("/classifications/save").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClass2ndSubJsonErr() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_Classi2Sub_JsonErr.xml"));
        String json = doc.getRootElement().getText();
        ClientResponse response = resource().path("/classifications/save").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void assertRootCategs() {
        Set<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel("de", "Rubriken Test 2 fuer MyCoRe", "test de"));
        labels.add(new MCRLabel("en", "Rubric test 2 for MyCoRe", "test en"));
        MCRCategory category = MCRCategUtils.newCategory(null, labels, null);
        Gson gson = MCRJSONManager.instance().createGson();
        String serializedRubric = gson.toJson(category);
        ClientResponse response = resource().path("/classifications").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, serializedRubric);
        assertEquals("could not create rubric: ", Status.CREATED.getStatusCode(), response.getClientResponseStatus()
            .getStatusCode());
        String responseStr = resource().path("/classifications").type(MediaType.APPLICATION_JSON).get(String.class);
        List<MCRCategory> rootCategories = MCRCategoryDAOFactory.getInstance().getRootCategories();
        String rootsCategsStr = gson.toJson(rootCategories);
        assertEquals(rootsCategsStr, responseStr);
    }

    private void assertDeleteRubric(URI rubricLocation, URI subRubricLocation) {
        ClientResponse deleteResponse = resource().uri(rubricLocation).delete(ClientResponse.class);
        assertEquals("Wrong HTTP status code,", Status.GONE.getStatusCode(), deleteResponse.getStatus());
        ClientResponse delRubricResponse = resource().uri(rubricLocation).type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), delRubricResponse.getStatus());
        ClientResponse delSubRubricResponse = resource().uri(subRubricLocation).type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), delSubRubricResponse.getStatus());
    }

    private void assertUpdateRubric(URI rubricLocation) {
        ClientResponse jsonResponse = resource().uri(rubricLocation).type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
        String serializedRubric = jsonResponse.getEntity(String.class);
        String modifiedRubric = serializedRubric.replaceAll("MyCoRe", "Jportal");
        ClientResponse updateResponse = resource().uri(rubricLocation).type(MediaType.APPLICATION_JSON)
            .put(ClientResponse.class, modifiedRubric);
        assertEquals(Status.OK.getStatusCode(), updateResponse.getStatus());
        String modJsonResponse = resource().uri(rubricLocation).type(MediaType.APPLICATION_JSON).get(String.class);
        assertEquals(modifiedRubric, modJsonResponse);
    }

    private void assertGetRubric(URI rubricLocation, String serializedRubric) {
        ClientResponse jsonResponse = resource().uri(rubricLocation).type(MediaType.APPLICATION_JSON)
            .get(ClientResponse.class);
        String categoryJsonStr = jsonResponse.getEntity(String.class);
        assertEquals(serializedRubric, removeCategID(categoryJsonStr));
    }

    private String removeCategID(String categoryJsonStr) {
        Gson gson = MCRJSONManager.instance().createGson();
        MCRJSONCategory receivedCategory = gson.fromJson(categoryJsonStr, MCRJSONCategory.class);
        receivedCategory.setId(null);
        String responseBody = gson.toJson(receivedCategory);
        return responseBody;
    }

    private String categJsonStr() {
        Set<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel("de", "Rubriken Test fuer MyCoRe", "test de"));
        labels.add(new MCRLabel("en", "Rubric test for MyCoRe", "test en"));
        MCRCategory category = MCRCategUtils.newCategory(null, labels, null);
        Gson gson = MCRJSONManager.instance().createGson();
        String serializedRubric = gson.toJson(category);
        return serializedRubric;
    }

    private URI assertCreateRubric(String serializedRubric) {
        ClientResponse response = resource().path("/classifications").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, serializedRubric);
        assertEquals("could not create rubric: ", Status.CREATED.getStatusCode(), response.getClientResponseStatus()
            .getStatusCode());
        return response.getLocation();
    }

    private URI assertCreateSubRubric(URI parentURI) {
        ClientResponse response = resource().path("/classifications").type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class, subRubricStr(parentURI));
        assertEquals("could not create sub rubric: ", Status.CREATED.getStatusCode(), response
            .getClientResponseStatus().getStatusCode());
        return response.getLocation();
    }

    private String subRubricStr(URI parentURI) {
        Map<String, String> queryMap = getQueryMap(parentURI);

        String rootID = queryMap.get("rootID");
        String categID = queryMap.get("categID");

        Set<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel("de", "Unterrubriken Test fuer MyCoRe", "untertest de"));
        labels.add(new MCRLabel("en", "Subrubric test for MyCoRe", "subtest en"));
        MCRCategory subCateg = MCRCategUtils.newCategory(MCRCategoryID.rootID(rootID), labels, new MCRCategoryID(
            rootID, categID));
        Gson gson = MCRJSONManager.instance().createGson();
        String serializedRubric = gson.toJson(subCateg);
        return serializedRubric;
    }

    private Map<String, String> getQueryMap(URI parentURI) {
        HashMap<String, String> queryMap = new HashMap<String, String>();
        String queryParams = parentURI.getQuery();

        for (String queryParam : queryParams.split("&")) {
            String[] queryPair = queryParam.split("=");
            queryMap.put(queryPair[0], queryPair[1]);
        }

        return queryMap;
    }

    @Override
    protected String[] getPackageName() {
        return new String[] { MCRClassificationEditorResource.class.getPackage().getName() };
    }
}
