/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRConfigurationException;

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
 * @author Frank Lützenkirchen
 */
public abstract class MCRStore {
    /** The ID of the store */
    protected String id;

    /** The base directory containing the stored data */
    protected File dir;

    /** The maximum length of IDs **/
    protected int idLength;

    /**
     * The slot subdirectory layout, which is the number of digits used at each
     * subdirectory level to build the filename.
     */
    protected int[] slotLength;

    /** The prefix of slot names */
    protected String prefix;

    /** The suffix of slot names */
    protected String suffix;

    /**
     * Creates a new store instance
     * 
     * @param id
     *            the ID of the store
     * @param baseDir
     *            the base directory containing the stored data
     * @param slotLayout
     *            the slot subdirectory layout (see class description above)
     * @param prefix
     *            the prefix of slot names
     * @param suffix
     *            the suffix of slot names
     */
    protected MCRStore(String id, String baseDir, String slotLayout, String prefix, String suffix) {
        this.id = id;
        this.prefix = prefix;
        this.suffix = suffix;

        this.idLength = 0;

        StringTokenizer st = new StringTokenizer(slotLayout, "-");
        slotLength = new int[st.countTokens() - 1];

        int i = 0;
        while (st.countTokens() > 1) {
            slotLength[i] = Integer.parseInt(st.nextToken());
            idLength += slotLength[i++];
        }
        idLength += Integer.parseInt(st.nextToken());

        dir = new File(baseDir);
        if (!dir.exists()) {
            try {
                boolean created = dir.mkdirs();
                if (!created) {
                    String msg = "Unable to create store directory " + baseDir;
                    throw new MCRConfigurationException(msg);
                }
            } catch (Exception ex) {
                String msg = "Exception while creating store directory " + baseDir;
                throw new MCRConfigurationException(msg, ex);
            }
        } else {
            if (!dir.canRead()) {
                String msg = "Store directory " + baseDir + " is not readable";
                throw new MCRConfigurationException(msg);
            }
            if (!dir.isDirectory()) {
                String msg = "Store " + baseDir + " is a file, not a directory";
                throw new MCRConfigurationException(msg);
            }
        }
    }

    /**
     * Returns the ID of this store
     */
    public String getID() {
        return id;
    }

    /**
     * Returns the absolute path of the local base directory
     * 
     * @return the base directory storing the data
     */
    String getBaseDir() {
        return dir.getAbsolutePath();
    }

    /**
     * Used to fill small IDs with leading zeros
     */
    private static String nulls = "00000000000000000000000000000000";

    /**
     * Returns the slot file object used to store data for the given ID. This
     * may be a file or directory, depending on the subclass of MCRStore that is
     * used.
     * 
     * @param ID
     *            the ID of the data
     * @return the file object storing that data
     */
    FileObject getSlot(int ID) throws Exception {
        return VFS.getManager().resolveFile(dir, getSlotPath(ID));
    }

    /**
     * Returns the relative path used to store data for the given ID within the
     * store base directory
     * 
     * @param ID
     *            the ID of the data
     * @return the relative path storing that data
     */
    String getSlotPath(int ID) {
        String[] parts = getSlotPathParts(ID);
        StringBuffer path = new StringBuffer();
        for (int i = 0; i < parts.length - 1; i++) {
            path.append(parts[i]);
            if (path.length() > 0)
                path.append('/');
        }
        path.append(parts[parts.length - 1]);
        return path.toString();
    }

    /**
     * Returns the parts of the relative path used to store data for the given
     * ID within the store base directory
     * 
     * @param ID
     *            the ID of the data
     * @return the directory and file names of the relative path storing that
     *         data
     */
    String[] getSlotPathParts(int ID) {
        String id = nulls + String.valueOf(ID);
        id = id.substring(id.length() - idLength);

        int offset = 0;
        String[] path = new String[slotLength.length + 1];
        for (int i = 0; i < slotLength.length; i++) {
            path[i] = id.substring(offset, offset + slotLength[i]);
            offset += slotLength[i];
        }
        path[path.length - 1] = prefix + id + suffix;
        return path;
    }

    /**
     * Returns true if data for the given ID is existing in the store.
     * 
     * @param id
     *            the ID of the data
     * @return true, if data for the given ID is existing in the store.
     */
    public boolean exists(int id) throws Exception {
        return getSlot(id).exists();
    }

    /**
     * Offset to add to the maximum ID found in the store to build the new ID.
     * This is normally 1, but initially higher to avoid rare conflicts when
     * batch import and creating objects through web application occur in
     * parallel.
     */
    protected int offset = 10; // Sicherheitsabstand, initially 10, later 1

    /**
     * The last ID assigned by this store.
     */
    protected int lastID = 0;

    /**
     * Returns the next free ID that can be used to store data. Call as late as
     * possible to avoid that another process, for example from batch import, in
     * the meantime already used that ID.
     * 
     * @return the next free ID that can be used to store data
     */
    public synchronized int getNextFreeID() {
        int found = 0;
        String max = findMaxID(dir, 0);
        if (max != null)
            found = slot2id(max);

        lastID = Math.max(found, lastID);
        lastID += (lastID > 0 ? offset : 1);
        offset = 1;
        return lastID;
    }

    /**
     * Extracts the numerical ID contained in the slot filename.
     * 
     * @param slot
     *            the file name of the slot containing the data
     * @return the ID of that data
     */
    private int slot2id(String slot) {
        slot = slot.substring(prefix.length());
        slot = slot.substring(0, idLength);
        return Integer.parseInt(slot);
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
    private String findMaxID(File dir, int depth) {
        String[] children = dir.list();

        if ((children == null) || (children.length == 0))
            return null;

        Arrays.sort(children);

        if (depth == slotLength.length)
            return children[children.length - 1];

        for (int i = children.length - 1; i >= 0; i--) {
            File child = new File(dir, children[i]);
            if (!child.isDirectory())
                continue;
            String found = findMaxID(child, depth + 1);
            if (found != null)
                return found;
        }
        return null;
    }

    /**
     * Indicates ascending order when listing IDs
     */
    public final static boolean ASCENDING = true;

    /**
     * Indicates descending order when listing IDs
     */
    public final static boolean DESCENDING = false;

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
    public Enumeration<Integer> listIDs(boolean order) {
        return new Enumeration<Integer>() {
            /**
             * List of files or directories in store not yet handled
             */
            List<File> files = new ArrayList<File>();

            /**
             * The next ID to return, when 0, all IDs have been returned
             */
            int nextID;

            /**
             * The order in which the IDs should be returned, ascending or
             * descending
             */
            boolean order;

            /**
             * Initializes the enumeration and searches for the first ID to
             * return
             * 
             * @param order
             *            the return order, ascending or descending
             */
            Enumeration<Integer> init(boolean order) {
                this.order = order;
                addChildren(dir);
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
             */
            private void addChildren(File dir) {
                String[] children = dir.list();
                if ((children == null) || (children.length == 0))
                    return;

                Arrays.sort(children);
                for (int i = 0; i < children.length; i++)
                    files.add((order ? i : 0), new File(dir, children[i]));
            }

            public boolean hasMoreElements() {
                return (nextID > 0);
            }

            public Integer nextElement() {
                int id = nextID;
                nextID = findNextID();
                return id;
            }

            /**
             * Finds the next ID used in the store.
             * 
             * @return the next ID, or 0 if there is no other ID any more
             */
            private int findNextID() {
                if (files.isEmpty())
                    return 0;

                File first = files.remove(0);
                if (first.getName().length() == idLength + prefix.length() + suffix.length())
                    return MCRStore.this.slot2id(first.getName());

                addChildren(first);
                return findNextID();
            }
        }.init(order);
    }

    /**
     * Deletes the data stored under the given ID from the store
     * 
     * @param id
     *            the ID of the document to be deleted
     */
    public void delete(int id) throws Exception {
        delete(getSlot(id));
    }

    /**
     * Deletes the data stored in the given file object from the store
     * 
     * @param fo
     *            the file object to be deleted
     */
    void delete(FileObject fo) throws Exception {
        FileObject parent = fo.getParent();
        fo.delete(Selectors.SELECT_ALL);

        FileObject base = VFS.getManager().resolveFile(dir.getAbsolutePath());
        while (!parent.equals(base)) {
            FileObject[] children = parent.getChildren();
            if (children.length > 0)
                break;
            fo = parent;
            parent = fo.getParent();
            fo.delete();
        }
    }
}