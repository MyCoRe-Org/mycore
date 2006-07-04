/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.datamodel.ifs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;

/**
 * Stores the content of MCRFiles in a persistent datastore. This can be a
 * filesystem, IBM Content Manager, video streaming servers like IBM
 * VideoCharger or Real Server, depending on the class that implements this
 * interface. The MCRContentStore provides methods to store, delete and retrieve
 * content. It uses a storage ID and the store ID to identify the place where
 * the content of a file is stored.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public abstract class MCRContentStore {
    /** The unique store ID for this MCRContentStore implementation */
    protected String storeID;

    /** The prefix of all properties in mycore.properties for this store */
    protected String prefix;

    /** The depth of slot subdirectories to build */
    protected int slotDirDepth;

    /** Default constructor * */
    public MCRContentStore() {
    }

    /**
     * Initializes the store and sets its unique store ID. MCRFiles must
     * remember this ID to indentify the store that holds their file content.
     * The store ID is set by MCRContentStoreFactory when a new store instance
     * is built. Subclasses should override this method.
     * 
     * @param storeID
     *            the non-null unique store ID for this store instance
     */
    public void init(String storeID) {
        this.storeID = storeID;
        this.prefix = "MCR.IFS.ContentStore." + storeID + ".";
        this.slotDirDepth = MCRConfiguration.instance().getInt(prefix + "SlotDirDepth", 2);
    }

    /**
     * Returns the unique store ID that was set for this store instance
     * 
     * @return the unique store ID that was set for this store instance
     */
    public String getID() {
        return storeID;
    }

    /**
     * Stores the content of an MCRFile by reading from an
     * MCRContentInputStream. Returns a StorageID to indentify the place where
     * the content was stored.
     * 
     * @param file
     *            the MCRFile thats content is to be stored
     * @param source
     *            the ContentInputStream where the file content is read from
     * @return an ID that indentifies the place where the content was stored
     */
    public String storeContent(MCRFileReader file, MCRContentInputStream source) throws MCRPersistenceException {
        try {
            return doStoreContent(file, source);
        } catch (Exception exc) {
            if (!(exc instanceof MCRException)) {
                StringBuffer msg = new StringBuffer();
                msg.append("Could not store content of file [");
                msg.append(file.getPath()).append("] in store [");
                msg.append(storeID).append("]");
                throw new MCRPersistenceException(msg.toString(), exc);
            }
            throw (MCRException) exc;
        }
    }

    /**
     * Stores the content of an MCRFile by reading from an
     * MCRContentInputStream. Returns a StorageID to indentify the place where
     * the content was stored.
     * 
     * @param file
     *            the MCRFile thats content is to be stored
     * @param source
     *            the ContentInputStream where the file content is read from
     * @return an ID that indentifies the place where the content was stored
     */
    protected abstract String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception;

    /**
     * Deletes the content of an MCRFile object that is stored under the given
     * Storage ID in this store instance.
     * 
     * @param storageID
     *            the storage ID of the MCRFile object
     */
    public void deleteContent(String storageID) throws MCRException {
        try {
            doDeleteContent(storageID);
        } catch (Exception exc) {
            if (!(exc instanceof MCRException)) {
                StringBuffer msg = new StringBuffer();
                msg.append("Could not delete content of file with storage ID [");
                msg.append(storageID).append("] in store [");
                msg.append(storeID).append("]");
                throw new MCRPersistenceException(msg.toString(), exc);
            }
            throw (MCRException) exc;
        }
    }

    /**
     * Deletes the content of an MCRFile object that is stored under the given
     * Storage ID in this store instance.
     * 
     * @param storageID
     *            the storage ID of the MCRFile object
     */
    protected abstract void doDeleteContent(String storageID) throws Exception;

    /**
     * Retrieves the content of an MCRFile to an OutputStream. Uses the
     * StorageID to indentify the place where the file content was stored in
     * this store instance.
     * 
     * @param file
     *            the MCRFile thats content should be retrieved
     * @param target
     *            the OutputStream to write the file content to
     */
    public void retrieveContent(MCRFileReader file, OutputStream target) throws MCRException {
        try {
            doRetrieveContent(file, target);
        } catch (Exception exc) {
            if (!(exc instanceof MCRException)) {
                StringBuffer msg = new StringBuffer();
                msg.append("Could not retrieve content of file with storage ID [");
                msg.append(file.getStorageID()).append("] in store [");
                msg.append(storeID).append("]");
                throw new MCRPersistenceException(msg.toString(), exc);
            }
            throw (MCRException) exc;
        }
    }

    /**
     * Retrieves the content of an MCRFile to an OutputStream. Uses the
     * StorageID to indentify the place where the file content was stored in
     * this store instance.
     * 
     * @param file
     *            the MCRFile thats content should be retrieved
     * @param target
     *            the OutputStream to write the file content to
     * @deprecated
     *        use doRetrieveContent(MCRFileReader file) instead
     */
    protected abstract void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception;

    /**
     * Retrieves the content of an MCRFile. Uses the
     * StorageID to indentify the place where the file content was stored in
     * this store instance.
     * 
     * @param file
     *            the MCRFile thats content should be retrieved
     * @since 1.3
     */
    protected abstract InputStream doRetrieveContent(MCRFileReader file) throws Exception;

    /**
     * Retrieves the content of an MCRFile as an InputStream. The default
     * implementation buffers all bytes in memory to implement this
     * functionality, so subclasses should overwrite this method to get better
     * performance and less memory consumption.
     * 
     * @param file
     *            the MCRFile thats content should be retrieved
     */
    public InputStream retrieveContent(MCRFileReader file) throws MCRException, IOException {
        return retrieveContent(file);
    }

    /** DateFormat used to construct new unique IDs based on timecode */
    protected static DateFormat formatter = new SimpleDateFormat("yyMMdd-HHmmss-SSS");

    /**
     * Constructs a new unique ID for storing content
     */
    protected static synchronized String buildNextID(MCRFileReader file) {
        StringBuffer sb = new StringBuffer();

        sb.append(buildNextTimestamp());
        sb.append("-").append(file.getID());

        if (file.getExtension().length() > 0) {
            sb.append(".").append(file.getExtension());
        }

        return sb.toString();
    }

    /** The last timestamp that was constructed */
    protected static String lastTimestamp = null;

    /**
     * Helper method for constructing a unique storage ID from a timestamp.
     */
    protected static synchronized String buildNextTimestamp() {
        String ts = null;

        do {
            ts = formatter.format(new Date());
        } while (ts.equals(lastTimestamp));

        return (lastTimestamp = ts);
    }

    /**
     * Some content store implementations store the file's content in a
     * hierarchical directory structure of the server's filesystem. Such stores
     * use a directory that contains 100 subdirectories with each 100
     * subsubdirectories, so that the internal directory operations will scale
     * well for large file collections. The depth of this subdirectory structure
     * can be set by the property SlotDirDepth, default is 2.
     * 
     * This helper method randomly chooses the "slot directory" to be used for
     * the next storage.
     * 
     * @return directory names between "00" and "99" that are the "slot" where
     *         to store the file's content in the filesystem.
     */
    protected String[] buildSlotPath() {
        Random random = new Random();
        String[] slots = new String[slotDirDepth];

        for (int i = 0; i < slotDirDepth; i++) {
            String slot = String.valueOf(random.nextInt(100));
            if (slot.length() < 2)
                slot = "0" + slot;
            slots[i] = slot;
        }

        return slots;
    }
}
