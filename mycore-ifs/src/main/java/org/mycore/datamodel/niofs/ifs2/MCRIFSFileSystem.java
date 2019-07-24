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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStoreCenter;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIFSFileSystem extends MCRAbstractFileSystem {

    private MCRFileSystemProvider provider;

    MCRIFSFileSystem(MCRFileSystemProvider provider) {
        super();
        this.provider = provider;
    }

    /* (non-Javadoc)
     * @see java.nio.file.FileSystem#provider()
     */
    @Override
    public MCRFileSystemProvider provider() {
        return provider;
    }

    /* (non-Javadoc)
     * @see java.nio.file.FileSystem#getRootDirectories()
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return MCRStoreCenter.instance().getCurrentStores(org.mycore.datamodel.ifs2.MCRFileStore.class)
            .filter(s -> s.getID().startsWith(MCRFileSystemUtils.STORE_ID_PREFIX))
            .sorted(Comparator.comparing(MCRStore::getID))
            .flatMap(s -> s.getStoredIDs()
                .mapToObj(
                    i -> MCRObjectID.formatID(s.getID().substring(MCRFileSystemUtils.STORE_ID_PREFIX.length()), i)))
            .map(owner -> MCRAbstractFileSystem.getPath(owner, "/", this))
            .map(Path.class::cast)::iterator;
    }

    /* (non-Javadoc)
     * @see java.nio.file.FileSystem#getFileStores()
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return MCRStoreCenter.instance().getCurrentStores(org.mycore.datamodel.ifs2.MCRFileStore.class)
            .filter(s -> s.getID().startsWith(MCRFileSystemUtils.STORE_ID_PREFIX))
            .sorted(Comparator.comparing(MCRStore::getID))
            .map(MCRStore::getID)
            .distinct()
            .map(storeId -> {
                try {
                    return MCRFileStore.getInstance(storeId);
                } catch (IOException e) {
                    LogManager.getLogger().error("Error while iterating FileStores.", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .map(FileStore.class::cast)::iterator;
    }

    @Override
    public void createRoot(String owner) throws FileSystemException {
        MCRObjectID derId = MCRObjectID.getInstance(owner);
        org.mycore.datamodel.ifs2.MCRFileStore fileStore = MCRFileSystemUtils.getStore(derId.getBase());
        MCRPath rootPath = getPath(owner, "", this);
        try {
            if (fileStore.retrieve(derId.getNumberAsInteger()) != null) {
                throw new FileAlreadyExistsException(rootPath.toString());
            }
            try {
                MCRDirectory rootDirectory = fileStore.create(derId.getNumberAsInteger());
            } catch (RuntimeException e) {
                LogManager.getLogger(getClass()).warn("Catched RuntimeException while creating new root directory.", e);
                throw new FileSystemException(rootPath.toString(), null, e.getMessage());
            }
        } catch (FileSystemException e) {
            throw e;
        } catch (IOException e) {
            throw new FileSystemException(rootPath.toString(), null, e.getMessage());
        }
        LogManager.getLogger(getClass()).info("Created root directory: {}", rootPath);
    }

    @Override
    public void removeRoot(String owner) throws FileSystemException {
        MCRPath rootPath = getPath(owner, "", this);
        MCRDirectory rootDirectory = null;
        try {
            rootDirectory = MCRFileSystemUtils.getFileCollection(owner);
            if (rootDirectory.hasChildren()) {
                throw new DirectoryNotEmptyException(rootPath.toString());
            }
            rootDirectory.delete();
        } catch (FileSystemException e) {
            throw e;
        } catch (IOException e) {
            throw new FileSystemException(rootPath.toString(), null, e.getMessage());
        } catch (RuntimeException e) {
            LogManager.getLogger(getClass()).warn("Catched RuntimeException while removing root directory.", e);
            throw new FileSystemException(rootPath.toString(), null, e.getMessage());
        }
        LogManager.getLogger(getClass()).info("Removed root directory: {}", rootPath);
    }

}
