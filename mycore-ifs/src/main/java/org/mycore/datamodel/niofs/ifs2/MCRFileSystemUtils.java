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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.ifs2.MCRDirectory;
import org.mycore.datamodel.ifs2.MCRFile;
import org.mycore.datamodel.ifs2.MCRFileCollection;
import org.mycore.datamodel.ifs2.MCRFileStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.ifs2.MCRStoredNode;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler
 *
 */
abstract class MCRFileSystemUtils {

    private static final Logger LOGGER = LogManager.getLogger(MCRFileSystemUtils.class);

    private static final String DEFAULT_CONFIG_PREFIX = "MCR.IFS.ContentStore.IFS2."; //todo: rename

    public static final String STORE_ID_PREFIX = "IFS2_";

    private static String getBaseDir() {
        return MCRConfiguration2.getStringOrThrow(DEFAULT_CONFIG_PREFIX + "BaseDir");
    }

    private static String getDefaultSlotLayout() {
        return MCRConfiguration2.getString(DEFAULT_CONFIG_PREFIX + "SlotLayout")
            .orElseGet(() -> {
                String baseID = "a_a";
                String formatID = MCRObjectID.formatID(baseID, 1);
                int patternLength = formatID.length() - baseID.length() - "_".length();
                return patternLength - 4 + "-2-2";
            });
    }

    static MCRFileCollection getFileCollection(String owner) throws IOException {
        MCRObjectID derId = MCRObjectID.getInstance(owner);
        MCRFileStore fileStore = getStore(derId.getBase());
        MCRFileCollection fileCollection = fileStore
            .retrieve(derId.getNumberAsInteger());
        if (fileCollection == null) {
            throw new NoSuchFileException(null, null,
                "File collection " + owner + " is not available here: " + fileStore.getBaseDirectory());
        }
        return fileCollection;
    }

    static MCRFileStore getStore(String base) {
        String storeBaseDir = getBaseDir();

        String sid = STORE_ID_PREFIX + base;
        storeBaseDir += File.separatorChar + base.replace("_", File.separator);

        MCRFileStore store = MCRStoreManager.getStore(sid);
        if (store == null) {
            synchronized (MCRStoreManager.class) {
                store = MCRStoreManager.getStore(sid);
                if (store == null) {
                    store = createStore(sid, storeBaseDir, base);
                }
            }
        }
        return store;
    }

    private static MCRFileStore createStore(String sid, String storeBaseDir, String base) {
        try {
            configureStore(sid, storeBaseDir, base);
            return MCRStoreManager.createStore(sid, MCRFileStore.class);
        } catch (Exception ex) {
            String msg = "Could not create IFS2 file store with ID " + sid;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    private static void configureStore(String sid, String storeBaseDir, String base) {
        String storeConfigPrefix = "MCR.IFS2.Store." + sid + ".";
        configureIfNotSet(storeConfigPrefix + "Class", MCRFileStore.class.getName());
        configureIfNotSet(storeConfigPrefix + "BaseDir", storeBaseDir);
        configureIfNotSet(storeConfigPrefix + "Prefix", base + "_");
        configureIfNotSet(storeConfigPrefix + "SlotLayout", getDefaultSlotLayout());
    }

    private static void configureIfNotSet(String property, String value) {
        MCRConfiguration2.getString(property)
            .ifPresentOrElse(s -> {
                //if set, do nothing
            }, () -> {
                MCRConfiguration2.set(property, value);
                LOGGER.info("Configured {}={}", property, value);
            });
    }

    static MCRPath checkPathAbsolute(Path path) {
        MCRPath mcrPath = MCRPath.toMCRPath(path);
        if (!(Objects.requireNonNull(mcrPath.getFileSystem(), "'path' requires a associated filesystem.")
            .provider() instanceof MCRFileSystemProvider)) {
            throw new ProviderMismatchException("Path does not match to this provider: " + path);
        }
        if (!mcrPath.isAbsolute()) {
            throw new InvalidPathException(mcrPath.toString(), "'path' must be absolute.");
        }
        return mcrPath;
    }

    static String getOwnerID(MCRStoredNode node) {
        MCRFileCollection collection = node.getRoot();
        int intValue = collection.getID();
        String storeId = collection.getStore().getID();
        String baseId = storeId.substring(STORE_ID_PREFIX.length());
        return MCRObjectID.formatID(baseId, intValue);
    }

    static MCRPath toPath(MCRStoredNode node) {
        String ownerID = getOwnerID(node);
        String path = node.getPath();
        return MCRAbstractFileSystem.getPath(ownerID, path, MCRFileSystemProvider.getMCRIFSFileSystem());
    }

    static MCRFile getMCRFile(MCRPath ifsPath, boolean create, boolean createNew) throws IOException {
        if (!ifsPath.isAbsolute()) {
            throw new IllegalArgumentException("'path' needs to be absolute.");
        }
        MCRFile file;
        MCRDirectory root = null;
        boolean rootCreated = false;
        try {
            try {
                root = getFileCollection(ifsPath.getOwner());
            } catch (NoSuchFileException e) {
                if (create || createNew) {
                    MCRObjectID derId = MCRObjectID.getInstance(ifsPath.getOwner());
                    root = getStore(derId.getBase()).create(derId.getNumberAsInteger());
                    rootCreated = true;
                } else {
                    throw e;
                }
            }
            MCRPath relativePath = toPath(root).relativize(ifsPath);
            file = getMCRFile(root, relativePath, create, createNew);
        } catch (Exception e) {
            if (rootCreated) {
                LOGGER.error("Exception while getting MCRFile {}. Removing created filesystem nodes.", ifsPath);
                try {
                    root.delete();
                } catch (Exception de) {
                    LOGGER.fatal("Error while deleting file system node: {}", root.getName(), de);
                }
            }
            throw e;
        }
        return file;
    }

    static MCRFile getMCRFile(MCRDirectory baseDir, MCRPath relativePath, boolean create, boolean createNew)
        throws IOException {
        MCRPath ifsPath = relativePath;
        if (relativePath.isAbsolute()) {
            if (getOwnerID(baseDir).equals(relativePath.getOwner())) {
                ifsPath = toPath(baseDir).relativize(relativePath);
            } else
                throw new IOException(relativePath + " is absolute does not fit to " + toPath(baseDir));
        }
        Deque<MCRStoredNode> created = new LinkedList<>();
        MCRFile file;
        try {
            file = (MCRFile) baseDir.getNodeByPath(ifsPath.toString());
            if (file != null && createNew) {
                throw new FileAlreadyExistsException(toPath(baseDir).resolve(ifsPath).toString());
            }
            if (file == null & (create || createNew)) {
                Path normalized = ifsPath.normalize();
                MCRDirectory parent = baseDir;
                int nameCount = normalized.getNameCount();
                int directoryCount = nameCount - 1;
                int i = 0;
                while (i < directoryCount) {
                    String curName = normalized.getName(i).toString();
                    MCRDirectory curDir = (MCRDirectory) parent.getChild(curName);
                    if (curDir == null) {
                        curDir = parent.createDir(curName);
                        created.addFirst(curDir);
                    }
                    i++;
                    parent = curDir;
                }
                String fileName = normalized.getFileName().toString();
                file = parent.createFile(fileName);
                created.addFirst(file);
            }
        } catch (Exception e) {
            if (create || createNew) {
                LOGGER.error("Exception while getting MCRFile {}. Removing created filesystem nodes.", ifsPath);
                while (created.peekFirst() != null) {
                    MCRStoredNode node = created.pollFirst();
                    try {
                        node.delete();
                    } catch (Exception de) {
                        LOGGER.fatal("Error while deleting file system node: {}", node.getName(), de);
                    }
                }
            }
            throw e;
        }
        return file;
    }

    @SuppressWarnings("unchecked")
    static <T extends MCRStoredNode> T resolvePath(MCRPath path) throws IOException {
        if (path.getNameCount() == 0) {
            return (T) getFileCollection(path.getOwner());
        }
        //recursive call
        String fileOrDir = path.getFileName().toString();
        MCRDirectory parentDir = doResolveParent(path.getParent())
            .map(MCRDirectory.class::cast)
            .orElseThrow(() -> new NoSuchFileException(path.getParent().toString(), fileOrDir,
                "parent directory does not exist"));

        return (T) Optional.ofNullable(parentDir.getChild(fileOrDir))
            .map(MCRStoredNode.class::cast)
            .orElseThrow(
                () -> new NoSuchFileException(path.getParent().toString(), fileOrDir, "file does not exist"));

    }

    private static Optional<MCRDirectory> doResolveParent(MCRPath parent) {
        if (parent.getNameCount() == 0) {
            MCRObjectID derId = MCRObjectID.getInstance(parent.getOwner());
            return Optional.ofNullable(getStore(derId.getBase()))
                .map(s -> {
                    try {
                        return s.retrieve(derId.getNumberAsInteger());
                    } catch (IOException e) {
                        LOGGER.warn("Exception while retrieving file collection " + derId, e);
                        return null;
                    }
                });
        }
        //recursive call
        String dirName = parent.getFileName().toString();
        return doResolveParent(parent.getParent())
            .map(p -> p.getChild(dirName))
            .map(MCRDirectory.class::cast);
    }

}
