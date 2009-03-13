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
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;

/**
 * Stores XML metadata documents in a persistent filesystem structure
 * 
 * For each metadata type, a store must be defined as follows:
 * 
 * MCR.IFS2.MetadataStore.DocPortal_document.BaseDir=c:\\store
 * MCR.IFS2.MetadataStore.DocPortal_document.SlotLayout=4-2-2
 * 
 * For a versioning store subclass, define the URL of a local SVN repository. If
 * it does not exist yet, it will be created automatically.
 * 
 * MCR.IFS2.MetadataStore.DocPortal_document.SVNRepositoryURL=
 * file:///c:/storesvn
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStore extends MCRStore {
    /**
     * Map of defined metadata stores. Key is the document type, value is the
     * store storing documents of that type.
     */
    private static HashMap<String, MCRMetadataStore> stores = new HashMap<String, MCRMetadataStore>();

    /**
     * Returns the store storing metadata of the given type
     * 
     * @param type
     *            the document type
     * @return the store defined for the given metadata type
     * @throws MCRConfigurationException
     *             when no store for that type is configured
     */
    public static MCRMetadataStore getStore(String type) {
        if (!stores.containsKey(type)) {
            String prefix = "MCR.IFS2.MetadataStore." + type + ".";
            MCRConfiguration config = MCRConfiguration.instance();

            String baseDir = config.getString(prefix + "BaseDir");
            String slotLayout = config.getString(prefix + "SlotLayout");
            String repositoryURL = config.getString(prefix + "SVNRepositoryURL", null);

            if (repositoryURL == null)
                new MCRMetadataStore(type, baseDir, slotLayout);
            else
                new MCRVersioningMetadataStore(type, baseDir, slotLayout, repositoryURL);
        }
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
        MCRStoredMetadata meta = buildMetadataObject(fo, id);
        meta.create(xml);
        return meta;
    }

    /**
     * Returns the metadata stored under the given ID, or null
     * 
     * @param id
     *            the ID of the XML document
     * @return the metadata stored under that ID, or null when there is no such
     *         metadata object
     */
    public MCRStoredMetadata retrieve(int id) throws Exception {
        FileObject fo = getSlot(id);
        if (!fo.exists())
            return null;
        else
            return buildMetadataObject(fo, id);
    }

    /**
     * Builds a new stored metadata object in this store
     * 
     * @param fo
     *            the FileObject that stores the data
     * @param id
     *            the ID of the metadata object
     */
    protected MCRStoredMetadata buildMetadataObject(FileObject fo, int id) {
        return new MCRStoredMetadata(this, fo, id);
    }
}
