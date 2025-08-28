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

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.config.MCRConfiguration2;

public abstract class MCRIFS2TestCase {

    protected static final String STORE_ID = "TEST";

    @TempDir
    public File storeBaseDir;

    private MCRFileStore store;

    protected void createStore() throws Exception {
        System.out.println(getClass() + " store id: " + STORE_ID);
        setStore(MCRStoreManager.createStore(STORE_ID, MCRFileStore.class));
    }

    @BeforeEach
    public void setUp() throws Exception {
        MCRConfiguration2.set("MCR.IFS2.Store.TEST.BaseDir", storeBaseDir.getAbsolutePath());
        MCRConfiguration2.set("MCR.IFS2.Store.TEST.SlotLayout", "4-2-2");
        createStore();
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRStoreManager.removeStore(STORE_ID);
        assertNull(MCRStoreManager.getStore(STORE_ID), "Could not remove store " + STORE_ID);
        store = null;
    }

    public void setStore(MCRFileStore store) {
        this.store = store;
    }

    public MCRFileStore getStore() {
        return store;
    }

    public MCRStore getGenericStore() {
        return getStore();
    }

    /**
     * Waits 1,1 seconds and does nothing
     */
    protected void bzzz() {
        synchronized (this) {
            try {
                wait(1100);
            } catch (InterruptedException e) {
            }
        }
    }

}
