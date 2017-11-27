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

package org.mycore.common;

import java.nio.file.Path;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

public abstract class MCRStoreTestCase extends MCRJPATestCase {

    private static MCRXMLMetadataManager store;

    @Rule
    public TemporaryFolder storeBaseDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder svnBaseDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        store = MCRXMLMetadataManager.instance();
        store.reload();
    }

    public Path getStoreBaseDir() {
        return storeBaseDir.getRoot().toPath();
    }

    public Path getSvnBaseDir() {
        return svnBaseDir.getRoot().toPath();
    }

    public static MCRXMLMetadataManager getStore() {
        return store;
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Store.BaseDir", storeBaseDir.getRoot().getAbsolutePath());
        testProperties.put("MCR.Metadata.Store.SVNBase", svnBaseDir.getRoot().toURI().toString());
        return testProperties;
    }

}
