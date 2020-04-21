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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;

/**
 * Stores XML metadata documents (or optionally any other BLOB data) in a
 * persistent filesystem structure
 * 
 * For each object type, a store must be defined as follows:
 * 
 * MCR.IFS2.Store.DocPortal_document.Class=org.mycore.datamodel.ifs2.MCRMetadataStore 
 * MCR.IFS2.Store.DocPortal_document.BaseDir=/foo/bar
 * MCR.IFS2.Store.DocPortal_document.SlotLayout=4-2-2 
 * MCR.IFS2.Store.DocPortal_document.ForceXML=true (which is default)
 * 
 * @author Frank Lützenkirchen
 */
public class MCRMetadataStore extends MCRStore {

    public static final Logger LOGGER = LogManager.getLogger();

    /**
     * If true (which is default), store will enforce it gets
     * XML to store, otherwise any binary content can be stored here.
     * 
     * Override with MCR.IFS2.Store.&lt;ObjectType&gt;.ForceXML=true|false
     */
    protected boolean forceXML = true;

    protected String forceDocType;

    /**
     * Initializes a new metadata store instance.
     * 
     * @param type
     *            the document type that is stored in this store
     */
    @Override
    protected void init(String type) {
        super.init(type);
        prefix = MCRConfiguration2.getString("MCR.IFS2.Store." + type + ".Prefix").orElse(type + "_");
        suffix = ".xml";
        forceXML = MCRConfiguration2.getBoolean("MCR.IFS2.Store." + type + ".ForceXML").orElse(true);
        if (forceXML) {
            forceDocType = MCRConfiguration2.getString("MCR.IFS2.Store." + type + ".ForceDocType").orElse(null);
            LOGGER.debug("Set doctype for {} to {}", type, forceDocType);
        }
    }

    /**
     * Initializes a new metadata store instance.
     * 
     * @param config
     *            the configuration for the store
     */
    @Override
    protected void init(MCRStoreConfig config) {
        super.init(config);
        prefix = Optional.ofNullable(config.getPrefix()).orElseGet(() -> config.getID() + "_");
        suffix = ".xml";
        forceXML = MCRConfiguration2.getBoolean("MCR.IFS2.Store." + config.getID() + ".ForceXML").orElse(true);
        if (forceXML) {
            forceDocType = MCRConfiguration2.getString("MCR.IFS2.Store." + config.getID() + ".ForceDocType")
                .orElse(null);
            LOGGER.debug("Set doctype for {} to {}", config.getID(), forceDocType);
        }
    }

    protected boolean shouldForceXML() {
        return forceXML;
    }

    /**
     * Stores a newly created document, using the next free ID.
     * 
     * @param xml
     *            the XML document to be stored
     * @return the stored metadata object
     */
    public MCRStoredMetadata create(MCRContent xml) throws IOException, JDOMException {
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
    public MCRStoredMetadata create(MCRContent xml, int id) throws IOException, JDOMException {
        if (id <= 0) {
            throw new MCRException("ID of metadata object must be a positive integer");
        }
        Path path = getSlot(id);
        if (Files.exists(path)) {
            String msg = "Metadata object with ID " + id + " already exists in store";
            throw new MCRException(msg);
        }
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        MCRStoredMetadata meta = buildMetadataObject(path, id);
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
    public MCRStoredMetadata retrieve(int id) throws IOException {
        Path path = getSlot(id);
        if (!Files.exists(path)) {
            return null;
        } else {
            return buildMetadataObject(path, id);
        }
    }

    /**
     * Builds a new stored metadata object in this store
     * 
     * @param fo
     *            the FileObject that stores the data
     * @param id
     *            the ID of the metadata object
     */
    protected MCRStoredMetadata buildMetadataObject(Path fo, int id) {
        return new MCRStoredMetadata(this, fo, id, forceDocType);
    }
}
