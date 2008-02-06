/*
 * 
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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.mycore.common.MCRArgumentChecker;

/**
 * Represents a stored file or directory node with its metadata and content.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public abstract class MCRFilesystemNode {
    protected static MCRFileMetadataManager manager = MCRFileMetadataManager.instance();

    public static MCRFilesystemNode getNode(String ID) {
        MCRArgumentChecker.ensureNotEmpty(ID, "ID");

        return manager.retrieveNode(ID);
    }

    public static MCRFilesystemNode getRootNode(String ownerID) {
        MCRArgumentChecker.ensureNotEmpty(ownerID, "ownerID");

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
    protected GregorianCalendar lastModified;

    /** A flag indicating if this node is deleted and therefore invalid */
    protected boolean deleted = false;

    protected MCRFilesystemNode(String name, String ownerID) {
        this(name, null, ownerID);
    }

    protected MCRFilesystemNode(String name, MCRDirectory parent) {
        this(name, parent.ID, parent.ownerID);
    }

    private MCRFilesystemNode(String name, String parentID, String ownerID) {
        MCRArgumentChecker.ensureNotEmpty(ownerID, "owner ID");

        this.ID = manager.createNodeID();
        this.parentID = parentID;
        this.ownerID = ownerID;
        this.size = 0;
        this.lastModified = new GregorianCalendar();
        this.label = null;

        checkName(name);
        this.name = name;
    }

    protected MCRFilesystemNode(String ID, String parentID, String ownerID, String name, String label, long size, GregorianCalendar date) {
        this.ID = ID;
        this.parentID = parentID;
        this.ownerID = ownerID;
        this.name = name;
        this.label = label;
        this.size = size;
        this.lastModified = date;
        this.deleted = false;
    }

    protected void storeNew() {
        manager.storeNode(this);

        if (hasParent()) {
            getParent().addChild(this);
        }
    }

    public void delete() {
        this.removeAllAdditionalData();
        manager.deleteNode(ID);

        if (parentID != null) {
            getParent().removeChild(this);
        }

        this.ID = null;
        this.ownerID = null;
        this.name = null;
        this.label = null;
        this.size = 0;
        this.lastModified = null;
        this.parentID = null;
        this.deleted = true;
    }

    protected void checkName(String name) {
        MCRArgumentChecker.ensureNotEmpty(name, "name");

        boolean error = (name.indexOf("/") + name.indexOf("\\")) != -2;
        String errorMsg = "Filesystem node name must not contain '\' or '/' characters: " + name;
        MCRArgumentChecker.ensureIsFalse(error, errorMsg);

        if (hasParent()) {
            boolean exists = getParent().hasChild(name);
            String existsMsg = "A node with this name already exists: " + name;
            MCRArgumentChecker.ensureIsFalse(exists, existsMsg);
        }
    }

    public String getID() {
        return ID;
    }

    /**
     * Returns the ID of the owner of this node
     * 
     * @return the ID of the owner of this node
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

        return (parentID != null);
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
        MCRArgumentChecker.ensureIsFalse(deleted, "Do not use this node, it is deleted");
    }

    /**
     * Sets the name of this node
     */
    public void setName(String name) {
        ensureNotDeleted();

        if (this.name.equals(name)) {
            return;
        }

        checkName(name);
        this.name = name;
        this.lastModified = new GregorianCalendar();

        manager.storeNode(this);

        if (parentID != null) {
            getParent().touch();
        }
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

        if ((label != null) && (label.trim().length() == 0)) {
            label = null;
        }

        if (this.label.equals(label)) {
            return;
        }

        this.label = label;
        this.lastModified = new GregorianCalendar();

        manager.storeNode(this);

        if (parentID != null) {
            getParent().touch();
        }
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

    public String getPath() {
        ensureNotDeleted();

        if (hasParent()) {
            return getParent().getPath() + "/" + name;
        }
        return name;
    }

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

        if (bytes >= (1024 * 1024)) // >= 1 MB
        {
            sizeUnit = "MB";
            sizeValue = (double) (Math.round(bytes / 10485.76)) / 100;
        } else if (bytes >= (5 * 1024)) // >= 5 KB
        {
            sizeUnit = "KB";
            sizeValue = (double) (Math.round(bytes / 102.4)) / 10;
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
        MCRFile dataFile = MCRFile.getRootFile(this.ID);
        Document doc;
        if (dataFile == null) {
            String name = "MCRFilesystemNode.additionalData";
            dataFile = new MCRFile(name, this.ID);
            doc = new Document(new Element("additionalData"));
        } else
            doc = dataFile.getContentAsJDOM();

        Element child = doc.getRootElement().getChild(data.getName());
        if (child != null)
            child.detach();
        doc.getRootElement().addContent((Element)(data.clone()));
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
        MCRFile dataFile = MCRFile.getRootFile(this.ID);
        if (dataFile == null)
            return;
        Document doc = dataFile.getContentAsJDOM();
        Element child = doc.getRootElement().getChild(dataName);
        if (child != null)
            child.detach();
        if (doc.getRootElement().getChildren().size() == 0)
            dataFile.delete();
        else
            dataFile.setContentFrom(doc);
    }

    /**
     * Removes all additional XML data stored for this node, if any.
     */
    public void removeAllAdditionalData() {
        MCRFile dataFile = MCRFile.getRootFile(this.ID);
        if (dataFile != null)
            dataFile.delete();
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
        MCRFile dataFile = MCRFile.getRootFile(this.ID);
        if ((dataFile == null) || (dataFile.getSize() == 0))
            return null;
        Document doc = dataFile.getContentAsJDOM();
        return doc.getRootElement().getChild(dataName);
    }

    /**
     * Gets all additional XML data stored for this node, if any.
     * 
     * @return the additional XML data document that was stored, or null
     * @throws IOException
     *             if the XML data can not be retrieved
     * @throws JDOMException
     *             if the XML data can not be parsed
     */
    public Document getAllAdditionalData() throws IOException, JDOMException {
        MCRFile dataFile = MCRFile.getRootFile(this.ID);
        if ((dataFile == null) || (dataFile.getSize() == 0))
            return null;
        else
            return dataFile.getContentAsJDOM();
    }

    protected static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS");

    public String toString() {
        String date = formatter.format(lastModified.getTime());

        StringBuffer sb = new StringBuffer();
        sb.append("ID          = ").append(this.ID).append("\n");
        sb.append("Name        = ").append(this.name).append("\n");
        sb.append("Label       = ").append(this.label).append("\n");
        sb.append("Type        = ").append(this.getClass().getName()).append("\n");
        sb.append("ParentID    = ").append(this.parentID).append("\n");
        sb.append("OwnerID     = ").append(this.ownerID).append("\n");
        sb.append("Size        = ").append(this.size).append("\n");
        sb.append("Modified    = ").append(date).append("\n");

        return sb.toString();
    }
}
