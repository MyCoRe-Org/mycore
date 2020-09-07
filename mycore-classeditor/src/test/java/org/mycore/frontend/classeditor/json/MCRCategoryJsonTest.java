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

package org.mycore.frontend.classeditor.json;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.frontend.classeditor.mocks.CategoryDAOMock;

import com.google.gson.Gson;

public class MCRCategoryJsonTest {
    @Test
    public void deserialize() throws Exception {
        final MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        MCRConfigurationBase.initialize(configurationLoader.loadDeprecated(), configurationLoader.load(), true);
        MCRConfiguration2.set("MCR.Metadata.DefaultLang", "de");
        MCRConfiguration2.set("MCR.Category.DAO", CategoryDAOMock.class.getName());

        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/categoryJsonErr.xml"));
        String json = doc.getRootElement().getText();

        Gson gson = MCRJSONManager.instance().createGson();
        try {
            gson.fromJson(json, MCRCategoryImpl.class);
            System.out.println("FOO");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
