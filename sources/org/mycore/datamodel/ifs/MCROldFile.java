/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import java.util.*;
import java.io.*;
import javax.servlet.http.*;

/**
 * Represents a stored file with its metadata and content. USED BY MILESS 1.3
 * ONLY!
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCROldFile implements MCRFileReader {
    /** The ID of the store that holds this file's content */
    protected String storeID;

    /** The ID that identifies the place where the store holds the content */
    protected String storageID;

    /** The ID of the file owner, e .g. a MILESS derivate ID */
    protected String ownerID;

    /** The ID of the content type of this file */
    protected String contentTypeID;

    /** The file path */
    protected String path;

    /** The md5 checksum that was built when content was read for this file */
    protected String md5;

    /** The file size in number of bytes */
    protected long size;

    /** The date of last modification of this file */
    protected GregorianCalendar lastModified;

    /** The optional extender for streaming audio/video files */
    protected MCRAudioVideoExtender avExtender;

    /**
     * Creates a new empty, unstored MCROldFile instance.
     */
    public MCROldFile() {
        storeID = "";
        storageID = "";
        ownerID = "";
        path = "";
        size = 0;
        contentTypeID = "unknown";
        md5 = "d41d8cd98f00b204e9800998ecf8427e";
        lastModified = new GregorianCalendar();
    }

    /**
     * Sets the ID of the owner of this file
     * 
     * @param ID
     *            the non-empty owner ID
     */
    public void setOwnerID(String ID) {
        MCRArgumentChecker.ensureNotEmpty(ID, "ID");
        this.ownerID = ID;
    }

    /**
     * Returns the ID of the owner of this file
     * 
     * @return the ID of the owner of this file
     */
    public String getOwnerID() {
        return ownerID;
    }

    public String getID() {
        return ownerID;
    }

    /**
     * Sets the relative path of this file
     */
    public void setPath(String path) {
        MCRArgumentChecker.ensureNotEmpty(path, "path");
        this.path = path;
    }

    /**
     * Returns the relative path of this file
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the filename of this file
     */
    public String getFileName() {
        int pos = path.lastIndexOf(File.separator);
        return (pos == -1 ? path : path.substring(pos + 1));
    }

    /**
     * Returns the directory of this file
     */
    public String getDirectory() {
        int pos = path.lastIndexOf(File.separator);
        return (pos == -1 ? "" : path.substring(0, pos));
    }

    /**
     * Returns the file extension of this file, or an empty string if the file
     * has no extension
     */
    public String getExtension() {
        if (path.endsWith("."))
            return "";

        int pos = path.lastIndexOf(".");
        return (pos == -1 ? "" : path.substring(pos + 1));
    }

    /**
     * Sets the file size
     */
    public void setSize(long size) {
        MCRArgumentChecker.ensureNotNegative(size, "size");
        this.size = size;
    }

    /**
     * Returns the file size as number of bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the file size, formatted as a string
     */
    public String getSizeFormatted() {
        return getSizeFormatted(size);
    }

    /**
     * Takes a file size in bytes and formats it as a string for output
     */
    public static String getSizeFormatted(long bytes) {
        String sizeUnit;
        String sizeText;
        double sizeValue;

        if (bytes >= 1024 * 1024) // >= 1 MB
        {
            sizeUnit = "MB";
            sizeValue = (double) (Math.round((double) bytes / 10485.76)) / 100;
        } else if (bytes >= 5 * 1024) // >= 5 KB
        {
            sizeUnit = "KB";
            sizeValue = (double) (Math.round((double) bytes / 102.4)) / 10;
        } else // < 5 KB
        {
            sizeUnit = "Byte";
            sizeValue = (double) bytes;
        }

        sizeText = String.valueOf(sizeValue).replace('.', ',');
        if (sizeText.endsWith(",0"))
            sizeText = sizeText.substring(0, sizeText.length() - 2);

        return sizeText + " " + sizeUnit;
    }

    /**
     * Sets the MD5 checksum for this file
     */
    public void setChecksum(String md5) {
        MCRArgumentChecker.ensureNotEmpty(md5, "md5 checksum");
        this.md5 = md5;
    }

    /**
     * Returns the MD5 checksum for this file
     */
    public String getChecksum() {
        return md5;
    }

    /**
     * Sets the time of last modification of this file
     */
    public void setLastModified(GregorianCalendar date) {
        MCRArgumentChecker.ensureNotNull(date, "date");
        this.lastModified = date;
    }

    /**
     * Returns the time of last modification of this file
     */
    public GregorianCalendar getLastModified() {
        return lastModified;
    }

    /**
     * Sets the ID of the MCRContentStore implementation that holds the content
     * of this file
     */
    public void setStoreID(String ID) {
        MCRArgumentChecker.ensureNotNull(ID, "ID");
        this.storeID = ID.trim();
    }

    /**
     * Returns the ID of the MCRContentStore implementation that holds the
     * content of this file
     */
    public String getStoreID() {
        return storeID;
    }

    /**
     * Sets the storage ID that identifies the place where the MCRContentStore
     * has stored the content of this file
     */
    public void setStorageID(String ID) {
        MCRArgumentChecker.ensureNotNull(ID, "ID");
        this.storageID = ID.trim();
    }

    /**
     * Returns the storage ID that identifies the place where the
     * MCRContentStore has stored the content of this file
     */
    public String getStorageID() {
        return storageID;
    }

    /**
     * Returns the MCRContentStore instance that holds the content of this file
     */
    protected MCRContentStore getContentStore() {
        if (storeID.length() == 0)
            return null;
        else
            return MCRContentStoreFactory.getStore(storeID);
    }

    /**
     * Reads the content of this file from the source ContentInputStream and
     * stores it in the ContentStore given
     */
    public void setContentFrom(MCRContentInputStream source,
            MCRContentStore store) throws MCRPersistenceException {
        if (source.getHeader().length == 0) {
            storageID = "";
            storeID = "";
        } else {
            storageID = store.storeContent(this, source);
            storeID = store.getID();
        }

        size = source.getLength();
        md5 = source.getMD5String();
    }

    /**
     * Deletes the content of this file from the ContentStore used
     */
    public void deleteContent() throws MCRPersistenceException {
        if (storageID.length() != 0)
            getContentStore().deleteContent(storageID);

        storageID = "";
        storeID = "";
        contentTypeID = "unknown";
        md5 = "d41d8cd98f00b204e9800998ecf8427e";
        size = 0;
        lastModified = new GregorianCalendar();
    }

    /**
     * Writes the content of this file to a target output stream
     */
    public void getContentTo(OutputStream target)
            throws MCRPersistenceException {
        if (storageID.length() != 0)
            getContentStore().retrieveContent(this, target);
    }

    /**
     * Writes the content of this file to a file on the local filesystem
     */
    public void getContentTo(File target) throws MCRPersistenceException,
            IOException {
        getContentTo(new FileOutputStream(target));
    }

    /**
     * Gets the content of this file as a byte array
     */
    public byte[] getContentAsByteArray() throws MCRPersistenceException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            getContentTo(baos);
            baos.close();
        } catch (IOException willNotBeThrown) {
        }
        return baos.toByteArray();
    }

    /**
     * Gets the content of this file as a string, using the default encoding
     */
    public String getContentAsString() throws MCRPersistenceException {
        return new String(getContentAsByteArray());
    }

    /**
     * Gets the content of this file as a string, using the given encoding
     */
    public String getContentAsString(String encoding)
            throws MCRPersistenceException, UnsupportedEncodingException {
        return new String(getContentAsByteArray(), encoding);
    }

    /**
     * Returns true, if this file is stored in a content store that provides an
     * MCRAudioVideoExtender for audio/video streaming and additional metadata
     */
    public boolean hasAudioVideoExtender() {
        if (storeID.length() == 0)
            return false;
        else
            return MCRContentStoreFactory.providesAudioVideoExtender(storeID);
    }

    /**
     * Returns the AudioVideoExtender in case this file is streaming audio/video
     * and stored in a ContentStore that supports this
     */
    public MCRAudioVideoExtender getAudioVideoExtender() {
        if (hasAudioVideoExtender() && (avExtender == null))
            avExtender = MCRContentStoreFactory.buildExtender(this);

        return avExtender;
    }

    /**
     * Sets the ID of the content type of this file
     */
    public void setContentTypeID(String ID) {
        MCRArgumentChecker.ensureNotEmpty(ID, "content type ID");
        this.contentTypeID = ID;
    }

    /**
     * Gets the ID of the content type of this file
     */
    public String getContentTypeID() {
        return contentTypeID;
    }

    /**
     * This method will throw an UnsupportedOperationException, it is not
     * implemented for MCROldFile class.
     */
    public MCRFileContentType getContentType() {
        throw new UnsupportedOperationException(
                "Not implemented for MCROldFile");
    }
}