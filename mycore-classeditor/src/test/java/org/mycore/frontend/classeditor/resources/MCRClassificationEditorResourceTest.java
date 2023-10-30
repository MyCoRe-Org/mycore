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

package org.mycore.frontend.classeditor.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.MCRTestCase;
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
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;
import org.mycore.frontend.jersey.resources.MCRJerseyTestFeature;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class MCRClassificationEditorResourceTest extends MCRTestCase {

    private MCRJerseyTestFeature jersey;

    private CategoryDAOMock categDAO;

    static Logger LOGGER = LogManager.getLogger(MCRClassificationEditorResourceTest.class);

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> map = super.getTestProperties();
        map.put("MCR.Metadata.Store.BaseDir", "/tmp");
        map.put("MCR.Metadata.Store.SVNBase", "/tmp/versions");
        map.put("MCR.IFS2.Store.ClasseditorTempStore.BaseDir", "jimfs:");
        map.put("MCR.IFS2.Store.ClasseditorTempStore.SlotLayout", "4-2-2");
        map.put("MCR.EventHandler.MCRObject.2.Class",
            "org.mycore.datamodel.common.MCRXMLMetadataEventHandler");
        map.put("MCR.Persistence.LinkTable.Store.Class", LinkTableStoreMock.class.getName());
        map.put("MCR.Category.DAO", CategoryDAOMock.class.getName());
        map.put("MCR.Category.LinkService", CategoryLinkServiceMock.class.getName());
        map.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        map.put("MCR.Access.Cache.Size", "200");
        return map;
    }

    @Before
    public void init() throws Exception {
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(
            MCRClassificationEditorResource.class,
            MCRSessionHookFilter.class,
            MultiPartFeature.class));

        MCRJSONManager mg = MCRJSONManager.instance();
        mg.registerAdapter(new MCRCategoryTypeAdapter());
        mg.registerAdapter(new MCRCategoryIDTypeAdapter());
        mg.registerAdapter(new MCRLabelSetTypeAdapter());
        mg.registerAdapter(new MCRCategoryListTypeAdapter());

        try {
            MCRStoreManager.createStore("ClasseditorTempStore", MCRMetadataStore.class);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("while creating store ClasseditorTempStore", e);
        }

        try {
            /* its important to set the daomock via this method because the factory could be called
             * by a previous test. In this case a class cast exception occur because MCRCategoryDAOImpl
             * was loaded. */
            MCRCategoryDAOFactory.set(CategoryDAOMock.class);
            categDAO = (CategoryDAOMock) MCRCategoryDAOFactory.getInstance();
            categDAO.init();
        } catch (Exception exc) {
            fail();
        }
    }

    @After
    public void cleanUp() throws Exception {
        MCRStoreManager.removeStore("ClasseditorTempStore");
        this.jersey.tearDown();
    }

    @Test
    public void getRootCategories() {
        final String categoryJsonStr = jersey.target("classifications").request().get(String.class);
        MCRCategoryListWrapper categListWrapper = MCRJSONManager.instance().createGson().fromJson(categoryJsonStr,
            MCRCategoryListWrapper.class);
        List<MCRCategory> categList = categListWrapper.getList();
        assertEquals("Wrong number of root categories.", 2, categList.size());
    }

    @Test
    public void getSingleCategory() {
        Collection<MCRCategory> categs = categDAO.getCategs();
        for (MCRCategory mcrCategory : categs) {
            MCRCategoryID id = mcrCategory.getId();
            String path = id.getRootID();
            String categID = id.getID();
            if (categID != null && !categID.isEmpty()) {
                path = path + "/" + categID;
            }
            String categoryJsonStr = jersey.target("/classifications/" + path).request().get(String.class);
            MCRJSONCategory retrievedCateg = MCRJSONManager.instance().createGson().fromJson(categoryJsonStr,
                MCRJSONCategory.class);
            String errorMsg = new MessageFormat("We want to retrieve the category {0} but it was {1}", Locale.ROOT)
                .format(new Object[] { id, retrievedCateg.getId() });
            assertEquals(errorMsg, id, retrievedCateg.getId());
        }
    }

    @Test
    public void saveClassification() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_OneClassification.xml"));
        String json = doc.getRootElement().getText();
        Response response = jersey.target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClassiWithSub() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_ClassiSub.xml"));
        String json = doc.getRootElement().getText();
        Response response = jersey.target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClass2ndSub() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_Classi2Sub.xml"));
        String json = doc.getRootElement().getText();
        Response response = jersey.target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void saveClass2ndSubJsonErr() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/classiEditor_Classi2Sub_JsonErr.xml"));
        String json = doc.getRootElement().getText();
        Response response = jersey.target("/classifications/save").request().post(Entity.json(json));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void forceException() {
        String json = """
                [{
                  "item":{"id":{"rootid":"rootID_01","categid":"categ_01"}},
                  "state":"update",
                  "parentId":{"rootid":"rootID_02"},
                  "depthLevel":3,
                  "index":-1
                }]
            """;
        Response response = jersey.target("/classifications/save").request().post(Entity.json(json));
        assertEquals(500, response.getStatus());
    }

}
