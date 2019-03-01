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

package org.mycore.datamodel.ifs2.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;
import org.mycore.datamodel.ifs2.MCRStoreAlreadyExistsException;
import org.mycore.datamodel.ifs2.MCRStoreCenter;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class MCRStoreCenterTest {
    @Before
    public void init() {
        MCRStoreCenter.instance().clear();
    }

    @Test
    public void heapOp() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.instance();

        FakeStoreConfig config = new FakeStoreConfig("heapOp");
        storeHeap.addStore(config.getID(), new FakeStore(config));
        String storeID = config.getID();
        FakeStore fakeStore = storeHeap.getStore(storeID);

        assertNotNull("The store should be not null.", fakeStore);
        assertEquals("Fake Store", fakeStore.getMsg());

        assertTrue("Could not find store with ID: " + storeID, storeHeap.getCurrentStores(FakeStore.class)
            .filter(s -> storeID.equals(s.getID()))
            .findAny().isPresent());

        assertTrue("Could not remove store with ID: " + storeID, storeHeap.removeStore(storeID));
        assertNull("There should be no store with ID: " + storeID, storeHeap.<FakeStore> getStore(storeID));
    }

    @Test(expected = MCRStoreAlreadyExistsException.class)
    public void addStoreTwice() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.instance();
        FakeStoreConfig config = new FakeStoreConfig("addStoreTwice");
        FakeStore fakeStore = new FakeStore(config);
        storeHeap.addStore(config.getID(), fakeStore);
        storeHeap.addStore(config.getID(), fakeStore);
    }

    class FakeStoreConfig implements MCRStoreConfig {
        private final Path baseDir;

        public FakeStoreConfig(String id) throws IOException {
            String fsName = MCRStoreCenterTest.class.getSimpleName() + "." + id;
            URI jimfsURI = URI.create("jimfs://" + fsName);
            FileSystem fileSystem;
            try {
                fileSystem = FileSystems.getFileSystem(jimfsURI);
            } catch (FileSystemNotFoundException e) {
                fileSystem = Jimfs.newFileSystem(fsName, Configuration.unix());
            }
            baseDir = fileSystem.getPath("/");
        }

        @Override
        public String getID() {
            return "fake";
        }

        @Override
        public String getPrefix() {
            return "fake_";
        }

        @Override
        public String getBaseDir() {
            return baseDir.toUri().toString();
        }

        @Override
        public String getSlotLayout() {
            return "4-4-2";
        }

    }

    private String getPropName(String storeID, String propType) {
        return new MessageFormat("MCR.IFS2.Store.{0}.{1}", Locale.ROOT).format(new Object[] { storeID, propType });
    }

    public static class FakeStore extends MCRStore {
        private String msg = "Fake Store";

        public FakeStore(MCRStoreConfig config) {
            init(config);
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }
}
