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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

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

    /**
     * Creates a new stored metadata object
     * 
     * @param store
     *            the store this document is stored in
     * @param fo
     *            the file object storing the data
     * @param id
     *            the ID of the metadata document
     */
    MCRStoredMetadata(MCRMetadataStore store, FileObject fo, int id) {
        this.store = store;
        this.id = id;
        this.fo = fo;
    }

    /**
     * Creates a new local file to save XML to
     * 
     * @param xml
     *            the XML to save to a new file
     */
    void create(Document xml) throws Exception {
        fo.createFile();
        save(xml);
    }

    /**
     * Saves the given XML to the internal file object
     * 
     * @param xml
     *            the XML to save
     */
    void save(Document xml) throws Exception {
        OutputStream out = fo.getContent().getOutputStream();
        out.write(xml2bytes(xml));
        out.close();
    }

    /**
     * Outputs the xml document's content to a byte array
     * 
     * @param xml
     *            the xml document
     * @return the xml document written in UTF-8 with pretty formatting
     */
    protected byte[] xml2bytes(Document xml) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getPrettyFormat().setEncoding("UTF-8").setIndent("  "));
        xout.output(xml, out);
        out.close();
        return out.toByteArray();
    }

    /**
     * Updates the stored XML document
     * 
     * @param xml
     *            the XML document to be stored
     */
    public void update(Document xml) throws Exception {
        save(xml);
    }

    /**
     * Returns the stored XML document
     * 
     * @return the stored XML document
     */
    public Document getXML() throws Exception {
        InputStream in = fo.getContent().getInputStream();
        Document xml = new SAXBuilder().build(in);
        in.close();
        return xml;
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
    public Date getLastModified() throws Exception {
        long time = fo.getContent().getLastModifiedTime();
        return new Date(time);
    }

    /**
     * Sets the date this metadata document was last modified
     * 
     * @param date
     *            the date this metadata document was last modified
     */
    public void setLastModified(Date date) throws Exception {
        fo.getContent().setLastModifiedTime(date.getTime());
    }

    /**
     * Deletes the metadata document. This object is invalid afterwards, do not
     * use it any more.
     * 
     * @throws Exception
     */
    public void delete() throws Exception {
        store.delete(id);
    }
}
