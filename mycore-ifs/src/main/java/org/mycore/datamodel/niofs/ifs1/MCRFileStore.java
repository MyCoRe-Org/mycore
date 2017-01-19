package org.mycore.datamodel.niofs.ifs1;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class MCRFileStore extends MCRAbstractFileStore {

    private MCRContentStore contentStore;

    private FileStore baseFileStore;

    private static LoadingCache<String, MCRFileStore> instanceHolder = CacheBuilder.newBuilder().weakKeys()
        .build(new CacheLoader<String, MCRFileStore>() {

            @Override
            public MCRFileStore load(String contentStoreID) throws Exception {
                MCRContentStore store = MCRContentStoreFactory.getStore(contentStoreID);
                return new MCRFileStore(store);
            }
        });

    private MCRFileStore(MCRContentStore contentStore) throws IOException {
        this.contentStore = contentStore;
        File baseDir = this.contentStore.getBaseDir();
        if (baseDir == null) {
            throw new IOException("Content Store " + contentStore.getID() + "(" + contentStore.getClass()
                + " ) has no base dir");
        }
        this.baseFileStore = getFileStore(baseDir);
        if (baseFileStore == null) {
            String reason = "Cannot access base directory or any parent directory of Content Store "
                + contentStore.getID();
            throw new NoSuchFileException(baseDir.toString(), null, reason);
        }
    }

    private static FileStore getFileStore(File baseDir) throws IOException {
        if (baseDir == null) {
            return null;
        }
        //fixes MCR-964
        return baseDir.exists() ? Files.getFileStore(baseDir.toPath()) : getFileStore(baseDir.getParentFile());
    }

    public static MCRFileStore getInstance(String contentStoreId) throws IOException {
        try {
            return instanceHolder.get(contentStoreId);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Error while geting instance of " + MCRFileStore.class.getSimpleName(), cause);
        }
    }

    @Override
    public String name() {
        return contentStore.getID();
    }

    @Override
    public String type() {
        return contentStore.getClass().getCanonicalName();
    }

    @Override
    public boolean isReadOnly() {
        return baseFileStore.isReadOnly();
    }

    @Override
    public long getTotalSpace() throws IOException {
        return baseFileStore.getTotalSpace();
    }

    @Override
    public long getUsableSpace() throws IOException {
        return baseFileStore.getUsableSpace();
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return baseFileStore.getUnallocatedSpace();
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return this.baseFileStore.supportsFileAttributeView(type);
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return this.baseFileStore.supportsFileAttributeView(name);
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return this.baseFileStore.getFileStoreAttributeView(type);
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        return this.baseFileStore.getAttribute(attribute);
    }

    @Override
    public Path getBaseDirectory() throws IOException {
        return this.contentStore.getBaseDir().toPath();
    }

    @Override
    public Path getPhysicalPath(MCRPath path) {
        MCRFileSystemUtils.checkPathAbsolute(path);
        try {
            MCRFile mcrFile = MCRFileSystemUtils.getMCRFile(path, false, false);
            return mcrFile.getLocalFile().toPath();
        } catch (IOException e) {
            LogManager.getLogger(getClass()).info(e.getMessage());
            return null;
        }
    }

}
