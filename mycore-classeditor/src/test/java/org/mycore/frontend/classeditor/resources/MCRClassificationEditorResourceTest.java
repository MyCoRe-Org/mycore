package org.mycore.frontend.classeditor.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRJSONManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
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
import org.mycore.frontend.classeditor.wrapper.MCRCategoryListWrapper;
import org.mycore.frontend.jersey.resources.MCRJerseyTest;

public class MCRClassificationEditorResourceTest extends MCRJerseyTest {
    private CategoryDAOMock categDAO;

    static Logger LOGGER = LogManager.getLogger(MCRClassificationEditorResourceTest.class);

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> map = super.getTestProperties();
        map.put("MCR.Metadata.Type.jpclassi", "true");
        map.put("MCR.Metadata.Store.BaseDir", "/tmp");
        map.put("MCR.Metadata.Store.SVNBase", "/tmp/versions");
        map.put("MCR.IFS2.Store.jportal_jpclassi.ForceXML", "true");
        map.put("MCR.IFS2.Store.jportal_jpclassi.BaseDir", "ram:///tmp");
        map.put("MCR.IFS2.Store.jportal_jpclassi.SlotLayout", "4-2-2");
        map.put("MCR.IFS2.Store.jportal_jpclassi.SVNRepositoryURL", "ram:///tmp");
        map.put("MCR.EventHandler.MCRObject.2.Class",
            "org.mycore.datamodel.common.MCRXMLMetadataEventHandler");
        map.put("MCR.Persistence.LinkTable.Store.Class", LinkTableStoreMock.class.getName());
        map.put("MCR.Category.DAO", CategoryDAOMock.class.getName());
        map.put("ClassificationResouce.useSession", "false");
        map.put("MCR.Category.LinkService", CategoryLinkServiceMock.class.getName());
        map.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        return map;
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

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }

    @BeforeClass
    public static void register() {
        JERSEY_CLASSES.add(MCRClassificationEditorResource.class);
        JERSEY_CLASSES.add(MultiPartFeature.class);
    }

    @Test
    public void getRootCategories() throws Exception {
        final String categoryJsonStr = target("classifications").request().get(String.class);
        MCRCategoryListWrapper categListWrapper = MCRJSONManager.instance().createGson().fromJson(categoryJsonStr,
            MCRCategoryListWrapper.class);
        List<MCRCategory> categList = categListWrapper.getList();
        assertEquals("Wrong number of root categories.", 2, categList.size());
    }

    @Test
    public void getSingleCategory() throws Exception {
        Collection<MCRCategory> categs = categDAO.getCategs();
        for (MCRCategory mcrCategory : categs) {
            MCRCategoryID id = mcrCategory.getId();
            String path = id.getRootID();
            String categID = id.getID();
            if (categID != null && !"".equals(categID)) {
                path = path + "/" + categID;
            }
            String categoryJsonStr = target("/classifications/" + path).request().get(String.class);
            MCRJSONCategory retrievedCateg = MCRJSONManager.instance().createGson().fromJson(categoryJsonStr,
                MCRJSONCategory.class);
            String errorMsg = MessageFormat.format("We want to retrieve the category {0} but it was {1}", id,
                retrievedCateg.getId());
            assertTrue(errorMsg, id.equals(retrievedCateg.getId()));
        }
    }

    @Test
    public void saveClassification() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_OneClassification.xml"));
        String json = doc.getRootElement().getText();
        Response response = target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClassiWithSub() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_ClassiSub.xml"));
        String json = doc.getRootElement().getText();
        Response response = target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClass2ndSub() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_Classi2Sub.xml"));
        String json = doc.getRootElement().getText();
        Response response = target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClass2ndSubJsonErr() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_Classi2Sub_JsonErr.xml"));
        String json = doc.getRootElement().getText();
        Response response = target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

}
