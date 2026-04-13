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
 * test configuration for an MCRStore, that uses an in-memory-fileystem (jimfs).
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
