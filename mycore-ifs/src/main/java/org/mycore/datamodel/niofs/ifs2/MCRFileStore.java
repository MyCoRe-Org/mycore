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

package org.mycore.datamodel.niofs.ifs2;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.niofs.MCRAbstractFileStore;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * IFS2 MyCoRe FileStore implementation
 */
public class MCRFileStore extends MCRAbstractFileStore {

    private MCRStore contentStore;

    private String baseId;

    private FileStore baseFileStore;

    private static LoadingCache<String, MCRFileStore> instanceHolder = CacheBuilder.newBuilder().weakKeys()
        .build(new CacheLoader<>() {

            @Override
            public MCRFileStore load(String contentStoreID) throws Exception {
                MCRStore store = MCRStoreManager.getStore(contentStoreID);
                return new MCRFileStore(store);
            }
        });

    private MCRFileStore(MCRStore contentStore) throws IOException {
        this.contentStore = contentStore;
        Path baseDir = this.contentStore.getBaseDirectory();
        this.baseFileStore = getFileStore(baseDir);
        this.baseId = contentStore.getID().substring(MCRFileSystemUtils.STORE_ID_PREFIX.length());
        if (baseFileStore == null) {
            String reason = "Cannot access base directory or any parent directory of Content Store "
                + contentStore.getID();
            throw new NoSuchFileException(baseDir.toString(), null, reason);
        }
    }

    private static FileStore getFileStore(Path baseDir) throws IOException {
        if (baseDir == null) {
            return null;
        }
        //fixes MCR-964
        return Files.exists(baseDir) ? Files.getFileStore(baseDir) : getFileStore(baseDir.getParent());
    }

    static MCRFileStore getInstance(String storeId) throws IOException {
        try {
            return instanceHolder.get(storeId);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioe) {
                throw ioe;
            }
            throw new IOException("Error while geting instance of " + MCRFileStore.class.getSimpleName(), cause);
        }
    }

    /**
     * creates a new MCRFileStore instance
     * @param node - the MyCoRe store node
     * @return the MCRFileStore instance
     * @throws IOException if the MCRFileStore could not be created
     */
    public static MCRFileStore getInstance(MCRStoredNode node) throws IOException {
        return getInstance(node.getRoot().getStore().getID());
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
    public Path getBaseDirectory() {
        return this.contentStore.getBaseDirectory();
    }

    @Override
    public Path getPhysicalPath(MCRPath path) {
        MCRFileSystemUtils.checkPathAbsolute(path);
        LogManager.getLogger().warn("Checking if {} is of baseId {}", path, baseId);
        if (!path.getOwner().startsWith(baseId + "_")) {
            LogManager.getLogger().warn("{} is not of baseId {}", path, baseId);
            return null;
        }
        try {
            MCRFile mcrFile = MCRFileSystemUtils.getMCRFile(path, false, false, true);
            return mcrFile.getLocalPath();
        } catch (IOException e) {
            LogManager.getLogger(getClass()).info(e.getMessage());
            return null;
        }
    }

}
