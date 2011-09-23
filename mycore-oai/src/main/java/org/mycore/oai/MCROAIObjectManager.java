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

import java.util.Date;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.Header;
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

    protected MCROAIAdapter oaiAdapter;

    protected String recordUriPattern;

    protected String headerUriPattern;

    public MCROAIObjectManager(MCROAIAdapter oaiAdapter) {
        this.oaiAdapter = oaiAdapter;
        this.recordUriPattern = MCRConfiguration.instance().getString(oaiAdapter.getConfigPrefix() + "Adapter.RecordURIPattern");
        this.headerUriPattern = MCRConfiguration.instance().getString(oaiAdapter.getConfigPrefix() + "Adapter.HeaderURIPattern");
    }

    public static String getMyCoReId(String oaiId) {
        return oaiId.substring(oaiId.lastIndexOf(':') + 1);
    }

    public String getOAIId(String mcrId) {
        return "oai:" + getReposId() + ":" + mcrId;
    }

    public Record getRecord(String mcrId, MetadataFormat format) {
        Element recordElement = getJDOMRecord(mcrId, format);
        Element headerElement = recordElement.getChild("header", OAIConstants.NS_OAI);
        Header header = headerToHeader(headerElement);
        Element metadataElement = recordElement.getChild("metadata", OAIConstants.NS_OAI);
        if (metadataElement != null && metadataElement.getContentSize() > 0) {
            Element metadataChild = (Element) metadataElement.getChildren().get(0);
            metadataChild.detach();
            return new Record(header, new SimpleMetadata(metadataChild));
        } else {
            return new Record(header);
        }
    }

    public Header getHeader(String mcrId, MetadataFormat format) {
        Element headerElement = getJDOMHeader(mcrId, format);
        return headerToHeader(headerElement);
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
        LOGGER.debug("get " + uri);
        return (Element) (MCRURIResolver.instance().resolve(uri).detach());
    }

    public String formatURI(String uri, String mcrId, String metadataPrefix) {
        String objectType = MCRObjectID.getIDParts(mcrId)[1];
        boolean exists = MCRMetadataManager.exists(MCRObjectID.getInstance(mcrId));
        String value = uri.replace("{id}", mcrId).replace("{format}", metadataPrefix).replace("{objectType}", objectType)
                .replace(":{flag}", exists == false ? ":deletedMcrObject" : "");
        return value;
    }

    Header headerToHeader(Element headerElement) {
        String id = headerElement.getChildText("identifier", OAIConstants.NS_OAI);
        String datestampString = headerElement.getChildText("datestamp", OAIConstants.NS_OAI);
        if(id == null || datestampString == null) {
            return null;
        }
        Date datestamp = DateUtils.parseUTC(datestampString);
        Header header = new Header(id, datestamp);
        if(!exists(id)) {
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
        String prefix = "oai:" + getReposId() + ":";
        String mcrId = oaiId.substring(prefix.length());
        try {
            MCRObjectID mcrObjId = MCRObjectID.getInstance(mcrId);
            return MCRXMLMetadataManager.instance().exists(mcrObjId);
        } catch (Exception ex) {
            String msg = "Exception while checking existence of object " + mcrId;
            LOGGER.warn(msg, ex);
            return false;
        }
    }

    private String getReposId() {
        MCROAIIdentify identify = (MCROAIIdentify) this.oaiAdapter.getIdentify();
        return identify.getIdentifierDescription().getRepositoryIdentifier();
    }
}
