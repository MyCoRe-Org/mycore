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

package org.mycore.datamodel.ifs2.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;

public class MCRStoreManagerTest {
    @Test
    public void createMCRFileStore() throws Exception {
        MCRJimfsTestStoreConfig config = new MCRJimfsTestStoreConfig(MCRStoreManagerTest.class.getSimpleName());
        MCRFileStore fileStore = MCRStoreManager.createStore(config, MCRFileStore.class);

        assertNotNull(fileStore, "MCRStoreManager could not create Filestore.");
    }

}
