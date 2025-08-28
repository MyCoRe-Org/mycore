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

package org.mycore.frontend.classeditor.json;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.frontend.classeditor.mocks.CategoryDAOMock;
import org.mycore.test.MyCoReTest;

import com.google.gson.Gson;

@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.DefaultLang", string = "de"),
        @MCRTestProperty(key = "MCR.Category.DAO", classNameOf = CategoryDAOMock.class)
    })
@MyCoReTest
public class MCRCategoryJsonTest {
    @Test
    public void deserialize() throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(getClass().getResourceAsStream("/classi/categoryJsonErr.xml"));
        String json = doc.getRootElement().getText();

        Gson gson = MCRJSONManager.obtainInstance().createGson();
        gson.fromJson(json, MCRCategoryImpl.class);
    }
}
