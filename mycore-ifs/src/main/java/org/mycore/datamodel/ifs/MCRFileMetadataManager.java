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

package org.mycore.datamodel.ifs;

import java.util.GregorianCalendar;
import java.util.List;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;

/**
 * The single instance of this class is used by the other IFS classes to create,
 * retrieve, update or delete the metadata of all MCRFilesystemNode objects in a
 * persistent datastore. The manager holds a cache of all MCRFilesystemNodes
 * most recently used. It creates the MCRFileMetadataStore instance as
 * configured in the MyCoRe properties and hides its usage from the other
 * classes.
 *
 * Configuration properties:
 * <ul>
 * <li><b>MCR.IFS.FileMetadataStore.Class: </b> The class that implements the
 * MCRFileMetadataStore interface and that should be used to store the
 * persistent metadata.</li>
 * <li><b>MCR.Persistence.IFS.FileMetadataStore.CacheSize: </b> The size of the cache
 * holding the most recently used MCRFilesystemNodes expressed as maximum number
 * of node objects in the cache.</li>
 * </ul>
 *
 * @see MCRFilesystemNode
 * @see MCRFileMetadataStore
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRFileMetadataManager {

    /** The single instance of this class * */
    private static MCRFileMetadataManager manager;

    /** Returns the single instance of this class to use * */
    public static synchronized MCRFileMetadataManager instance() {
        if (manager == null) {
            manager = new MCRFileMetadataManager();
        }

        return manager;
    }

    /** Cache containing the most recently used MCRFilesystemNode objects * */
    private MCRCache<String, MCRFilesystemNode> cache;

    /** The store that holds all saved MCRFilesystemNode metadata * */
    private MCRFileMetadataStore store;

    /** Creates the single instance of this class * */
    private MCRFileMetadataManager() {
        MCRConfiguration config = MCRConfiguration.instance();

        // The FileMetadataStore to use
        store = config.getInstanceOf("MCR.Persistence.IFS.FileMetadataStore.Class");

        // The cache size for the MCRFilesystemNode cache
        int size = config.getInt("MCR.IFS.FileMetadataStore.CacheSize", 500);
        cache = new MCRCache<>(size, "IFS FileSystemNodes");
    }

    /**
     * Last number that was used for creating a unique ID for each
     * MCRFilesystemNode
     */
    private long last_number = System.currentTimeMillis();

    /**
     * Prefix for all generated IDs that is derived from the server's IP address
     * to ensure that different MyCoRe instances will create different unique
     * IDs for each MCRFilesystemNode
     */
    private String prefix;

    /**
     * Creates a prefix for all generated IDs. This ID is derived from the
     * server's IP address to ensure that different MyCoRe instances will create
     * different unique IDs for each MCRFilesystemNode
     */
    private String getIDPrefix() {
        if (prefix == null) {
            String ip = "127.0.0.1";

            try {
                ip = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (java.net.UnknownHostException ignored) {
            }

            java.util.StringTokenizer st = new java.util.StringTokenizer(ip, ".");

            long sum = Integer.parseInt(st.nextToken());

            while (st.hasMoreTokens()) {
                sum = (sum << 8) + Integer.parseInt(st.nextToken());
            }

            String address = Long.toString(sum, 36);
            address = "000000" + address;
            prefix = address.substring(address.length() - 6);
        }

        return prefix;
    }

    /**
     * Creates a new, unique ID for each MCRFilesystemNode
     */
    public synchronized String createNodeID() {
        String time = "0000000000" + Long.toString(last_number++, 36);

        StringBuilder sb = new StringBuilder(getIDPrefix());
        sb.append(time.substring(time.length() - 10));
        sb.reverse();
        return sb.toString();
    }

    /**
     * Creates or updates the data of the MCRFilesystemNode in the persistent
     * MCRFileMetadataStore.
     *
     * @param node
     *            the MCRFilesystemNode to store
     */
    public void storeNode(MCRFilesystemNode node) throws MCRPersistenceException {
        store.storeNode(node);
        cache.put(node.getID(), node);
    }

    /**
     * Retrieves the MCRFilesystemNode with the given ID from the persistent
     * MCRFileMetadataStore.
     *
     * @param ID
     *            the unique ID of the MCRFilesystemNode
     * @return the MCRFilesystemNode with that ID, or null if no such node
     *         exists.
     */
    MCRFilesystemNode retrieveNode(String ID) throws MCRPersistenceException {
        MCRFilesystemNode n = cache.get(ID);
        return n != null ? n : store.retrieveNode(ID);
    }

    /**
     * Retrieves the first MCRFilesystemNode found in the persistent
     * MCRFileMetadataStore, that has the given owner ID. This is assumed to be
     * a root node.
     *
     * @param ownerID
     *            the ID of the owner of the MCRFilesystemNode to be retrieved
     * @return the MCRFilesystemNode with that owner, or null if no such node
     *         exists.
     */
    MCRFilesystemNode retrieveRootNode(String ownerID) throws MCRPersistenceException {
        String ID = store.retrieveRootNodeID(ownerID);

        return ID == null ? null : retrieveNode(ID);
    }

    /**
     * Retrieves a child node of a given MCRDirectory node.
     *
     * @param parentID
     *            the ID of the parent MCRDirectory
     * @param name
     *            the file name of the child in that directory
     * @return the child MCRFilesystemNode, or null, if no such child exists
     */
    public MCRFilesystemNode retrieveChild(String parentID, String name) throws MCRPersistenceException {
        return store.retrieveChild(parentID, name);
    }

    /**
     * Callback method for internal use by any MCRFileMetadataStore
     * implementation, do not use. Builds a MCRFilesystemNode object from the
     * raw data that is retrieved from the persistent store, or uses the
     * existing copy in the MCRCache instance.
     */
    public MCRFilesystemNode buildNode(String type, String ID, String parentID, String ownerID, String name,
        String label, long size,
        GregorianCalendar date, String storeID, String storageID, String fctID, String md5, int numchdd, int numchdf,
        int numchtd,
        int numchtf) throws MCRPersistenceException {
        MCRFilesystemNode n = cache.get(ID);

        if (n != null) {
            return n;
        }

        if (type.equals("D")) {
            n = new MCRDirectory(ID, parentID, ownerID, name, label, size, date, numchdd, numchdf, numchtd, numchtf);
        } else {
            n = new MCRFile(ID, parentID, ownerID, name, label, size, date, storeID, storageID, fctID, md5);
        }

        cache.put(ID, n);

        return n;
    }

    /**
     * Retrieves a list of all child MCRFilesystemNodes of a given
     * MCRDirectory.
     *
     * @param ID
     *            the ID of the parent MCRDirectory
     * @return a List of all children of that MCRDirectory
     */
    List<MCRFilesystemNode> retrieveChildren(String ID) throws MCRPersistenceException {
        return store.retrieveChildren(ID);
    }

    /**
     * Deletes a MCRFilesystemNode in the persistent MCRFileMetadataStore.
     *
     * @param ID
     *            the ID of the node to delete from the store
     */
    void deleteNode(String ID) throws MCRPersistenceException {
        cache.remove(ID);
        store.deleteNode(ID);
    }

    void clearMetadataCache() {
        cache.clear();
    }

    /**
     * Returns an object to iterate over the owner IDs.
     */
    public Iterable<String> getOwnerIDs() {
        return store.getOwnerIDs();
    }
}
