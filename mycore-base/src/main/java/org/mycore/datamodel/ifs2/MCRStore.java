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

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Stores metadata files or file collections containing files and directories in
 * a persistent store implemented using a local filesystem.
 * 
 * For better filesystem performance, the store can build slot subdirectories
 * (containing other subdirectories and so on) so that not all objects are
 * stored in the same filesystem directory. Directories containing a very large
 * number of files typically show bad performance.
 * 
 * The slot layout of the store defines the usage of subdirectories within the
 * base directory. A layout of "8" would mean no subdirectories will be used,
 * the maximum ID size is 8 digits, and therefore up to 99999999 objects can be
 * stored all in the same base directory. A layout of "2-2-4" would mean data is
 * stored using two levels of subdirectories, where the first subdirectory
 * contains up to 100 (00-99) subdirectories, the second subdirectory level
 * below contains up to 100 subdirectories, too, and below the data is stored,
 * with up to 10000 data objects in the subdirectory. Using this slot layout,
 * the data of ID 10485 would be stored in the file object "/00/01/00010485",
 * for example. Using layout "4-2-2", data would be stored in
 * "/0001/04/00010485", and so on.
 * 
 * The slot file name itself may optionally have a prefix and suffix. With
 * prefix "derivate-", the slot name would be "derivate-00010485". With prefix
 * "DocPortal_document_" and suffix ".xml", the slot name would be
 * "DocPortal_document_00010485.xml" for example.
 * 
 * MCR.IFS2.Store.ID.Class=org.mycore.datamodel.ifs2.MCRFileStore
 * MCR.IFS2.Store.ID.BaseDir=/foo/bar
 * MCR.IFS2.Store.ID.SlotLayout=4-2-2
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRStore {

    /**
     * Indicates ascending order when listing IDs
     */
    public static final boolean ASCENDING = true;

    /**
     * Indicates descending order when listing IDs
     */
    public static final boolean DESCENDING = false;

    /** The ID of the store */
    protected String id;

    /** The base directory containing the stored data */
    protected FileObject baseDirectory;

    /** The maximum length of IDs **/
    protected int idLength;

    /**
     * The slot subdirectory layout, which is the number of digits used at each
     * subdirectory level to build the filename.
     */
    protected int[] slotLength;

    /** The prefix of slot names */
    protected String prefix = "";

    /** The suffix of slot names */
    protected String suffix = "";

    private MCRStoreConfig storeConfig;

    /**
     * Offset to add to the maximum ID found in the store to build the new ID.
     * This is normally 1, but initially higher to avoid reassigning the same ID
     * after system restarts. Consider the following example:
     * 
     * 1) User creates new document, ID assigned is 10. 2) User deletes document
     * 10. 3) Web application is restarted. 4) User creates new document, ID
     * assigned is 20. If offset would always be 1, ID assigned would have been
     * 10 again, and that is not nice, because we can not distinguish the two
     * creates easily.
     */
    protected int offset = 11; // Sicherheitsabstand, initially 11, later 1

    /**
     * The last ID assigned by this store.
     */
    protected int lastID = 0;

    /**
     * Deletes the data stored under the given ID from the store
     * 
     * @param id
     *            the ID of the document to be deleted
     */
    public void delete(final int id) throws IOException {
        delete(getSlot(id));
    }

    /**
     * Returns true if data for the given ID is existing in the store.
     * 
     * @param id
     *            the ID of the data
     * @return true, if data for the given ID is existing in the store.
     */
    public boolean exists(final int id) throws IOException {
        return getSlot(id).exists();
    }

    public synchronized int getHighestStoredID() {
        int found = 0;
        try {
            String max;
            max = findMaxID(baseDirectory, 0);
            if (max != null) {
                found = slot2id(max);
            }
        } catch (final FileSystemException e) {
            e.printStackTrace();
        }
        return found;
    }

    /**
     * Returns the ID of this store
     */
    public String getID() {
        return getStoreConfig().getID();
    }

    /**
     * Returns the next free ID that can be used to store data. Call as late as
     * possible to avoid that another process, for example from batch import, in
     * the meantime already used that ID.
     * 
     * @return the next free ID that can be used to store data
     */
    public synchronized int getNextFreeID() {
        lastID = Math.max(getHighestStoredID(), lastID);
        lastID += lastID > 0 ? offset : 1;
        offset = 1;
        return lastID;
    }

    public boolean isEmpty() {
        try {
            return baseDirectory.getChildren().length == 0;
        } catch (final FileSystemException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Lists all IDs currently used in the store, in ascending or descending
     * order
     * 
     * @see #ASCENDING
     * @see #DESCENDING
     * 
     * @param order
     *            the order in which IDs should be returned.
     * @return all IDs currently used in the store
     */
    public Iterator<Integer> listIDs(final boolean order) {
        return new Iterator<Integer>() {
            /**
             * List of files or directories in store not yet handled
             */
            List<FileObject> files = new ArrayList<>();

            /**
             * The next ID to return, when 0, all IDs have been returned
             */
            int nextID;

            /**
             * The last ID that was returned
             */
            int lastID;

            /**
             * The order in which the IDs should be returned, ascending or
             * descending
             */
            boolean order;

            @Override
            public boolean hasNext() {
                return nextID > 0;
            }

            @Override
            public Integer next() {
                if (nextID < 1) {
                    throw new NoSuchElementException();
                }

                lastID = nextID;
                nextID = findNextID();
                return lastID;
            }

            @Override
            public void remove() {
                if (lastID == 0) {
                    throw new IllegalStateException();
                }
                try {
                    MCRStore.this.delete(lastID);
                } catch (final Exception ex) {
                    throw new MCRException("Could not delete " + MCRStore.this.getID() + " " + lastID, ex);
                }
                lastID = 0;
            }

            /**
             * Initializes the enumeration and searches for the first ID to
             * return
             * 
             * @param order
             *            the return order, ascending or descending
             */
            Iterator<Integer> init(final boolean order) {
                this.order = order;
                try {
                    addChildren(baseDirectory);
                } catch (final FileSystemException e) {
                    e.printStackTrace();
                }
                nextID = findNextID();
                return this;
            }

            /**
             * Adds children of the given directory to the list of files to
             * handle next. Depending on the return sort order, ascending or
             * descending file name order is used.
             * 
             * @param dir
             *            the directory thats children should be added
             * @throws FileSystemException 
             */
            private void addChildren(final FileObject dir) throws FileSystemException {
                if (dir.getType() == FileType.FOLDER) {
                    final FileObject[] children = dir.getChildren();
                    Arrays.sort(children, new MCRFileObjectComparator());

                    for (int i = 0; i < children.length; i++) {
                        files.add(order ? i : 0, children[i]);
                    }
                }
            }

            /**
             * Finds the next ID used in the store.
             * 
             * @return the next ID, or 0 if there is no other ID any more
             */
            private int findNextID() {
                if (files.isEmpty()) {
                    return 0;
                }

                final FileObject first = files.remove(0);
                // checks basename length against prefix (projectId_typeId), file suffix (.xml) and configured id length
                // if they match it should be a parseable id
                if (first.getName().getBaseName().length() == idLength + prefix.length() + suffix.length()) {
                    return MCRStore.this.slot2id(first.getName().getBaseName());
                }

                try {
                    addChildren(first);
                } catch (final FileSystemException e) {
                    e.printStackTrace();
                }
                return findNextID();
            }
        }.init(order);
    }

    /**
     * Deletes the data stored in the given file object from the store
     * 
     * @param fo
     *            the file object to be deleted
     */
    void delete(FileObject fo) throws IOException {
        FileObject parent = fo.getParent();
        fo.delete(Selectors.SELECT_ALL);

        while (!parent.equals(baseDirectory)) {
            final FileObject[] children = parent.getChildren();
            if (children.length > 0) {
                break;
            }
            fo = parent;
            parent = fo.getParent();
            fo.delete();
        }
    }

    /**
     * Returns the absolute path of the local base directory
     * 
     * @return the base directory storing the data
     */
    String getBaseDirURI() {
        return baseDirectory.getName().getURI();
    }

    /** Returns the maximum length of any ID stored in this store */
    int getIDLength() {
        return idLength;
    }

    /**
     * Returns the relative path used to store data for the given ID within the
     * store base directory
     * 
     * @param ID
     *            the ID of the data
     * @return the relative path storing that data
     */
    String getSlotPath(final int ID) {
        final String[] paths = getSlotPaths(ID);
        return paths[paths.length - 1];
    }

    /**
     * Returns the paths of all subdirectories and the slot itself used to store
     * data for the given ID relative to the store base directory
     * 
     * @param ID
     *            the ID of the data
     * @return the directory and file names of the relative path storing that
     *         data
     */
    String[] getSlotPaths(final int ID) {
        final String id = createIDWithLeadingZeros(ID);

        final String[] paths = new String[slotLength.length + 1];
        final StringBuilder path = new StringBuilder();
        int offset = 0;
        for (int i = 0; i < paths.length - 1; i++) {
            path.append(id.substring(offset, offset + slotLength[i]));
            paths[i] = path.toString();
            path.append("/");
            offset += slotLength[i];
        }
        path.append(prefix).append(id).append(suffix);
        paths[paths.length - 1] = path.toString();
        return paths;
    }

    /**
     * Extracts the numerical ID contained in the slot filename.
     * 
     * @param slot
     *            the file name of the slot containing the data
     * @return the ID of that data
     */
    int slot2id(String slot) {
        slot = slot.substring(prefix.length());
        slot = slot.substring(0, idLength);
        return Integer.parseInt(slot);
    }

    /**
     * Returns the slot file object used to store data for the given ID. This
     * may be a file or directory, depending on the subclass of MCRStore that is
     * used.
     * 
     * @param ID
     *            the ID of the data
     * @return the file object storing that data
     */
    protected FileObject getSlot(final int ID) throws IOException {
        return VFS.getManager().resolveFile(baseDirectory, getSlotPath(ID));
    }

    protected MCRStoreConfig getStoreConfig() {
        return storeConfig;
    }

    protected void init(final MCRStoreConfig config) {
        setStoreConfig(config);

        idLength = 0;

        final StringTokenizer st = new StringTokenizer(getStoreConfig().getSlotLayout(), "-");
        slotLength = new int[st.countTokens() - 1];

        int i = 0;
        while (st.countTokens() > 1) {
            slotLength[i] = Integer.parseInt(st.nextToken());
            idLength += slotLength[i++];
        }
        idLength += Integer.parseInt(st.nextToken());

        try {
            baseDirectory = VFS.getManager().resolveFile(getStoreConfig().getBaseDir());

            if (!baseDirectory.exists()) {
                baseDirectory.createFolder();
            } else {
                if (!baseDirectory.isReadable()) {
                    final String msg = "Store directory " + getStoreConfig().getBaseDir() + " is not readable";
                    throw new MCRConfigurationException(msg);
                }

                if (baseDirectory.getType() != FileType.FOLDER) {
                    final String msg = "Store " + getStoreConfig().getBaseDir() + " is a file, not a directory";
                    throw new MCRConfigurationException(msg);
                }
            }
        } catch (final FileSystemException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes a new store instance
     */
    protected void init(final String id) {
        init(new MCRStoreDefaultConfig(id));
    }

    protected void setStoreConfig(final MCRStoreConfig storeConfig) {
        this.storeConfig = storeConfig;
    }

    private String createIDWithLeadingZeros(final int ID) {
        final NumberFormat numWithLeadingZerosFormat = NumberFormat.getIntegerInstance(Locale.ROOT);
        numWithLeadingZerosFormat.setMinimumIntegerDigits(idLength);
        numWithLeadingZerosFormat.setGroupingUsed(false);
        return numWithLeadingZerosFormat.format(ID);
    }

    /**
     * Recursively searches for the highest ID, which is the greatest slot file
     * name currently used in the store.
     * 
     * @param dir
     *            the directory to search
     * @param depth
     *            the subdirectory depth level of the dir
     * @return the highest slot file name / ID currently stored
     */
    private String findMaxID(final FileObject dir, final int depth) throws FileSystemException {
        final FileObject[] children = dir.getChildren();

        if (children.length == 0) {
            return null;
        }

        Arrays.sort(children, new MCRFileObjectComparator());

        if (depth == slotLength.length) {
            return children[children.length - 1].getName().getBaseName();
        }

        for (int i = children.length - 1; i >= 0; i--) {
            final FileObject child = children[i];
            if (!child.getType().hasChildren()) {
                continue;
            }
            final String found = findMaxID(child, depth + 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public interface MCRStoreConfig {
        String getBaseDir();
        String getID();
        String getSlotLayout();
    }
}
