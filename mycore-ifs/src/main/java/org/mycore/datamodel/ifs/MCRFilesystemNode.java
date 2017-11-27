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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.niofs.MCRAbstractFileSystem;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.ifs1.MCRFileSystemProvider;

/**
 * Represents a stored file or directory node with its metadata and content.
 *
 * @author Frank Lützenkirchen
 * @author Stefan Freitag
 * @version $Revision$ $Date$
 */
public abstract class MCRFilesystemNode {
    private static final Logger LOGGER = LogManager.getLogger(MCRFilesystemNode.class);

    protected static MCRFileMetadataManager manager = MCRFileMetadataManager.instance();

    public static MCRFilesystemNode getNode(String ID) {
        if (ID == null || ID.trim().length() == 0) {
            throw new MCRUsageException("ID is an empty String or null");
        }

        return manager.retrieveNode(ID);
    }

    public static MCRFilesystemNode getRootNode(String ownerID) {
        if (ownerID == null || ownerID.trim().length() == 0) {
            throw new MCRUsageException("owner ID is an empty String or null");
        }

        return manager.retrieveRootNode(ownerID);
    }

    protected String ID;

    /** The ID of the node owner, e .g. a MILESS derivate ID */
    protected String ownerID;

    /** The ID of the parent directory, if any */
    protected String parentID;

    /** The name of this node */
    protected String name;

    /** The optional label of this node */
    protected String label;

    /** The size in number of bytes */
    protected long size;

    /** The date of last modification of this node */
    private GregorianCalendar lastModified;

    /** A flag indicating if this node is deleted and therefore invalid */
    protected boolean deleted = false;

    protected MCRFilesystemNode(String name, String ownerID) {
        this(name, null, ownerID);
    }

    protected MCRFilesystemNode(String name, MCRDirectory parent) {
        this(name, parent.ID, parent.ownerID);
    }

    protected MCRFilesystemNode(String name, MCRDirectory parent, boolean checkName) {
        this(name, parent.ID, parent.ownerID, checkName);
    }

    private MCRFilesystemNode(String name, String parentID, String ownerID) {
        this(name, parentID, ownerID, true);
    }

    private MCRFilesystemNode(String name, String parentID, String ownerID, boolean doExistCheck) {
        if (ownerID == null || ownerID.trim().length() == 0) {
            throw new MCRUsageException("owner ID is an empty String or null");
        }

        ID = manager.createNodeID();
        this.parentID = parentID;
        this.ownerID = ownerID;
        size = 0;
        lastModified = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
        label = null;
        checkName(name, doExistCheck);
        this.name = name;
    }

    protected MCRFilesystemNode(String ID, String parentID, String ownerID, String name, String label, long size,
        GregorianCalendar date) {
        this.ID = ID;
        this.parentID = parentID;
        this.ownerID = ownerID;
        this.name = name;
        this.label = label;
        this.size = size;
        lastModified = date;
        deleted = false;
    }

    protected void storeNew() {
        manager.storeNode(this);

        if (hasParent()) {
            getParent().addChild(this);
        }
    }

    public void delete() {
        removeAllAdditionalData();
        manager.deleteNode(ID);

        if (parentID != null) {
            getParent().removeChild(this);
        }

        ID = null;
        ownerID = null;
        name = null;
        label = null;
        size = 0;
        lastModified = null;
        parentID = null;
        deleted = true;
    }

    public void move(MCRDirectory dest) {
        this.ownerID = dest.getOwnerID();
        getParent().removeChild(this);
        this.parentID = dest.getID();
        manager.storeNode(this);
        manager.clearMetadataCache();
    }

    /**
     * Changed method because of problems with update of files.
     *
     */
    protected void checkName(String name, boolean doExistCheck) {
        name = Normalizer.normalize(name, Normalizer.Form.NFC);

        if (name == null) {
            throw new MCRUsageException(name + " is null.");
        }

        boolean error = name.indexOf("/") + name.indexOf("\\") != -2;
        String errorMsg = "Filesystem node name must not contain '\' or '/' characters: " + name;
        if (error) {
            throw new MCRUsageException(errorMsg);
        }
        if (hasParent() && doExistCheck) {
            boolean exists = getParent().hasChild(name);
            if (exists) {
                throw new MCRUsageException(getParent().getAbsolutePath() + " -> " + name + " exists already.");
            }
        }
    }

    public String getID() {
        return ID;
    }

    /**
     * Returns the ID of the owner (the derivate id) of this node.
     *
     * @return the ID of the owner of this node (derivate id)
     */
    public String getOwnerID() {
        ensureNotDeleted();

        return ownerID;
    }

    public String getParentID() {
        return parentID;
    }

    public MCRDirectory getParent() {
        ensureNotDeleted();

        if (!hasParent()) {
            return null;
        }
        return MCRDirectory.getDirectory(parentID);
    }

    public boolean hasParent() {
        ensureNotDeleted();

        return parentID != null;
    }

    public MCRDirectory getRootDirectory() {
        ensureNotDeleted();

        if (hasParent()) {
            return getParent().getRootDirectory();
        } else if (this instanceof MCRDirectory) {
            return (MCRDirectory) this;
        } else {
            return null;
        }
    }

    protected void ensureNotDeleted() {
        if (deleted) {
            throw new MCRUsageException("Do not use this node, it is deleted");
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Sets the name of this node
     */
    public void setName(String name) {
        ensureNotDeleted();

        if (this.name != null && this.name.equals(name)) {
            return;
        }

        checkName(name, true);
        this.name = name;

        touch(true);
        fireUpdateEvent();
    }

    protected void touch(boolean recursive) {
        touch(null, recursive);
    }

    protected void touch(FileTime time, boolean recursive) {
        lastModified = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
        if (time != null) {
            lastModified.setTimeInMillis(time.toMillis());
        }
        manager.storeNode(this);

        if (recursive && parentID != null) {
            getParent().touch(time, true);
        }
    }

    protected void fireUpdateEvent() {
        MCREvent event = new MCREvent(MCREvent.PATH_TYPE, MCREvent.UPDATE_EVENT);
        event.put(MCRFileEventHandlerBase.FILE_TYPE, this); //to support old events
        event.put(MCREvent.PATH_KEY, toPath());
        try {
            event.put(MCREvent.FILEATTR_KEY, getBasicFileAttributes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MCREventManager.instance().handleEvent(event);
    }

    /**
     * Returns the name of this node
     *
     * @return the name of this node
     */
    public String getName() {
        ensureNotDeleted();

        return name;
    }

    /**
     * Sets the label of this node
     *
     * @param label
     *            the label (may be null)
     */
    public void setLabel(String label) {
        ensureNotDeleted();

        if (label != null && label.trim().length() == 0) {
            label = null;
        }

        if (label != null && label.equals(this.label)) {
            return;
        }

        this.label = label;
        touch(true);
        fireUpdateEvent();
    }

    /**
     * Returns the label of this node
     *
     * @return the label of this node, or null
     */
    public String getLabel() {
        ensureNotDeleted();

        return label;
    }

    /**
     * Returns an absolute path that is build like this: {@link #getOwnerID()}+{@link #getAbsolutePath()}.
     *
     * If this node is a file that has no parent directory, this method returns the same as {@link #getName()}.
     */
    public String getPath() {
        ensureNotDeleted();

        if (hasParent()) {
            return getParent().getPath() + "/" + name;
        }
        return name;
    }

    /**
     * Returns the absolute path of this node starting with a '/'.
     */
    public String getAbsolutePath() {
        ensureNotDeleted();

        if (hasParent()) {
            String path = getParent().getAbsolutePath();

            if (!path.endsWith("/")) {
                path += "/";
            }

            return path + name;
        }
        return "/";
    }

    public MCRPath toPath() {
        return MCRAbstractFileSystem.getPath(ownerID, getAbsolutePath(), MCRFileSystemProvider.getMCRIFSFileSystem());
    }

    public abstract MCRFileAttributes<String> getBasicFileAttributes() throws IOException;

    /**
     * Returns the node size as number of bytes
     */
    public long getSize() {
        ensureNotDeleted();

        return size;
    }

    /**
     * Returns the node size, formatted as a string
     */
    public String getSizeFormatted() {
        ensureNotDeleted();

        return getSizeFormatted(size);
    }

    /**
     * Takes a file size in bytes and formats it as a string for output. For
     * values &lt; 5 KB the output format is for example "320 Byte". For values
     * &gt; 5 KB the output format is for example "6,8 KB". For values &gt; 1 MB
     * the output format is for example "3,45 MB".
     */
    public static String getSizeFormatted(long bytes) {
        String sizeUnit;
        String sizeText;
        double sizeValue;

        if (bytes >= 1024 * 1024) // >= 1 MB
        {
            sizeUnit = "MB";
            sizeValue = (double) Math.round(bytes / 10485.76) / 100;
        } else if (bytes >= 5 * 1024) // >= 5 KB
        {
            sizeUnit = "KB";
            sizeValue = (double) Math.round(bytes / 102.4) / 10;
        } else // < 5 KB
        {
            sizeUnit = "Byte";
            sizeValue = bytes;
        }

        sizeText = String.valueOf(sizeValue).replace('.', ',');

        if (sizeText.endsWith(",0")) {
            sizeText = sizeText.substring(0, sizeText.length() - 2);
        }

        return sizeText + " " + sizeUnit;
    }

    /**
     * Returns the time of last modification of this node
     */
    public GregorianCalendar getLastModified() {
        ensureNotDeleted();

        return lastModified;
    }

    /**
     * Stores additional XML data for this node. The name of the data element is
     * used as unique key for storing data. If data with this name already
     * exists, it is overwritten.
     *
     * @param data
     *            the additional XML data to be saved
     * @throws IOException
     *             if the XML data can not be retrieved
     * @throws JDOMException
     *             if the XML data can not be parsed
     */
    public void setAdditionalData(Element data) throws IOException, JDOMException {
        MCRFile dataFile = MCRFile.getRootFile(ID);
        Document doc;
        if (dataFile == null) {
            String name = "MCRFilesystemNode.additionalData";
            dataFile = new MCRFile(name, ID);
            doc = new Document(new Element("additionalData"));
        } else {
            doc = dataFile.getContentAsJDOM();
        }

        Element child = doc.getRootElement().getChild(data.getName());
        if (child != null) {
            child.detach();
        }
        doc.getRootElement().addContent(data.clone());
        dataFile.setContentFrom(doc);
    }

    /**
     * Removes additional XML data from this node.
     *
     * @param dataName
     *            the name of the additional XML data element to be removed
     * @throws IOException
     *             if the XML data can not be retrieved
     * @throws JDOMException
     *             if the XML data can not be parsed
     */
    public void removeAdditionalData(String dataName) throws IOException, JDOMException {
        MCRFile dataFile = MCRFile.getRootFile(ID);
        if (dataFile == null) {
            return;
        }
        Document doc = dataFile.getContentAsJDOM();
        Element child = doc.getRootElement().getChild(dataName);
        if (child != null) {
            child.detach();
        }
        if (doc.getRootElement().getChildren().size() == 0) {
            dataFile.delete();
        } else {
            dataFile.setContentFrom(doc);
        }
    }

    /**
     * Removes all additional XML data stored for this node, if any.
     */
    public void removeAllAdditionalData() {
        MCRFile dataFile = MCRFile.getRootFile(ID);
        if (dataFile != null) {
            dataFile.delete();
        }
    }

    /**
     * Gets additional XML data stored for this node, if any.
     *
     * @param dataName
     *            the name of the additional XML data element to be retrieved
     * @return the additional XML data elemet that was stored, or null
     * @throws IOException
     *             if the XML data can not be retrieved
     * @throws JDOMException
     *             if the XML data can not be parsed
     */
    public Element getAdditionalData(String dataName) throws IOException, JDOMException {
        MCRFile dataFile = MCRFile.getRootFile(ID);
        if (dataFile == null || dataFile.getSize() == 0) {
            return null;
        }
        Document doc = dataFile.getContentAsJDOM();
        return doc.getRootElement().getChild(dataName);
    }

    /**
     * Gets all additional XML data stored for this node, if any.
     *
     * @return the additional XML data document that was stored, or null
     * @throws IOException
     *             if the XML data can not be retrieved
     */
    public MCRContent getAllAdditionalData() throws IOException {
        MCRFile dataFile = MCRFile.getRootFile(ID);
        if (dataFile == null || dataFile.getSize() == 0) {
            return null;
        } else {
            return dataFile.getContent();
        }
    }

    protected static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS", Locale.ROOT);

    @Override
    public String toString() {
        String date = formatter.format(lastModified.getTime());

        return "ID          = " + ID + "\n" + "Name        = " + name + "\n" + "Label       = " + label + "\n"
            + "Type        = " + this.getClass().getName() + "\n" + "ParentID    = " + parentID + "\n"
            + "OwnerID     = " + ownerID + "\n" + "Size        = " + size + "\n" + "Modified    = " + date + "\n";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRFilesystemNode)) {
            return false;
        }
        MCRFilesystemNode other = (MCRFilesystemNode) obj;
        if (other.ID == null) {
            return super.equals(obj);
        } else {
            return other.ID.equals(ID);
        }
    }

    @Override
    public int hashCode() {
        if (ID != null) {
            return ID.hashCode();
        } else {
            return super.hashCode();
        }
    }

    /**
     * Returns a list of {@link MCRFile}s in the hierarchy (both up and down) of this node.
     *
     * @return list of {@link MCRFile}
     */
    public MCRFile[] getFiles() {
        MCRFilesystemNode rootNode = MCRFilesystemNode.getRootNode(this.getOwnerID());
        List<MCRFile> fList = new ArrayList<>();
        if (rootNode instanceof MCRDirectory) {
            processNode(rootNode, fList);
        }
        return fList.toArray(new MCRFile[0]);
    }

    private void processNode(MCRFilesystemNode node, List<MCRFile> fList) {
        MCRDirectory dir = (MCRDirectory) node;
        MCRFilesystemNode[] children = dir.getChildren();
        for (MCRFilesystemNode child : children) {
            if (child instanceof MCRDirectory) {
                processNode(child, fList);
            }
            if (child instanceof MCRFile) {
                fList.add((MCRFile) child);
            }
        }
    }
}
