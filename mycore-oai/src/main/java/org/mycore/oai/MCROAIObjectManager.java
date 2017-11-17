/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */
package org.mycore.oai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.history.MCRMetadataHistoryManager;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Header.Status;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.OAIConstants;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.pmh.SimpleMetadata;

/**
 * Provides an interface to the MyCoRe object engine.
 * 
 * @author Matthias Eichner
 */
public class MCROAIObjectManager {

    protected static final Logger LOGGER = LogManager.getLogger(MCROAIObjectManager.class);

    protected MCROAIIdentify identify;

    protected String recordUriPattern;

    /**
     * Initialize the object manager. Its important to call this method before you
     * can retrieve records or headers!
     * 
     * @param identify oai repository identifier
     */
    public void init(MCROAIIdentify identify) {
        this.identify = identify;
        String configPrefix = this.identify.getConfigPrefix();
        this.recordUriPattern = MCRConfiguration.instance().getString(configPrefix + "Adapter.RecordURIPattern");
    }

    /**
     * Converts a oai identifier to a mycore id.
     * 
     * @param oaiId the oai identifier
     * @return the mycore identifier
     */
    public String getMyCoReId(String oaiId) {
        return oaiId.substring(oaiId.lastIndexOf(':') + 1);
    }

    /**
     * Converts a mycore id to a oai id.
     * 
     * @param mcrId mycore identifier
     */
    public String getOAIId(String mcrId) {
        return getOAIIDPrefix() + mcrId;
    }

    public Record getRecord(Header header, MetadataFormat format) {
        Element recordElement;
        if (header.isDeleted()) {
            return new Record(header);
        }
        try {
            recordElement = getJDOMRecord(getMyCoReId(header.getId()), format);
        } catch (Exception exc) {
            LOGGER.error("unable to get record {} ({})", header.getId(), format.getPrefix(), exc);
            return null;
        }
        Record record = new Record(header);
        if (recordElement.getNamespace().equals(OAIConstants.NS_OAI)) {
            Element metadataElement = recordElement.getChild("metadata", OAIConstants.NS_OAI);
            if (metadataElement != null && !metadataElement.getChildren().isEmpty()) {
                Element metadataChild = metadataElement.getChildren().get(0);
                record.setMetadata(new SimpleMetadata(metadataChild.detach()));
            }
            Element aboutElement = recordElement.getChild("about", OAIConstants.NS_OAI);
            if (aboutElement != null) {
                for (Element aboutChild : aboutElement.getChildren()) {
                    record.getAboutList().add(aboutChild.detach());
                }
            }
        } else {
            //handle as metadata
            record.setMetadata(new SimpleMetadata(recordElement));
        }
        return record;
    }

    /**
     * Returns a deleted record without metadata by given MyCoRe identifier or null, if the
     * record is not deleted.
     * 
     * @param mcrId id of the deleted record
     * @return deleted record
     */
    public Record getDeletedRecord(String mcrId) {
        try {
            // building the query
            return MCRMetadataHistoryManager.getLastDeletedDate(MCRObjectID.getInstance(mcrId))
                .map(deletedDate -> new Record(
                    new Header(getOAIId(mcrId), deletedDate, Status.deleted)))
                .orElse(null);
        } catch (Exception ex) {
            LOGGER.warn("Error while retrieving deleted record {}", mcrId, ex);
        }
        return null;
    }

    protected Element getJDOMRecord(String mcrId, MetadataFormat format) {
        String uri = formatURI(this.recordUriPattern, mcrId, format.getPrefix());
        return getURI(uri);
    }

    protected Element getURI(String uri) {
        Element e = MCRURIResolver.instance().resolve(uri).detach();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("get {}", uri);
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            LOGGER.debug(out.outputString(e));
        }
        return e;
    }

    protected String formatURI(String uri, String id, String metadataPrefix) {
        MCRObjectID mcrID = MCRObjectID.isValid(id) ? MCRObjectID.getInstance(id) : null;
        boolean exists;
        String objectType;
        if (mcrID != null) {
            exists = MCRMetadataManager.exists(mcrID);
            objectType = mcrID.getTypeId();
        } else {
            MCRFilesystemNode node = MCRFilesystemNode.getNode(id);
            exists = node != null;
            objectType = "data_file";
        }
        return uri.replace("{id}", id).replace("{format}", metadataPrefix).replace("{objectType}", objectType).replace(
            ":{flag}", !exists ? ":deletedMcrObject" : "");
    }

    /**
     * Checks if a mycore object with the given oai identifier exists.
     * 
     * @param oaiId
     *            e.g. oai:www.mycore.de:Document_document_00000003
     * @return true if exists, otherwise false
     */
    protected boolean exists(String oaiId) {
        String mcrId = oaiId.substring(getOAIIDPrefix().length());
        try {
            MCRObjectID mcrObjId = MCRObjectID.getInstance(mcrId);
            return MCRXMLMetadataManager.instance().exists(mcrObjId);
        } catch (Exception ex) {
            return false;
        }
    }

    private String getOAIIDPrefix() {
        return "oai:" + this.identify.getIdentifierDescription().getRepositoryIdentifier() + ":";
    }

}
