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

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRVFSContent;
import org.xml.sax.SAXException;

/**
 * Represents a set of files and directories belonging together, that are stored
 * in a persistent MCRFileStore. A FileCollection has a unique ID within the
 * store, it is the root folder of all files and directories in the collection.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileCollection extends MCRDirectory {

    /**
     * The logger
     */
    private final static Logger LOGGER = LogManager.getLogger(MCRFileCollection.class);

    /**
     * The store this file collection is stored in.
     */
    private MCRStore store;

    /**
     * The ID of this file collection
     */
    private int id;

    /**
     * Creates a new file collection in the given store, or retrieves an
     * existing one.
     * 
     * @see MCRFileStore
     * 
     * @param store
     *            the store this file collection is stored in
     * @param id
     *            the ID of this file collection
     */
    protected MCRFileCollection(MCRStore store, int id) throws IOException {
        super(null, store.getSlot(id), new Element("collection"));
        this.store = store;
        this.id = id;
        if (fo.exists()) {
            readAdditionalData();
        } else {
            fo.createFolder();
            new Document(data);
            saveAdditionalData();
        }
    }

    private final static String dataFile = "mcrdata.xml";

    private void readAdditionalData() throws IOException {
        FileObject src = VFS.getManager().resolveFile(fo, dataFile);
        if (!src.exists()) {
            LOGGER.warn("Metadata file is missing, repairing metadata...");
            data = new Element("collection");
            new Document(data);
            repairMetadata();
        }
        try {
            data = new MCRVFSContent(src).asXML().getRootElement();
        } catch (JDOMException | SAXException e) {
            throw new IOException(e);
        }
    }

    protected void saveAdditionalData() throws IOException {
        FileObject target = VFS.getManager().resolveFile(fo, dataFile);
        new MCRJDOMContent(data.getDocument()).sendTo(target);
    }

    /**
     * Deletes this file collection with all its data and children
     */
    public void delete() throws IOException {
        data.removeContent();
        fo.delete(Selectors.SELECT_ALL);
    }

    /**
     * Throws a exception, because a file collection's name is always the empty
     * string and therefore can not be renamed.
     */
    @Override
    public void renameTo(String name) {
        throw new UnsupportedOperationException("File collections can not be renamed");
    }

    /**
     * Returns the store this file collection is stored in.
     * 
     * @return the store this file collection is stored in.
     */
    public MCRStore getStore() {
        return store;
    }

    /**
     * Returns the ID of this file collection
     * 
     * @return the ID of this file collection
     */
    public int getID() {
        return id;
    }

    /**
     * Returns this object, because the FileCollection instance is the root of
     * all files and directories contained in the collection.
     * 
     * @return this
     */
    @Override
    public MCRFileCollection getRoot() {
        return this;
    }

    @Override
    public int getNumChildren() throws IOException {
        return super.getNumChildren() - 1;
    }

    @Override
    public MCRNode getChild(String name) throws IOException {
        if (dataFile.equals(name)) {
            return null;
        } else {
            return super.getChild(name);
        }
    }

    @Override
    public String getName() {
        return "";
    }

    /**
     * Repairs additional metadata stored for all files and directories in this
     * collection
     */
    @Override
    public void repairMetadata() throws IOException {
        super.repairMetadata();
        data.setName("collection");
        data.removeAttribute("name");
        saveAdditionalData();
    }

    /**
     * Returns additional metadata stored for all files and directories in this
     * collection
     */
    Document getMetadata() {
        return data.getDocument();
    }
}
