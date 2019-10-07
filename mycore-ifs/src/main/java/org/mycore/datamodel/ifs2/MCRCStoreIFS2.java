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

package org.mycore.datamodel.ifs2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Optional;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Implements the MCRContentStore interface to store the content of
 * IFS1 MCRFile objects in a structure of the new, but currently incomplete
 * IFS2 store. Example configuration:
 * 
 * <code>
 * MCR.IFS.ContentStore.FS.Class=org.mycore.datamodel.ifs2.MCRCStoreIFS2
 * MCR.IFS.ContentStore.FS.BaseDir=/foo/bar
 * MCR.IFS.ContentStore.FS.SlotLayout=4-2-2
 * </code>
 * 
 * @author Frank L\u00FCtzenkirchen
 **/
public class MCRCStoreIFS2 extends MCRContentStore {

    private String slotLayout;

    private String baseDir;

    private boolean ignoreOwnerBase;

    private static final Logger LOGGER = LogManager.getLogger(MCRCStoreIFS2.class);

    @Override
    public void init(String storeID) {
        super.init(storeID);

        MCRConfiguration config = MCRConfiguration.instance();
        baseDir = config.getString(storeConfigPrefix + "BaseDir");
        LOGGER.debug("Base directory for store {} is {}", storeID, baseDir);

        String pattern = config.getString("MCR.Metadata.ObjectID.NumberPattern", "0000000000");
        slotLayout = pattern.length() - 4 + "-2-2";
        slotLayout = config.getString(storeConfigPrefix + "SlotLayout", slotLayout);
        LOGGER.debug("Default slot layout for store {} is {}", storeID, slotLayout);

        ignoreOwnerBase = config.getBoolean(storeConfigPrefix + "IgnoreOwnerBase", false);
        MCREventManager.instance().addEventHandler(MCREvent.PATH_TYPE, new UpdateMetadataHandler(this));
    }

    private MCRFileStore getStore(String base) {
        String sid = getID();
        String storeBaseDir = baseDir;
        String prefix = "";

        if (!ignoreOwnerBase) {
            sid += "_" + base;
            storeBaseDir += File.separatorChar + base.replace("_", File.separator);
            prefix = base + "_";
        }

        MCRFileStore store = MCRStoreManager.getStore(sid);
        if (store == null) {
            synchronized (this) {
                store = MCRStoreManager.getStore(sid);
                if (store == null) {
                    store = createStore(sid, storeBaseDir);
                }
            }
        }
        store.prefix = prefix;
        return store;
    }

    private MCRFileStore createStore(String sid, String storeBaseDir) {
        try {
            configureStore(sid, storeBaseDir);
            return MCRStoreManager.createStore(sid, MCRFileStore.class);
        } catch (Exception ex) {
            String msg = "Could not create IFS2 file store with ID " + sid;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    private void configureStore(String sid, String storeBaseDir) {
        String storeConfigPrefix = "MCR.IFS2.Store." + sid + ".";
        configureIfNotSet(storeConfigPrefix + "Class", MCRFileStore.class.getName());
        configureIfNotSet(storeConfigPrefix + "BaseDir", storeBaseDir);
        configureIfNotSet(storeConfigPrefix + "SlotLayout", slotLayout);
    }

    private void configureIfNotSet(String property, String value) {
        value = MCRConfiguration.instance().getString(property, value);
        MCRConfiguration.instance().set(property, value);
        LOGGER.info("Configured {}={}", property, value);
    }

    private int getSlotID(String ownerID) {
        int pos = ownerID.lastIndexOf("_") + 1;
        return Integer.parseInt(ownerID.substring(pos));
    }

    private String getBase(String ownerID) {
        int pos = ownerID.lastIndexOf("_");
        return ownerID.substring(0, pos);
    }

    @Override
    protected boolean exists(org.mycore.datamodel.ifs.MCRFile fr) {
        int slotID = getSlotID(fr.getOwnerID());
        String base = getBase(fr.getOwnerID());
        MCRFileStore store = getStore(base);

        try {
            MCRFileCollection slot = store.retrieve(slotID);
            if (slot == null) {
                return false;
            }

            String path = fr.getAbsolutePath();
            MCRNode node = slot.getNodeByPath(path);
            return (node != null);
        } catch (IOException ex) {
            String msg = "Exception checking existence of file " + fr.getAbsolutePath();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    /**
     * For the given derivateID, returns the underlying IFS2 file collection storing the files of the derivate 
     */
    public MCRFileCollection getIFS2FileCollection(MCRObjectID derivateID) throws IOException {
        return getSlot(derivateID);
    }

    @Override
    protected String doStoreContent(org.mycore.datamodel.ifs.MCRFile fr, MCRContentInputStream source)
        throws Exception {
        int slotID = getSlotID(fr.getOwnerID());
        String base = getBase(fr.getOwnerID());
        MCRFileStore store = getStore(base);

        MCRFileCollection slot = store.retrieve(slotID);
        if (slot == null) {
            synchronized (store) {
                slot = store.retrieve(slotID);
                if (slot == null) {
                    slot = store.create(slotID);
                }
            }
        }

        String path = fr.getAbsolutePath();
        MCRDirectory dir = slot;
        StringTokenizer steps = new StringTokenizer(path, "/");
        while (steps.hasMoreTokens()) {
            String step = steps.nextToken();
            if (steps.hasMoreTokens()) {
                MCRNode child = dir.getChild(step);
                if (child == null) {
                    dir = dir.createDir(step);
                } else {
                    dir = (MCRDirectory) child;
                }
            } else {
                MCRFile file = (MCRFile) (dir.getChild(step));
                if (file == null) {
                    file = dir.createFile(step);
                }

                file.setContent(new MCRStreamContent(source));
            }
        }

        return fr.getOwnerID() + path;
    }

    @Override
    protected void doDeleteContent(String storageID) throws Exception {
        Optional<MCRFile> file;
        try {
            file = Optional.of(getFile(storageID));
        } catch (IOException e) {
            //file is not present
            LOGGER.warn(e.getMessage());
            file = Optional.empty();
        }
        Optional<MCRDirectory> parent;
        try {
            parent = Optional.ofNullable(file
                .map(MCRFile::getParent)
                .map(MCRDirectory.class::cast)
                .orElseGet(() -> getParentDirectory(storageID)));
        } catch (UncheckedIOException e) {
            LOGGER.warn(e.getMessage());
            parent = Optional.empty();
        }
        if (file.isPresent()) {
            file.get().delete();
        }
        if (parent.isPresent()) {
            deleteEmptyParents(parent.get(), toDerivateID(storageID));
        }
    }

    private MCRDirectory getParentDirectory(String storageID) throws UncheckedIOException {
        MCRPath relPath = MCRPath.getPath("", toPath(storageID));
        String parentPath = relPath.getParent().getOwnerRelativePath();
        MCRFileCollection slot;
        try {
            slot = getSlot(toDerivateID(storageID));
            return (MCRDirectory) slot.getNodeByPath(parentPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteEmptyParents(MCRDirectory dir, MCRObjectID derId) throws IOException {
        if (dir == null) {
            return;
        }
        if (dir.hasChildren()) {
            //check metadatastore
            String owner = derId.toString();
            MCRPath path = MCRPath.getPath(owner, dir.getPath());
            LOGGER.debug("Checking {}", path);
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                    Iterator<Path> pathIterator = ds.iterator();
                    if (pathIterator.hasNext()) {
                        //has children
                        LOGGER.debug("Child: {}", pathIterator.next());
                        return;
                    }
                }
            }
        }
        MCRDirectory parent = (MCRDirectory) (dir.getParent());
        dir.delete();
        deleteEmptyParents(parent, derId);
    }

    @Override
    protected MCRContent doRetrieveMCRContent(org.mycore.datamodel.ifs.MCRFile fr) throws IOException {
        String storageID = fr.getStorageID();
        MCRFile file = getFile(storageID);
        return file.getContent();
    }

    @Override
    public File getLocalFile(String storageId) throws IOException {
        if (storageId == null || storageId.isEmpty()) {
            throw new IOException("No storage id");
        }
        MCRFile file = getFile(storageId);
        return file.getLocalPath().toFile();
    }

    private MCRFileCollection getSlot(MCRObjectID derivateID) throws IOException {
        return Optional
            .ofNullable(getStore(derivateID.getBase()).retrieve(derivateID.getNumberAsInteger()))
            .orElseThrow(() -> getIOException(derivateID.getBase(), derivateID.getNumberAsInteger()));
    }

    private MCRObjectID toDerivateID(String storageID) {
        return MCRObjectID.getInstance(storageID.substring(0, storageID.indexOf("/")));
    }

    private String toPath(String storageID) {
        int pos = storageID.indexOf("/") + 1;
        return storageID.substring(pos);
    }

    private MCRFile getFile(String storageID) throws IOException {
        MCRFileCollection slot = getSlot(toDerivateID(storageID));
        String path = toPath(storageID);
        return Optional
            .ofNullable(slot.getNodeByPath(path))
            .map(MCRFile.class::cast)
            .orElseThrow(() -> getIOException(slot, path, storageID));
    }

    private IOException getIOException(String base, int slotID) {
        return new IOException(
            "Could not resolve slot '" + slotID + "' of store: " + getStore(base).getID());
    }

    private IOException getIOException(MCRFileCollection slot, String path, String storageID) {
        return new IOException(
            "Could not find path '" + path + "' in file collection '" + slot.getID() + "' for storageID: " + storageID);
    }

    @Override
    public File getBaseDir() throws IOException {
        return new File(baseDir);
    }

    private class UpdateMetadataHandler extends MCREventHandlerBase {
        private final MCRCStoreIFS2 myStore;

        private final Path baseDir;

        public UpdateMetadataHandler(MCRCStoreIFS2 storeIFS2) {
            myStore = storeIFS2;
            baseDir = Paths.get(myStore.baseDir);
        }

        @Override
        protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
            updateMD5(path, attrs);
        }

        @Override
        protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
            updateMD5(path, attrs);
        }

        private void updateMD5(Path path, BasicFileAttributes attrs) {
            if (!attrs.isRegularFile()) {
                return;
            }
            if (!(attrs instanceof MCRFileAttributes)) {
                LOGGER.warn("Cannot handle update of {} with file attribut class {}", path, attrs.getClass());
                return;
            }
            String md5 = ((MCRFileAttributes) attrs).md5sum();
            try {
                Path physicalPath = MCRPath.toMCRPath(path).toPhysicalPath();
                if (physicalPath.startsWith(baseDir)) {
                    getFile(path).ifPresent(f -> updateMD5(f, path, md5));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void updateMD5(MCRFile file, Path path, String md5) {
            try {
                file.setMD5(md5);
                LOGGER.info("Updated MD5 sum of {} to {}", path, md5);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Optional<MCRFile> getFile(Path path) throws IOException {
            MCRPath mcrPath = MCRPath.toMCRPath(path);
            MCRFileCollection fileCollection = myStore
                .getIFS2FileCollection(MCRObjectID.getInstance(mcrPath.getOwner()));
            MCRNode nodeByPath = fileCollection.getNodeByPath(mcrPath.getOwnerRelativePath());
            return Optional.ofNullable(nodeByPath)
                .filter(MCRFile.class::isInstance)
                .map(MCRFile.class::cast);
        }
    }
}
