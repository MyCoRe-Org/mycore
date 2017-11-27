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
import java.util.Date;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRVFSContent;
import org.xml.sax.SAXException;

/**
 * Represents an XML metadata document that is stored in MCRMetadataStore.
 *
 * @author Frank Lützenkirchen
 */
public class MCRStoredMetadata {

    /** The ID of the metadata document */
    protected int id;

    /** The file object in local filesystem storing the data */
    protected FileObject fo;

    /** The store this document is stored in */
    protected MCRMetadataStore store;

    private String docType;

    protected boolean deleted;

    /**
     * Creates a new stored metadata object
     *
     * @param store
     *            the store this document is stored in
     * @param fo
     *            the file object storing the data
     * @param id
     *            the ID of the metadata document
     * @param docType
     *            if not null overwrites any detected doctype
     */
    MCRStoredMetadata(MCRMetadataStore store, FileObject fo, int id, String docType) {
        this.store = store;
        this.id = id;
        this.fo = fo;
        this.docType = docType;
        this.deleted = false;
    }

    /**
     * Creates a new local file to save XML to
     *
     * @param xml
     *            the XML to save to a new file
     * @throws JDOMException if content is not XML and corresponding {@link MCRMetadataStore} forces MCRContent to be XML
     */
    void create(MCRContent xml) throws IOException, JDOMException {
        if (store.shouldForceXML()) {
            try {
                xml = xml.ensureXML();
            } catch (SAXException e) {
                throw new IOException(e);
            }
        }
        fo.createFile();
        xml.sendTo(fo);
    }

    /**
     * Updates the stored XML document
     *
     * @param xml
     *            the XML document to be stored
     * @throws JDOMException if content is not XML and corresponding {@link MCRMetadataStore} forces MCRContent to be XML
     */
    public void update(MCRContent xml) throws IOException, JDOMException {
        if (isDeleted()) {
            String msg = "You can not update a deleted data object";
            throw new MCRUsageException(msg);
        }
        if (store.shouldForceXML()) {
            try {
                xml = xml.ensureXML();
            } catch (SAXException e) {
                throw new IOException(e);
            }
        }
        xml.sendTo(fo);
    }

    /**
     * Returns the stored XML document
     *
     * @return the stored XML document
     */
    public MCRContent getMetadata() throws IOException {
        return new MCRVFSContent(fo, docType);
    }

    /**
     * Returns the ID of this metadata document
     *
     * @return the ID of this metadata document
     */
    public int getID() {
        return id;
    }

    /**
     * Returns the store this metadata document is stored in
     *
     * @return the store this metadata document is stored in
     */
    public MCRMetadataStore getStore() {
        return store;
    }

    /**
     * Returns the date this metadata document was last modified
     *
     * @return the date this metadata document was last modified
     */
    public Date getLastModified() throws IOException {
        try (FileContent fileContent = fo.getContent()) {
            long time = fileContent.getLastModifiedTime();
            return new Date(time);
        }
    }

    /**
     * Sets the date this metadata document was last modified
     *
     * @param date
     *            the date this metadata document was last modified
     */
    public void setLastModified(Date date) throws IOException {
        if (!isDeleted()) {
            try (FileContent fileContent = fo.getContent()) {
                fo.getContent().setLastModifiedTime(date.getTime());
            }
        }
    }

    /**
     * Deletes the metadata document. This object is invalid afterwards, do not
     * use it any more.
     *
     */
    public void delete() throws IOException {
        if (!deleted) {
            store.delete(fo);
            deleted = true;
        }
    }

    /**
     * Returns true if this object is deleted
     */
    public boolean isDeleted() throws IOException {
        return deleted;
    }
}
