package org.mycore.frontend.classeditor.json;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.frontend.classeditor.mocks.CategoryDAOMock;

import com.google.gson.Gson;

public class MCRCategoryJsonTest {
    @Test
    public void deserialize() throws Exception {
        MCRConfiguration mcrProperties = MCRConfiguration.instance();
        mcrProperties.initialize(MCRConfigurationLoaderFactory.getConfigurationLoader().load(), true);
        mcrProperties.set("MCR.Metadata.DefaultLang", "de");
        mcrProperties.set("MCR.Category.DAO", CategoryDAOMock.class.getName());

        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/categoryJsonErr.xml"));
        String json = doc.getRootElement().getText();

        Gson gson = MCRJSONManager.instance().createGson();
        try {
            MCRCategoryImpl fromJson = gson.fromJson(json, MCRCategoryImpl.class);
            System.out.println("FOO");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
