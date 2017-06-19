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

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.pmh.BadResumptionTokenException;
import org.mycore.oai.pmh.CannotDisseminateFormatException;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.IdDoesNotExistException;
import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.NoMetadataFormatsException;
import org.mycore.oai.pmh.NoRecordsMatchException;
import org.mycore.oai.pmh.NoSetHierarchyException;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.pmh.Set;
import org.mycore.oai.pmh.dataprovider.OAIAdapter;
import org.mycore.oai.set.MCRSet;

/**
 * Default MyCoRe {@link OAIAdapter} implementation.
 * 
 * @author Matthias Eichner
 */
public class MCROAIAdapter implements OAIAdapter {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAIAdapter.class);

    protected final static ZoneId UTC_ZONE = ZoneId.of("UTC");

    public final static String PREFIX = "MCR.OAIDataProvider.";

    public static int DEFAULT_PARTITION_SIZE;

    protected String baseURL;

    protected MCROAIIdentify identify;

    protected String configPrefix;

    protected MCRConfiguration config;

    protected MCROAISearchManager searchManager;

    protected MCROAIObjectManager objectManager;

    protected MCROAISetManager setManager;

    static {
        String prefix = MCROAIAdapter.PREFIX + "ResumptionTokens.";
        DEFAULT_PARTITION_SIZE = MCRConfiguration.instance().getInt(prefix + "PartitionSize", 50);
        LOGGER.info(MCROAIAdapter.PREFIX + "ResumptionTokens.PartitionSize is set to " + DEFAULT_PARTITION_SIZE);
    }

    /**
     * Initialize the adapter.
     * 
     * @param baseURL
     *            baseURL of the adapter e.g. http://localhost:8291/oai2
     * @param oaiConfiguration
     *            specifies the OAI-PMH configuration
     */
    public void init(String baseURL, String oaiConfiguration) {
        this.baseURL = baseURL;
        this.configPrefix = PREFIX + oaiConfiguration + ".";
        this.config = MCRConfiguration.instance();
    }

    public MCROAISetManager getSetManager() {
        if (this.setManager == null) {
            this.setManager = MCRConfiguration.instance().getInstanceOf(getConfigPrefix() + "SetManager",
                MCROAISetManager.class.getName());
            int cacheMaxAge = MCRConfiguration.instance().getInt(this.configPrefix + "SetCache.MaxAge", 0);
            this.setManager.init(getConfigPrefix(), cacheMaxAge);
        }
        return this.setManager;
    }

    public MCROAIObjectManager getObjectManager() {
        if (this.objectManager == null) {
            this.objectManager = new MCROAIObjectManager();
            this.objectManager.init(getIdentify());
        }
        return this.objectManager;
    }

    public MCROAISearchManager getSearchManager() {
        if (this.searchManager == null) {
            this.searchManager = new MCROAISearchManager();
            int partitionSize = MCRConfiguration.instance().getInt(getConfigPrefix() + "ResumptionTokens.PartitionSize",
                DEFAULT_PARTITION_SIZE);
            this.searchManager.init(getIdentify(), getObjectManager(), getSetManager(), partitionSize);
        }
        return this.searchManager;
    }

    public String getConfigPrefix() {
        return this.configPrefix;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getIdentify()
     */
    @Override
    public MCROAIIdentify getIdentify() {
        if (this.identify == null) {
            this.identify = new MCROAIIdentify(this.baseURL, getConfigPrefix());
        }
        return this.identify;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getSets()
     */
    @Override
    public OAIDataList<Set> getSets() throws NoSetHierarchyException {
        MCROAISetManager setManager = getSetManager();
        OAIDataList<MCRSet> setList2 = setManager.get();
        OAIDataList<Set> setList = cast(setList2);
        if (setList.isEmpty()) {
            throw new NoSetHierarchyException();
        }
        return setList;
    }

    @SuppressWarnings("unchecked")
    private OAIDataList<Set> cast(OAIDataList<? extends Set> setList) {
        return (OAIDataList<Set>) setList;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getSets(java.lang.String)
     */
    @Override
    public OAIDataList<Set> getSets(String resumptionToken)
        throws NoSetHierarchyException, BadResumptionTokenException {
        MCROAISetManager setManager = getSetManager();
        OAIDataList<Set> setList = cast(setManager.get());
        if (setList.isEmpty()) {
            throw new NoSetHierarchyException();
        }
        // TODO: this is like to old oai implementation but actually wrong with a large amount of sets
        throw new BadResumptionTokenException();
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getSet(java.lang.String)
     */
    @Override
    public MCRSet getSet(String setSpec) throws NoSetHierarchyException, NoRecordsMatchException {
        MCROAISetManager setManager = getSetManager();
        OAIDataList<MCRSet> setList = setManager.get();
        if (setList.isEmpty()) {
            throw new NoSetHierarchyException();
        }
        MCRSet set = MCROAISetManager.get(setSpec, setList);
        if (set == null) {
            throw new NoRecordsMatchException();
        }
        return set;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getMetadataFormats()
     */
    @Override
    public List<MetadataFormat> getMetadataFormats() {
        return new ArrayList<MetadataFormat>(getMetadataFormatMap().values());
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getMetadataFormat(java.lang.String)
     */
    @Override
    public MetadataFormat getMetadataFormat(String prefix) throws CannotDisseminateFormatException {
        MetadataFormat format = getMetadataFormatMap().get(prefix);
        if (format == null) {
            throw new CannotDisseminateFormatException();
        }
        return format;
    }

    protected Map<String, MetadataFormat> getMetadataFormatMap() {
        Map<String, MetadataFormat> metdataFormatMap = new HashMap<String, MetadataFormat>();
        String formats = this.config.getString(getConfigPrefix() + "MetadataFormats", "");
        StringTokenizer st = new StringTokenizer(formats, ", ");
        while (st.hasMoreTokens()) {
            String format = st.nextToken();
            String namespaceURI = this.config.getString(PREFIX + "MetadataFormat." + format + ".Namespace");
            String schema = this.config.getString(PREFIX + "MetadataFormat." + format + ".Schema");
            metdataFormatMap.put(format, new MetadataFormat(format, namespaceURI, schema));
        }
        return metdataFormatMap;
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getMetadataFormats(java.lang.String)
     */
    @Override
    public List<MetadataFormat> getMetadataFormats(String identifier)
        throws IdDoesNotExistException, NoMetadataFormatsException {
        if (!getObjectManager().exists(identifier)) {
            throw new IdDoesNotExistException(identifier);
        }
        return getMetadataFormats();
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getRecord(java.lang.String, org.mycore.oai.pmh.MetadataFormat)
     */
    @Override
    public Record getRecord(String identifier, MetadataFormat format)
        throws CannotDisseminateFormatException, IdDoesNotExistException {
        //Update set for response header
        getSetManager().getDirectList();
        Optional<Record> possibleRecord = getSearchManager()
            .getHeader(identifier)
            .map(h -> objectManager.getRecord(h, format));
        if (possibleRecord.isPresent()) {
            return possibleRecord.get();
        }
        if (!objectManager.exists(identifier)) {
            DeletedRecordPolicy rP = getIdentify().getDeletedRecordPolicy();
            if (DeletedRecordPolicy.Persistent.equals(rP)) {
                // get deleted item
                Record deletedRecord = objectManager.getDeletedRecord(objectManager.getMyCoReId(identifier));
                if (deletedRecord != null) {
                    return deletedRecord;
                }
            }
        }
        throw new IdDoesNotExistException(identifier);
    }

    /*
     * (non-Javadoc)
     * @see org.mycore.oai.pmh.dataprovider.OAIAdapter#getRecords(java.lang.String)
     */
    @Override
    public OAIDataList<Record> getRecords(String resumptionToken) throws BadResumptionTokenException {
        //Update set for response header
        getSetManager().getDirectList();
        OAIDataList<Record> recordList = getSearchManager().searchRecord(resumptionToken);
        if (recordList.isEmpty()) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return recordList;
    }

    @Override
    public OAIDataList<Record> getRecords(MetadataFormat format, Set set, Instant from, Instant until)
        throws CannotDisseminateFormatException, NoSetHierarchyException, NoRecordsMatchException {
        //Update set for response header
        getSetManager().getDirectList();
        OAIDataList<Record> recordList = getSearchManager().searchRecord(format, toMCRSet(set), from, until);
        if (recordList.isEmpty()) {
            throw new NoRecordsMatchException();
        }
        return recordList;
    }

    private MCRSet toMCRSet(Set set) throws NoSetHierarchyException, NoRecordsMatchException {
        if (set == null) {
            return null;
        }
        return getSet(set.getSpec());
    }

    @Override
    public OAIDataList<Header> getHeaders(String resumptionToken) throws BadResumptionTokenException {
        //Update set for response header
        getSetManager().getDirectList();
        OAIDataList<Header> headerList = getSearchManager().searchHeader(resumptionToken);
        if (headerList.isEmpty()) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return headerList;
    }

    @Override
    public OAIDataList<Header> getHeaders(MetadataFormat format, Set set, Instant from, Instant until)
        throws CannotDisseminateFormatException, NoSetHierarchyException, NoRecordsMatchException {
        //Update set for response header
        getSetManager().getDirectList();
        OAIDataList<Header> headerList = getSearchManager().searchHeader(format, toMCRSet(set), from, until);
        if (headerList.isEmpty()) {
            throw new NoRecordsMatchException();
        }
        return headerList;
    }

}
