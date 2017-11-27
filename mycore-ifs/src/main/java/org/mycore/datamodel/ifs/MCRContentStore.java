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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;

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
    protected String storeConfigPrefix;

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
        storeConfigPrefix = "MCR.IFS.ContentStore." + storeID + ".";
        slotDirDepth = MCRConfiguration.instance().getInt(storeConfigPrefix + "SlotDirDepth", 2);
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
     * MCRContentInputStream. Returns a StorageID to identify the place where
     * the content was stored.
     * 
     * @param file
     *            the MCRFile thats content is to be stored
     * @param source
     *            the ContentInputStream where the file content is read from
     * @return an ID that indentifies the place where the content was stored
     */
    public String storeContent(MCRFile file, MCRContentInputStream source) throws MCRPersistenceException {
        try {
            return doStoreContent(file, source);
        } catch (Exception exc) {
            if (!(exc instanceof MCRException)) {
                String msg = "Could not store content of file [" + file.getPath() + "] in store [" + storeID + "]";
                throw new MCRPersistenceException(msg, exc);
            }
            throw (MCRException) exc;
        }
    }

    /**
     * checks md5 sum
     */
    public boolean isValid(MCRFile file) throws IOException {
        if (!exists(file)) {
            return false;
        }
        return MCRUtils.getMD5Sum(doRetrieveMCRContent(file).getInputStream()).equals(file.getMD5());

    }

    /**
     * Checks if the content of the file is accessible.
     */
    protected abstract boolean exists(MCRFile file);

    /**
     * Stores the content of an MCRFile by reading from an
     * MCRContentInputStream. Returns a StorageID to identify the place where
     * the content was stored.
     * 
     * @param file
     *            the MCRFile thats content is to be stored
     * @param source
     *            the ContentInputStream where the file content is read from
     * @return an ID that indentifies the place where the content was stored
     */
    protected abstract String doStoreContent(MCRFile file, MCRContentInputStream source) throws Exception;

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
                String msg = "Could not delete content of file with storage ID [" + storageID + "] in store [" + storeID
                    + "]";
                throw new MCRPersistenceException(msg, exc);
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
     * Retrieves the content of an MCRFile. Uses the
     * StorageID to indentify the place where the file content was stored in
     * this store instance.
     * 
     * @param file
     *            the MCRFile thats content should be retrieved
     * @since 2.2
     */
    protected abstract MCRContent doRetrieveMCRContent(MCRFile file) throws IOException;

    /**
     * Retrieves the content of an MCRFile as an InputStream.
     * 
     * @param file
     *            the MCRFile thats content should be retrieved
     */
    public InputStream retrieveContent(MCRFile file) throws MCRException {
        try {
            return doRetrieveMCRContent(file).getInputStream();
        } catch (Exception exc) {
            if (!(exc instanceof MCRException)) {
                String msg = "Could not retrieve content of file with storage ID [" + file.getStorageID()
                    + "] in store ["
                    + storeID + "]";
                throw new MCRPersistenceException(msg, exc);
            }
            throw (MCRException) exc;
        }
    }

    /**
     * Returns the local java.io.File that really stores the content of the MCRFile 
     */
    public File getLocalFile(MCRFile reader) throws IOException {
        return getLocalFile(reader.getStorageID());
    }

    /**
     * Returns the local java.io.File that really stores the content of the MCRFile 
     */
    public abstract File getLocalFile(String storageId) throws IOException;

    /**
     * Returns the base dir as {@link File} if available or null if the base directory is no local file.
     * 
     * All files handled by this content store instance must resist under this directory.
     */
    public abstract File getBaseDir() throws IOException;

    /** DateFormat used to construct new unique IDs based on timecode */
    protected static DateFormat formatter = new SimpleDateFormat("yyMMdd-HHmmss-SSS", Locale.ROOT);

    /**
     * Constructs a new unique ID for storing content
     */
    protected static synchronized String buildNextID(MCRFile file) {
        StringBuilder sb = new StringBuilder();

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

        return lastTimestamp = ts;
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
            if (slot.length() < 2) {
                slot = "0" + slot;
            }
            slots[i] = slot;
        }

        return slots;
    }

}
