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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.parsers.bool.MCRCondition;

/**
 * @author Frank L\u00fctzenkirchen
 */
public abstract class MCROAIAdapter {
    protected final static Logger LOGGER = Logger.getLogger(MCRVerbHandler.class);

    protected String recordUriPattern;

    protected String headerUriPattern;

    private String repositoryName;

    private String repositoryIdentifier;

    private String adminEmail;

    /**
     * List of metadata formats supported by this data provider instance.
     */
    private List<MCRMetadataFormat> metadataFormats = new ArrayList<MCRMetadataFormat>();

    /** The earliest datestamp supported by this data provider instance. */
    protected static String EARLIEST_DATESTAMP;

    /** the sample id */
    private String recordSampleID;

    /** deleted record policy value */
    private String deletedRecord;

    void init(String prefix) {
        MCRConfiguration config = MCRConfiguration.instance();
        recordUriPattern = MCRConfiguration.instance().getString(prefix + "RecordURIPattern");
        EARLIEST_DATESTAMP = config.getString(prefix + "EarliestDatestamp", "1970-01-01");
        headerUriPattern = MCRConfiguration.instance().getString(prefix + "HeaderURIPattern");
        repositoryName = config.getString(prefix + "RepositoryName");
        repositoryIdentifier = config.getString(prefix + "RepositoryIdentifier");
        adminEmail = config.getString(prefix + "AdminEmail", config.getString("MCR.Mail.Address"));
        recordSampleID = config.getString(prefix + "RecordSampleID");
        deletedRecord = config.getString(prefix + "DeletedRecord", "transient");
    }

    /** Returns the list of supported metadata formats */
    public List<MCRMetadataFormat> listMetadataFormats(String id, List<MCRMetadataFormat> defaults) {
        return defaults;
    }

    public Element getRecord(String id, MCRMetadataFormat format) {
        String uri = formatURI(recordUriPattern, id, format);
        return getURI(uri);
    }

    public Element getHeader(String id, MCRMetadataFormat format) {
        String uri = formatURI(headerUriPattern, id, format);
        return getURI(uri);
    }

    public String formatURI(String uri, String id, MCRMetadataFormat format) {
        return uri.replace("{id}", id).replace("{format}", format.getPrefix());
    }

    /** Returns the name of the repository */
    public String getRepositoryName() {
        return repositoryName;
    }

    /** Returns the id of the oai repository */
    public String getRepositoryIdentifier() {
        return repositoryIdentifier;
    }

    /**
     * Returns the earliest datestamp supported by this data provider instance.
     * That is the guaranteed lower limit of all datestamps recording changes,
     * modifications, or deletions in the repository. A repository must not use
     * datestamps lower than the one specified by the content of the
     * earliestDatestamp element. Configuration is done using a property, for
     * example MCR.OAIDataProvider.OAI.EarliestDatestamp=1970-01-01
     */
    public String getEarliestDatestamp() {
        return EARLIEST_DATESTAMP;
    }

    /** Returns a samle record id */
    public String getRecordSampleID() {
        return recordSampleID;
    }

    /**
     * Returns the policy for deleted items
     * 
     * @return one of no, transient or persistent
     */
    public String getDeletedRecord() {
        return deletedRecord;
    }

    /** Returns the admin email adress */
    public String getAdminEmail() {
        return adminEmail;
    }

    /**
     * Returns the metadata formats supported by this data provider instance.
     * For each instance, a configuration property lists the prefixes of all
     * supported formats, for example
     * MCR.OAIDataProvider.OAI.MetadataFormats=oai_dc Each metadata format must
     * be globally configured with its prefix, schema and namespace, for example
     * MCR.OAIDataProvider.MetadataFormat.oai_dc.Schema=http://www.openarchives.
     * org/OAI/2.0/oai_dc.xsd
     * MCR.OAIDataProvider.MetadataFormat.oai_dc.Namespace
     * =http://www.openarchives.org/OAI/2.0/oai_dc/
     * 
     * @see MCRMetadataFormat
     */
    List<MCRMetadataFormat> getMetadataFormats() {
        return metadataFormats;
    }

    protected Element getURI(String uri) {
        LOGGER.debug("get " + uri);
        return (Element) (MCRURIResolver.instance().resolve(uri).detach());
    }

    public abstract MCRCondition buildSetCondition(String setSpec);

    public abstract boolean exists(String identifier);
}
