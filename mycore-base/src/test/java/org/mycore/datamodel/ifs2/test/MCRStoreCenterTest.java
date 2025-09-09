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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;
import org.mycore.datamodel.ifs2.MCRStoreAlreadyExistsException;
import org.mycore.datamodel.ifs2.MCRStoreCenter;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class MCRStoreCenterTest {
    @BeforeEach
    public void init() {
        MCRStoreCenter.getInstance().clear();
    }

    @Test
    public void heapOp() throws Exception {
        MCRStoreCenter storeHeap = MCRStoreCenter.getInstance();

        FakeStoreConfig config = new FakeStoreConfig("heapOp");
        storeHeap.addStore(config.getID(), new FakeStore(config));
        String storeID = config.getID();
        FakeStore fakeStore = storeHeap.getStore(storeID);

        assertNotNull(fakeStore, "The store should be not null.");
        assertEquals("Fake Store", fakeStore.getMsg());

        assertTrue(storeHeap.getCurrentStores(FakeStore.class)
            .filter(s -> storeID.equals(s.getID()))
            .findAny().isPresent(), "Could not find store with ID: " + storeID);

        assertTrue(storeHeap.removeStore(storeID), "Could not remove store with ID: " + storeID);
        assertNull(storeHeap.<FakeStore>getStore(storeID), "There should be no store with ID: " + storeID);
    }

    @Test
    public void addStoreTwice() {
        assertThrows(
            MCRStoreAlreadyExistsException.class,
            () -> {
                MCRStoreCenter storeHeap = MCRStoreCenter.getInstance();
                FakeStoreConfig config = new FakeStoreConfig("addStoreTwice");
                FakeStore fakeStore = new FakeStore(config);
                storeHeap.addStore(config.getID(), fakeStore);
                storeHeap.addStore(config.getID(), fakeStore);

            });
    }

    class FakeStoreConfig implements MCRStoreConfig {
        private final Path baseDir;

        FakeStoreConfig(String id) {
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
