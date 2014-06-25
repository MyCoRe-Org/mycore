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

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRDELETEDITEMS;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Header.Status;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.OAIConstants;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.pmh.Set;
import org.mycore.oai.pmh.SimpleMetadata;

/**
 * Provides an interface to the MyCoRe object engine.
 * 
 * @author Matthias Eichner
 */
public class MCROAIObjectManager {

    protected final static Logger LOGGER = Logger.getLogger(MCROAIObjectManager.class);

    protected String configPrefix;

    protected String repositoryIdentifier;

    protected String recordUriPattern;

    protected String headerUriPattern;

    /**
     * Initialize the object mananger. Its important to call this method before you
     * can retrieve records or headers!
     * 
     * @param configPrefix
     * @param repositoryIdentifier identifier of the repository, use the {@link MCROAIIdentify} to retrieve it
     */
    public void init(String configPrefix, String repositoryIdentifier) {
        this.configPrefix = configPrefix;
        this.repositoryIdentifier = repositoryIdentifier;
        this.recordUriPattern = MCRConfiguration.instance().getString(configPrefix + "Adapter.RecordURIPattern");
        this.headerUriPattern = MCRConfiguration.instance().getString(configPrefix + "Adapter.HeaderURIPattern");
    }

    /**
     * Converts a oai identifier to a mycore id.
     */
    public String getMyCoReId(String oaiId) {
        return oaiId.substring(oaiId.lastIndexOf(':') + 1);
    }

    /**
     * Converts a mycore id to a oai id.
     */
    public String getOAIId(String mcrId) {
        return "oai:" + this.repositoryIdentifier + ":" + mcrId;
    }

    public Record getRecord(String mcrID, MetadataFormat format) {
        Element recordElement;
        try {
            recordElement = getJDOMRecord(mcrID, format);
        } catch (Exception exc) {
            LOGGER.error("unable to get record " + mcrID + " (" + format.getPrefix() + ")");
            return null;
        }
        Element headerElement = recordElement.getChild("header", OAIConstants.NS_OAI);
        if (headerElement == null) {
            LOGGER.error("Header element of record " + mcrID + " (" + format.getPrefix() + ") is null!");
            return null;
        }
        Header header = headerToHeader(headerElement);
        Element metadataElement = recordElement.getChild("metadata", OAIConstants.NS_OAI);
        if (metadataElement != null && metadataElement.getChildren().size() > 0) {
            Element metadataChild = (Element) metadataElement.getChildren().get(0);
            metadataChild.detach();
            return new Record(header, new SimpleMetadata(metadataChild));
        } else {
            return new Record(header);
        }
    }

    /**
     * Returns a deleted record without metadata by given MyCoRe identifier or null, if the
     * record is not deleted.
     * 
     * @param mcrId id of the deleted record
     * @return deleted record
     */
    @SuppressWarnings("rawtypes")
    public Record getDeletedRecord(String mcrId) {
        try {
            // building the query
            MCRHIBConnection conn = MCRHIBConnection.instance();
            Criteria criteria = conn.getSession().createCriteria(MCRDELETEDITEMS.class);
            criteria.setProjection(Projections.property("id.dateDeleted"));
            Criterion idCriterion = Restrictions.eq("id.identifier", mcrId);
            criteria.add(idCriterion);
            List resultList = criteria.list();
            if (resultList.size() > 0) {
                Timestamp timestamp = (Timestamp) resultList.get(0);
                Header header = new Header(getOAIId(mcrId), new Date(timestamp.getTime()), Status.deleted);
                Record record = new Record(header);
                return record;
            }
        } catch (Exception ex) {
            LOGGER.warn("Error while retrieving deleted record " + mcrId, ex);
        }
        return null;
    }

    public Header getHeader(String mcrId, MetadataFormat format) {
        try {
            Element headerElement = getJDOMHeader(mcrId, format);
            return headerElement != null ? headerToHeader(headerElement) : null;
        } catch (Exception exc) {
            LOGGER.error("unable to get header element of record " + mcrId + " (" + format.getPrefix() + ")", exc);
            return null;
        }
    }

    protected Element getJDOMRecord(String mcrId, MetadataFormat format) {
        String uri = formatURI(this.recordUriPattern, mcrId, format.getPrefix());
        return getURI(uri);
    }

    protected Element getJDOMHeader(String mcrId, MetadataFormat format) {
        String uri = formatURI(this.headerUriPattern, mcrId, format.getPrefix());
        return getURI(uri);
    }

    protected Element getURI(String uri) {
        Element e = MCRURIResolver.instance().resolve(uri).detach();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("get " + uri);
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            LOGGER.debug(out.outputString(e));
        }
        return e;
    }

    protected String formatURI(String uri, String id, String metadataPrefix) {
        MCRObjectID mcrID = null;
        try {
            mcrID = MCRObjectID.getInstance(id);
        } catch (Exception exc) {
            // just check if its a valid mcr id
        }
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
        return uri.replace("{id}", id).replace("{format}", metadataPrefix).replace("{objectType}", objectType)
                .replace(":{flag}", !exists ? ":deletedMcrObject" : "");
    }

    Header headerToHeader(Element headerElement) {
        String id = headerElement.getChildText("identifier", OAIConstants.NS_OAI);
        String datestampString = headerElement.getChildText("datestamp", OAIConstants.NS_OAI);
        if (id == null || datestampString == null) {
            return null;
        }
        Date datestamp = DateUtils.parseUTC(datestampString);
        Header header = new Header(id, datestamp);
        if (!exists(id)) {
            header.setDeleted(true);
        }
        for (Object setSpec : headerElement.getChildren("setSpec", OAIConstants.NS_OAI)) {
            header.getSetList().add(setToSet((Element) setSpec));
        }
        return header;
    }

    Set setToSet(Element setElement) {
        return new Set(setElement.getText());
    }

    /**
     * Checks if a mycore object with the given oai identifier exists.
     * 
     * @param oaiId
     *            e.g. oai:www.mycore.de:Document_document_00000003
     * @return true if exists, otherwise false
     */
    protected boolean exists(String oaiId) {
        String prefix = "oai:" + this.repositoryIdentifier + ":";
        String mcrId = oaiId.substring(prefix.length());
        try {
            MCRObjectID mcrObjId = MCRObjectID.getInstance(mcrId);
            return MCRXMLMetadataManager.instance().exists(mcrObjId);
        } catch (Exception ex) {
            return false;
        }
    }

}
