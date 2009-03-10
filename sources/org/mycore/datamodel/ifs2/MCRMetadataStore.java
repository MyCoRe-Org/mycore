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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Stores XML metadata documents in a persistent filesystem structure
 * 
 * For each metadata type, a store must be defined as follows:
 * 
 * MCR.IFS2.MetadataStore.DocPortal_document.BaseDir=c:\\store
 * MCR.IFS2.MetadataStore.DocPortal_document.SlotLayout=4-2-2
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStore extends MCRStore {
    /**
     * Map of defined metadata stores. Key is the document type, value is the
     * store storing documents of that type.
     */
    private static HashMap<String, MCRMetadataStore> stores;

    /**
     * Reads configuration and initializes defined stores
     */
    static {
        stores = new HashMap<String, MCRMetadataStore>();

        // MCR.IFS2.MetadataStore.DocPortal_document.BaseDir=c:\\store
        // MCR.IFS2.MetadataStore.DocPortal_document.SlotLayout=4-2-2

        String prefix = "MCR.IFS2.MetadataStore.";
        MCRConfiguration config = MCRConfiguration.instance();
        Properties prop = config.getProperties(prefix);
        for (Enumeration keys = prop.keys(); keys.hasMoreElements();) {
            String key = (String) (keys.nextElement());
            if (!key.endsWith("BaseDir"))
                continue;
            String baseDir = prop.getProperty(key);
            String type = key.substring(prefix.length(), key.indexOf(".BaseDir"));
            String slotLayout = config.getString(prefix + type + ".SlotLayout");
            new MCRMetadataStore(type, baseDir, slotLayout);
        }
    }

    /**
     * Returns the store storing metadata of the given´type
     * 
     * @param type
     *            the document type
     * @return the store defined for the given metadata type
     */
    public static MCRMetadataStore getStore(String type) {
        return stores.get(type);
    }

    /**
     * Creates a new metadata store instance.
     * 
     * @param type
     *            the document type that is stored in this store
     * @param baseDir
     *            the base directory in the local filesystem storing the data
     * @param slotLayout
     *            the layout of slot subdirectories
     */
    protected MCRMetadataStore(String type, String baseDir, String slotLayout) {
        super(type, baseDir, slotLayout, type + "_", ".xml");
        stores.put(type, this);
    }

    /**
     * Stores a newly created document, using the next free ID.
     * 
     * @param xml
     *            the XML document to be stored
     * @return the stored metadata object
     */
    public MCRStoredMetadata create(Document xml) throws Exception {
        int id = getNextFreeID();
        return create(xml, id);
    }

    /**
     * Stores a newly created document under the given ID.
     * 
     * @param xml
     *            the XML document to be stored
     * @param id
     *            the ID under which the document should be stored
     * @return the stored metadata object
     */
    public MCRStoredMetadata create(Document xml, int id) throws Exception {
        FileObject fo = getSlot(id);
        if (fo.exists()) {
            String msg = "Metadata object with ID " + id + " already exists in store";
            throw new MCRException(msg);
        }
        fo.createFile();
        MCRStoredMetadata meta = new MCRStoredMetadata(this,fo,id);
        fo.createFile();
        meta.save(xml);
        return meta;
    }

    /**
     * Returns the metadata stored under the given ID, or null
     * 
     * @param id
     *            the ID of the XML document
     * @return the metadata stored under that ID, or null when there is no
     *         such metadata object
     */
    public MCRStoredMetadata retrieve(int id) throws Exception {
        FileObject fo = getSlot(id);
        if (!fo.exists())
            return null;
        else return new MCRStoredMetadata( this, fo, id );
    }
}
