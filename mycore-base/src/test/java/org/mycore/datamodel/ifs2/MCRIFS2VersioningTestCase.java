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

package org.mycore.datamodel.ifs2;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class MCRIFS2VersioningTestCase extends MCRIFS2TestCase {
    @Rule
    public TemporaryFolder versionBaseDir = new TemporaryFolder();

    private MCRVersioningMetadataStore versStore;

    @Override
    protected void createStore() throws Exception {
        setVersStore(MCRStoreManager.createStore(STORE_ID, MCRVersioningMetadataStore.class));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.out.println("teardown: " + getVersStore().repURL);
        MCRStoreManager.removeStore(STORE_ID);
        VFS.getManager().resolveFile(getVersStore().repURL.getPath()).delete(Selectors.SELECT_ALL);
    }

    public void setVersStore(MCRVersioningMetadataStore versStore) {
        this.versStore = versStore;
    }

    public MCRVersioningMetadataStore getVersStore() {
        return versStore;
    }

    @Override
    public MCRStore getGenericStore() {
        return getVersStore();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        try {
            String url = versionBaseDir.getRoot().toURI().toURL().toString();
            testProperties.put("MCR.IFS2.Store.TEST.SVNRepositoryURL", url);
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }
        return testProperties;
    }
}
