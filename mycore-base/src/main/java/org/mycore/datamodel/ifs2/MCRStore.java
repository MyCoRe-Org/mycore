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
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;

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
 * 
 * MCR.IFS2.Store.ID.BaseDir=/foo/bar
 * 
 * MCR.IFS2.Store.ID.SlotLayout=4-2-2
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRStore {

    /**
     * Map of defined stores, where store ID is the map key.
     */
    protected static HashMap<String, MCRStore> stores = new HashMap<String, MCRStore>();

    /**
     * Returns the store with the given ID
     * 
     * @param ID
     *            the ID of the store
     */
    protected static MCRStore getStore(String ID) {
        return stores.get(ID);
    }
    
    protected static <T extends MCRStore> T getStore(String ID, Class<T> storeClass) {
    	MCRStore retrievedStore = stores.get(ID);
    	if(storeClass.isAssignableFrom(retrievedStore.getClass())){
    		return (T) retrievedStore;
    	}
    	
    	return null;
    }
    
    
    

	public static <T extends MCRStore> T createStore(String ID, Class<T> storeClass) throws Exception {
		if (stores.containsKey(ID)) {
			throw new MCRException("Could not create store with ID " + ID + ", store allready exists");
		}
		
		T store = storeClass.newInstance();
		store.init(ID);
		
		return getStore(ID, storeClass);
	}

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
    protected String prefix = "";

    /** The suffix of slot names */
    protected String suffix = "";

    /**
     * Initializes a new store instance
     */
    protected void init(String id) {
        stores.put(id, this);
        this.id = id;

        String cfg = "MCR.IFS2.Store." + id + ".";
        MCRConfiguration config = MCRConfiguration.instance();
        String baseDir = config.getString(cfg + "BaseDir");
        String slotLayout = config.getString(cfg + "SlotLayout");

        idLength = 0;

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
     * Returns the slot file object used to store data for the given ID. This
     * may be a file or directory, depending on the subclass of MCRStore that is
     * used.
     * 
     * @param ID
     *            the ID of the data
     * @return the file object storing that data
     */
    FileObject getSlot(int ID) throws IOException {
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
        String[] paths = getSlotPaths(ID);
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
    String[] getSlotPaths(int ID) {
		String id = createIDWithLeadingZeros(ID);
        
        String[] paths = new String[slotLength.length + 1];
        StringBuffer path = new StringBuffer();
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

	private String createIDWithLeadingZeros(int ID) {
		DecimalFormat numWithLeadingZerosFormat = new DecimalFormat();
		numWithLeadingZerosFormat.setMinimumIntegerDigits(idLength);
		numWithLeadingZerosFormat.setGroupingUsed(false);
        String id = numWithLeadingZerosFormat.format(ID);
		return id;
	}

    /**
     * Returns true if data for the given ID is existing in the store.
     * 
     * @param id
     *            the ID of the data
     * @return true, if data for the given ID is existing in the store.
     */
    public boolean exists(int id) throws IOException {
        return getSlot(id).exists();
    }

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

    public synchronized int getHighestStoredID() {
        int found = 0;
        String max = findMaxID(dir, 0);
        if (max != null) {
            found = slot2id(max);
        }
        return found;
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

        if (children == null || children.length == 0) {
            return null;
        }

        Arrays.sort(children);

        if (depth == slotLength.length) {
            return children[children.length - 1];
        }

        for (int i = children.length - 1; i >= 0; i--) {
            File child = new File(dir, children[i]);
            if (!child.isDirectory()) {
                continue;
            }
            String found = findMaxID(child, depth + 1);
            if (found != null) {
                return found;
            }
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
    public Iterator<Integer> listIDs(boolean order) {
        return new Iterator<Integer>() {
            /**
             * List of files or directories in store not yet handled
             */
            List<File> files = new ArrayList<File>();

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

            /**
             * Initializes the enumeration and searches for the first ID to
             * return
             * 
             * @param order
             *            the return order, ascending or descending
             */
            Iterator<Integer> init(boolean order) {
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
                if (children == null || children.length == 0) {
                    return;
                }

                Arrays.sort(children);
                for (int i = 0; i < children.length; i++) {
                    files.add((order ? i : 0), new File(dir, children[i]));
                }
            }

            public boolean hasNext() {
                return nextID > 0;
            }

            public Integer next() {
                if (nextID < 1) {
                    throw new NoSuchElementException();
                }

                lastID = nextID;
                nextID = findNextID();
                return lastID;
            }

            public void remove() {
                if (lastID == 0) {
                    throw new IllegalStateException();
                }
                try {
                    MCRStore.this.delete(lastID);
                } catch (Exception ex) {
                    throw new MCRException("Could not delete " + MCRStore.this.getID() + " " + lastID, ex);
                }
                lastID = 0;
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

                File first = files.remove(0);
                if (first.getName().length() == idLength + prefix.length() + suffix.length()) {
                    return MCRStore.this.slot2id(first.getName());
                }

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
    public void delete(int id) throws IOException {
        delete(getSlot(id));
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

        FileObject base = VFS.getManager().resolveFile(dir.getAbsolutePath());
        while (!parent.equals(base)) {
            FileObject[] children = parent.getChildren();
            if (children.length > 0) {
                break;
            }
            fo = parent;
            parent = fo.getParent();
            fo.delete();
        }
    }
    
    public boolean isEmpty() {
		if (dir.list() == null) {
			return true;
		} else {
			return dir.list().length == 0;
		}
	}

	public void remove(String id) {
		stores.remove(id);
	}
}