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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.GregorianCalendar;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRArgumentChecker;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;

/**
 * Represents a stored file with its metadata and content.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRFile extends MCRFilesystemNode implements MCRFileReader {
    /** The ID of the store that holds this file's content */
    protected String storeID;

    /** The ID that identifies the place where the store holds the content */
    protected String storageID;

    /** The ID of the content type of this file */
    protected String contentTypeID;

    /** The md5 checksum that was built when content was read for this file */
    protected String md5;

    /** The optional extender for streaming audio/video files */
    protected MCRAudioVideoExtender avExtender;

    /** Is true if this file is a new MCRFile and not retrieved from store * */
    protected boolean isNew;

    /**
     * Creates a new and empty root MCRFile with the given filename, belonging
     * to the given ownerID. The file is assumed to be a standalone "root file"
     * that has no parent directory.
     * 
     * @param name
     *            the filename of the new MCRFile
     * @param ownerID
     *            any ID String of the logical owner of this file
     */
    public MCRFile(String name, String ownerID) {
        super(name, ownerID);
        initContentFields();
        isNew = true;
        storeNew();
    }

    /**
     * Creates a new, empty MCRFile with the given filename in the parent
     * MCRDirectory.
     * 
     * @param name
     *            the filename of the new MCRFile
     * @param parent
     *            the parent directory that will contain the new child
     * 
     * @throws MCRUsageException
     *             if that directory already contains a child with that name
     */
    public MCRFile(String name, MCRDirectory parent) {
        super(name, parent);
        initContentFields();
        isNew = true;
        storeNew();
    }

    /**
     * Internal constructor, do not use on your own.
     */
    MCRFile(String ID, String parentID, String ownerID, String name, String label, long size, GregorianCalendar date, String storeID, String storageID, String fctID, String md5) {
        super(ID, parentID, ownerID, name, label, size, date);

        this.storageID = storageID;
        this.storeID = storeID;
        this.contentTypeID = fctID;
        this.md5 = md5;
        this.isNew = false;
    }

    /**
     * Returns the MCRFile with the given ID.
     * 
     * @param ID
     *            the unique ID of the MCRFile to return
     * @return the MCRFile with the given ID, or null if no such file exists
     */
    public static MCRFile getFile(String ID) {
        return (MCRFile) (MCRFilesystemNode.getNode(ID));
    }

    /**
     * Returns the root MCRFile that has no parent and is logically owned by the
     * object with the given ID.
     * 
     * @param ownerID
     *            the ID of the logical owner of that file
     * @return the root MCRFile stored for that owner ID, or null if no such
     *         file exists
     */
    public static MCRFile getRootFile(String ownerID) {
        return (MCRFile) (MCRFilesystemNode.getRootNode(ownerID));
    }

    /**
     * Sets initial values for the fields of a new, empty MCRFile
     */
    private void initContentFields() {
        storageID = "";
        storeID = "";
        contentTypeID = MCRFileContentTypeFactory.getDefaultType().getID();
        md5 = "d41d8cd98f00b204e9800998ecf8427e"; // md5 of empty file
        size = 0;
        avExtender = null;
    }

    /**
     * Returns the file extension of this file's name
     * 
     * @return the file extension, or an empty string if the file has no
     *         extension
     */
    public String getExtension() {
        ensureNotDeleted();

        if (name.endsWith(".")) {
            return "";
        }

        int pos = name.lastIndexOf(".");

        return ((pos == -1) ? "" : name.substring(pos + 1));
    }

    /**
     * Returns the MD5 checksum for this file
     */
    public String getMD5() {
        ensureNotDeleted();

        return md5;
    }

    /**
     * Returns the ID of the MCRContentStore implementation that holds the
     * content of this file
     */
    public String getStoreID() {
        ensureNotDeleted();

        return storeID;
    }

    /**
     * Returns the storage ID that identifies the place where the
     * MCRContentStore has stored the content of this file
     */
    public String getStorageID() {
        ensureNotDeleted();

        return storageID;
    }

    /**
     * Returns the MCRContentStore instance that holds the content of this file
     * 
     * @return the MCRContentStore instance that holds the content of this file,
     *         or null if no content is stored
     */
    protected MCRContentStore getContentStore() {
        if (storeID.length() == 0) {
            return null;
        }
        return MCRContentStoreFactory.getStore(storeID);
    }

    /**
     * Reads the content of this file from a java.lang.String and stores its
     * text as bytes, encoded in the default encoding of the platform where this
     * is running.
     * 
     * @param source
     *            the String that is the file's content
     */
    public void setContentFrom(String source) throws MCRPersistenceException {
        MCRArgumentChecker.ensureNotNull(source, "source string");

        byte[] bytes = source.getBytes();

        setContentFrom(bytes);
    }

    /**
     * Reads the content of this file from a java.lang.String and stores its
     * text as bytes, encoded in the encoding given, in an MCRContentStore.
     * 
     * @param source
     *            the String that is the file's content
     * @param encoding
     *            the character encoding to use to store the String as bytes
     */
    public void setContentFrom(String source, String encoding) throws MCRPersistenceException, UnsupportedEncodingException {
        MCRArgumentChecker.ensureNotNull(source, "source string");
        MCRArgumentChecker.ensureNotNull(source, "source string encoding");

        byte[] bytes = source.getBytes(encoding);

        setContentFrom(bytes);
    }

    /**
     * Reads the content of this file from a source file in the local filesystem
     * and stores it in an MCRContentStore.
     * 
     * @param source
     *            the file in the local host's filesystem thats content should
     *            be imported
     */
    public void setContentFrom(File source) throws MCRPersistenceException {
        MCRArgumentChecker.ensureNotNull(source, "source file");
        MCRArgumentChecker.ensureIsTrue(source.exists(), "source file does not exist:" + source.getPath());
        MCRArgumentChecker.ensureIsTrue(source.canRead(), "source file not readable:" + source.getPath());
        FileInputStream fin = null;

        try {
            fin = new FileInputStream(source);
        } catch (FileNotFoundException ignored) {
        } // We already checked it exists

        setContentFrom(new BufferedInputStream(fin));
    }

    /**
     * Reads the content of this file from a byte array and stores it in an
     * MCRContentStore.
     * 
     * @param source
     *            the file's content
     */
    public void setContentFrom(byte[] source) throws MCRPersistenceException {
        MCRArgumentChecker.ensureNotNull(source, "source byte array");

        setContentFrom(new ByteArrayInputStream(source));
    }

    /**
     * Sets the content of this file from a JDOM xml document.
     * 
     * @param xml
     *            the JDOM xml document that should be stored as file content
     */
    public void setContentFrom(Document xml) {
        MCRArgumentChecker.ensureNotNull(xml, "jdom xml document");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new XMLOutputter().output(xml, baos);
            baos.close();
        } catch (IOException ignored) {
        }
        setContentFrom(baos.toByteArray());
    }

    /**
     * Reads the content of this file from the source InputStream and stores it
     * in an MCRContentStore. InputStream does NOT get closed at end of process,
     * this must be done by invoking code if required/appropriate.
     * 
     * @param source
     *            the source for the file's content bytes
     */
    public void setContentFrom(InputStream source) throws MCRPersistenceException {
        ensureNotDeleted();

        String old_md5 = this.md5;
        long old_size = this.size;
        String old_storageID = this.storageID;
        MCRContentStore old_store = getContentStore();

        initContentFields();

        MCRContentInputStream cis = new MCRContentInputStream(source);
        byte[] header = cis.getHeader();

        contentTypeID = MCRFileContentTypeFactory.detectType(this.getName(), header).getID();

        if (header.length > 0) // Do not store empty file content
        {
            MCRContentStore store = MCRContentStoreFactory.selectStore(this);

            storageID = store.storeContent(this, cis);
            storeID = store.getID();
        }

        size = cis.getLength();
        md5 = cis.getMD5String();

        boolean changed = ((size != old_size) || (!md5.equals(old_md5)));

        if (changed) {
            lastModified = new GregorianCalendar();
        }

        manager.storeNode(this);

        if (changed && hasParent()) {
            getParent().sizeOfChildChanged(old_size, size);
        }

        if (old_storageID.length() != 0) {
            old_store.deleteContent(old_storageID);
        }

        // If file content has changed, call event handlers to index content
        if (changed) {
            String type = (isNew ? MCREvent.CREATE_EVENT : MCREvent.UPDATE_EVENT);
            MCREvent event = new MCREvent(MCREvent.FILE_TYPE, type);
            event.put("file", this);
            MCREventManager.instance().handleEvent(event);
        }

        isNew = false;
    }

    /**
     * Deletes this file and its content stored in the system. Note that after
     * calling this method, the file object is deleted and invalid and can not
     * be used any more.
     */
    public void delete() throws MCRPersistenceException {
        ensureNotDeleted();

        if (storageID.length() != 0) {
            getContentStore().deleteContent(storageID);

            // Call event handlers to update indexed content
            MCREvent event = new MCREvent(MCREvent.FILE_TYPE, MCREvent.DELETE_EVENT);
            event.put("file", this);
            MCREventManager.instance().handleEvent(event);

            if (hasParent()) {
                getParent().sizeOfChildChanged(size, 0);
            }
        }

        super.delete();

        this.contentTypeID = null;
        this.md5 = null;
        this.storageID = null;
        this.storeID = null;
        this.avExtender = null;
    }

    /**
     * Gets an InputStream to read the content of this file from the underlying
     * store. It is important that you close() the stream when you are finished
     * reading content from it.
     * 
     * @return an InputStream to read the file's content from
     * @throws IOException
     */
    public InputStream getContentAsInputStream() throws IOException {
        return getContentStore().retrieveContent(this);
    }

    /**
     * Writes the content of this file to a target output stream.
     * 
     * @param target
     *            the output stream to write the content to
     */
    public void getContentTo(OutputStream target) throws MCRPersistenceException {
        ensureNotDeleted();

        if (storageID.length() != 0) {
            MessageDigest digest = MCRContentInputStream.buildMD5Digest();

            DigestOutputStream dos = new DigestOutputStream(target, digest);
            getContentStore().retrieveContent(this, dos);

            String md5_new = MCRContentInputStream.getMD5String(digest);

            if (!this.md5.equals(md5_new)) {
                String msg = "MD5 Checksum failure while retrieving file content for file " + ID;
                throw new MCRPersistenceException(msg);
            }
        }
    }

    /**
     * Writes the content of this file to a file on the local filesystem
     * 
     * @param target
     *            the local file to write the content to
     */
    public void getContentTo(File target) throws MCRPersistenceException, IOException {
        getContentTo(new FileOutputStream(target));
    }

    /**
     * Gets the content of this file as a byte array
     * 
     * @return the content of this file as a byte array
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
     * Gets the content of this file as a string, using the default encoding of
     * the system environment
     * 
     * @return the file's content as a String
     */
    public String getContentAsString() throws MCRPersistenceException {
        return new String(getContentAsByteArray());
    }

    /**
     * Gets the content of this file as a string, using the given encoding
     * 
     * @param encoding
     *            the character encoding to use
     * @return the file's content as a String
     */
    public String getContentAsString(String encoding) throws MCRPersistenceException, UnsupportedEncodingException {
        return new String(getContentAsByteArray(), encoding);
    }

    public org.jdom.Document getContentAsJDOM() throws MCRPersistenceException, IOException, org.jdom.JDOMException {
        return new org.jdom.input.SAXBuilder().build(getContentAsInputStream());
    }

    /**
     * Returns true, if this file is stored in a content store that provides an
     * MCRAudioVideoExtender for audio/video streaming and additional metadata
     */
    public boolean hasAudioVideoExtender() {
        ensureNotDeleted();

        if (storeID.length() == 0) {
            return false;
        }
        return MCRContentStoreFactory.providesAudioVideoExtender(storeID);
    }

    /**
     * Returns the AudioVideoExtender in case this file is streaming audio/video
     * and stored in a ContentStore that supports this
     */
    public MCRAudioVideoExtender getAudioVideoExtender() {
        ensureNotDeleted();

        if (hasAudioVideoExtender() && (avExtender == null)) {
            avExtender = MCRContentStoreFactory.buildExtender(this);
        }

        return avExtender;
    }

    /**
     * Gets the ID of the content type of this file
     */
    public String getContentTypeID() {
        ensureNotDeleted();

        return contentTypeID;
    }

    /**
     * Gets the content type of this file
     */
    public MCRFileContentType getContentType() {
        ensureNotDeleted();

        return MCRFileContentTypeFactory.getType(contentTypeID);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append("ContentType = ").append(this.contentTypeID).append("\n");
        sb.append("MD5         = ").append(this.md5).append("\n");
        sb.append("StoreID     = ").append(this.storeID).append("\n");
        sb.append("StorageID   = ").append(this.storageID);

        return sb.toString();
    }

    /**
     * Build a XML representation of all technical metadata of this MCRFile and
     * its MCRAudioVideoExtender, if present. That xml can be used for indexing
     * this data.
     */
    public Document createXML() {
        Element root = new Element("file");
        root.setAttribute("id", this.getID());
        root.setAttribute("owner", this.getOwnerID());
        root.setAttribute("name", this.getName());
        root.setAttribute("path", this.getAbsolutePath());
        root.setAttribute("size", Long.toString(this.getSize()));
        root.setAttribute("extension", this.getExtension());
        root.setAttribute("contentTypeID", this.getContentTypeID());
        root.setAttribute("contentType", this.getContentType().getLabel());

        MCRMetaISO8601Date iDate = new MCRMetaISO8601Date();
        iDate.setDate(this.getLastModified().getTime());
        root.setAttribute("modified", iDate.getISOString());

        if (this.hasAudioVideoExtender()) {
            MCRAudioVideoExtender ext = this.getAudioVideoExtender();
            root.setAttribute("bitRate", String.valueOf(ext.getBitRate()));
            root.setAttribute("frameRate", String.valueOf(ext.getFrameRate()));
            root.setAttribute("duration", ext.getDurationTimecode());
            root.setAttribute("mediaType", ((ext.getMediaType() == MCRAudioVideoExtender.AUDIO) ? "audio" : "video"));
        }

        return new Document(root);
    }

}
