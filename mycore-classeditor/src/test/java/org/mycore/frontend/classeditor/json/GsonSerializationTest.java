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

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.frontend.classeditor.mocks.CategoryDAOMock;

public class GsonSerializationTest {
    @Before
    public void init() {
        final MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        MCRConfigurationBase.initialize(configurationLoader.loadDeprecated(), configurationLoader.load(), true);
        MCRConfiguration2.set("MCR.Category.DAO", CategoryDAOMock.class.getName());
    }

    protected MCRCategoryImpl createCateg(String rootID, String id2, String text) {
        MCRCategoryImpl mcrCategoryImpl = new MCRCategoryImpl();
        MCRCategoryID id = new MCRCategoryID(rootID, id2);
        mcrCategoryImpl.setId(id);
        SortedSet<MCRLabel> labels = new TreeSet<>();
        labels.add(new MCRLabel("de", text + "_de", "desc_" + text + "_de"));
        labels.add(new MCRLabel("en", text + "_en", "desc_" + text + "_en"));
        mcrCategoryImpl.setLabels(labels);
        return mcrCategoryImpl;
    }

}
