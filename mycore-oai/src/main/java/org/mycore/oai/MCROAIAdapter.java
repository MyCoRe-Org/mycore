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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

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

    private String recordPolicy;

    /** The earliest datestamp supported by this data provider instance. */
    protected static String EARLIEST_DATESTAMP;

    void init(String prefix) {
        MCRConfiguration config = MCRConfiguration.instance();

        recordUriPattern = MCRConfiguration.instance().getString(prefix + "Adapter.RecordURIPattern");
        headerUriPattern = MCRConfiguration.instance().getString(prefix + "Adapter.HeaderURIPattern");
        EARLIEST_DATESTAMP = config.getString(prefix + "Adapter.EarliestDatestamp", "1970-01-01");
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

    protected Element getURI(String uri) {
        LOGGER.debug("get " + uri);
        return (Element) (MCRURIResolver.instance().resolve(uri).detach());
    }

    public String formatURI(String uri, String id, MCRMetadataFormat format) {
        return uri.replace("{id}", id).replace("{format}", format.getPrefix());
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

    /**
     * Sublasses should override this method. This implementation returns an
     * empty list.
     * 
     * @param from
     * @param until
     * @return returns the identifiers of the deleted items matching the given
     *         date boundaries
     */
    List<String> getDeletedObjectsIdentifiers(String from, String until) {
        return new Vector<String>();
    }

    public abstract MCRCondition buildSetCondition(String setSpec);

    public abstract boolean exists(String identifier);

    /**
     * Returns the deleted record policy
     */
    protected String getDeletedRecordPolicy() {
        return this.recordPolicy;
    }

    /**
     * Sets the deleted record policy
     * 
     * @param policy
     *            policy to set
     */
    public void setDeletedRecordPolicy(String policy) {
        this.recordPolicy = policy;
    }

    /**
     * Sets the time of the until date to 23:59:59
     * 
     * @param until
     *            the until date
     */
    protected Date modifyUntilDate(Date until) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(until);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }
}
