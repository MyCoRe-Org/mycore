/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
package org.mycore.oai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
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

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PREFIX = "MCR.OAIDataProvider.";

    public static final int DEFAULT_PARTITION_SIZE;

    protected String baseURL;

    protected MCROAIIdentify identify;

    protected String configPrefix;

    protected MCROAISearchManager searchManager;

    protected MCROAIObjectManager objectManager;

    protected MCROAISetManager setManager;

    static {
        String prefix = PREFIX + "ResumptionTokens.";
        DEFAULT_PARTITION_SIZE = MCRConfiguration2.getInt(prefix + "PartitionSize").orElse(50);
        LOGGER.info(PREFIX + "ResumptionTokens.PartitionSize is set to {}", DEFAULT_PARTITION_SIZE);
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
    }

    public MCROAISetManager getSetManager() {
        if (this.setManager == null) {
            String setManager = getConfigPrefix() + "SetManager";
            this.setManager = MCRConfiguration2.getInstanceOf(MCROAISetManager.class, setManager)
                .orElseGet(() -> MCRConfiguration2.getInstanceOfOrThrow(
                    MCROAISetManager.class, PREFIX + "DefaultSetManager"));
            int cacheMaxAge = MCRConfiguration2.getInt(this.configPrefix + "SetCache.MaxAge").orElse(0);
            this.setManager.init(getConfigPrefix(), cacheMaxAge);
        }
        return this.setManager;
    }

    public boolean moveNamespaceDeclarationsToRoot() {
        return MCRConfiguration2.getString(this.configPrefix + "MoveNamespaceDeclarationsToRoot")
            .map(Boolean::parseBoolean)
            .orElse(true);
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
            int partitionSize = MCRConfiguration2.getInt(getConfigPrefix() + "ResumptionTokens.PartitionSize")
                .orElse(DEFAULT_PARTITION_SIZE);
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
        return new ArrayList<>(getMetadataFormatMap().values());
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
        Map<String, MetadataFormat> metdataFormatMap = new HashMap<>();
        String formats = MCRConfiguration2.getString(getConfigPrefix() + "MetadataFormats").orElse("");
        StringTokenizer st = new StringTokenizer(formats, ", ");
        while (st.hasMoreTokens()) {
            String format = st.nextToken();
            String namespaceURI = MCRConfiguration2
                .getStringOrThrow(PREFIX + "MetadataFormat." + format + ".Namespace");
            String schema = MCRConfiguration2.getStringOrThrow(PREFIX + "MetadataFormat." + format + ".Schema");
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
        Header header = getSearchManager().getHeader(identifier).get();
        if (header.isDeleted()) {
            throw new NoMetadataFormatsException(identifier);
        }
        List<MetadataFormat> metadataFormats = getMetadataFormats()
            .stream()
            .filter(format -> objectManager.getRecord(header, format) != null)
            .collect(Collectors.toList());
        if (metadataFormats.isEmpty()) {
            throw new NoMetadataFormatsException(identifier);
        }
        return metadataFormats;

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
        } else {
            throw new CannotDisseminateFormatException()
                .setId(identifier)
                .setMetadataPrefix(format.getPrefix());
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
        if (recordList.isEmpty() && getHeaders(resumptionToken).isEmpty()) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return recordList;
    }

    @Override
    public OAIDataList<Record> getRecords(MetadataFormat format, Set set, Instant from, Instant until)
        throws NoSetHierarchyException, NoRecordsMatchException {
        //Update set for response header
        getSetManager().getDirectList();
        OAIDataList<Record> recordList = getSearchManager().searchRecord(format, toMCRSet(set), from, until);
        if (recordList.isEmpty() && getHeaders(format, set, from, until).isEmpty()) {
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
        throws NoSetHierarchyException, NoRecordsMatchException {
        //Update set for response header
        getSetManager().getDirectList();
        OAIDataList<Header> headerList = getSearchManager().searchHeader(format, toMCRSet(set), from, until);
        if (headerList.isEmpty()) {
            throw new NoRecordsMatchException();
        }
        return headerList;
    }

}
