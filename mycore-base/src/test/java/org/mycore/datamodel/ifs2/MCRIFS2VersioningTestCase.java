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

package org.mycore.datamodel.ifs2;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRIFS2VersioningTestCase extends MCRIFS2TestCase {

    @TempDir
    public File versionBaseDir;

    private MCRVersioningMetadataStore versStore;

    @Override
    protected void createStore() throws Exception {
        setVersStore(MCRStoreManager.createStore(STORE_ID, MCRVersioningMetadataStore.class));
    }

    @BeforeEach
    public void setUp() throws Exception {
        try {
            String url = versionBaseDir.toURI().toURL().toString();
            MCRConfiguration2.set("MCR.IFS2.Store.TEST.SVNRepositoryURL", url);
        } catch (MalformedURLException e) {
            throw new MCRException(e);
        }
        super.setUp();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        System.out.println("teardown: " + getVersStore().repURL);
        MCRStoreManager.removeStore(STORE_ID);
        versionBaseDir.delete();
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

}
