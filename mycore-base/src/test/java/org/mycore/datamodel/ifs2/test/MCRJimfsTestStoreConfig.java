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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

/**
 * Test configuration for an MCRStore that uses an in-memory-fileystem (jimfs).
 */
public class MCRJimfsTestStoreConfig implements MCRStoreConfig {

    private final Path baseDir;

    public MCRJimfsTestStoreConfig(String fsName) throws IOException {
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
