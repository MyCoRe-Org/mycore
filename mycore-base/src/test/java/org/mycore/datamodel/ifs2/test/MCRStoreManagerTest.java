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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;
import org.mycore.datamodel.ifs2.MCRStoreManager;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class MCRStoreManagerTest {
    @Test
    public void createMCRFileStore() throws Exception {
        StoreConfig config = new StoreConfig();
        MCRFileStore fileStore = MCRStoreManager.createStore(config, MCRFileStore.class);

        assertNotNull("MCRStoreManager could not create Filestore.", fileStore);
    }

    class StoreConfig implements MCRStoreConfig {

        private final Path baseDir;

        StoreConfig() throws IOException {
            String fsName = MCRStoreManagerTest.class.getSimpleName();
            URI jimfsURI = URI.create("jimfs://" + fsName);
            FileSystem fileSystem = Jimfs.newFileSystem(fsName, Configuration.unix());
            try {
                fileSystem = FileSystems.getFileSystem(jimfsURI);
            } catch (FileSystemNotFoundException e) {
                fileSystem = FileSystems.newFileSystem(jimfsURI, Map.of("fileSystem", fileSystem));
            }
            baseDir = fileSystem.getPath("/");
        }

        @Override
        public String getID() {
            return "Test";
        }

        @Override
        public String getPrefix() {
            return getID() + "_";
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
}
