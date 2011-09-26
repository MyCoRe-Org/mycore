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
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRDELETEDITEMS;
import org.mycore.common.MCRConfiguration;
import org.mycore.oai.pmh.BadResumptionTokenException;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.DefaultResumptionToken;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.pmh.Set;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Provides an interface to the MyCoRe search engine.
 * 
 * @author Matthias Eichner
 */
public class MCROAISearchManager {

    protected final static Logger LOGGER = Logger.getLogger(MCROAISearchManager.class);

    protected final static String TOKEN_DELIMITER = "@";

    protected static Map<String, MCROAIResults> resultMap;

    protected static int partitionSize;

    protected static int maxAge;

    protected MCRConfiguration config;

    protected String configPrefix;

    protected MCROAIObjectManager objManager;

    protected DeletedRecordPolicy deletedRecordPolicy;
    
    static {
        resultMap = new ConcurrentHashMap<String, MCROAIResults>();
        String prefix = MCROAIAdapter.PREFIX + "ResumptionTokens.";
        partitionSize = MCRConfiguration.instance().getInt(prefix + "PartitionSize", 50);
        maxAge = MCRConfiguration.instance().getInt(prefix + "MaxAge", 30) * 60 * 1000;

        TimerTask tt = new TimerTask() {
            public void run() {
                for (Map.Entry<String, MCROAIResults> entry : resultMap.entrySet()) {
                    String searchId = entry.getKey();
                    MCROAIResults results = entry.getValue();
                    if ((results != null) && results.isExpired()) {
                        LOGGER.info("Removing expired resumption token " + searchId);
                        resultMap.remove(searchId);
                    }
                }
            }
        };
        new Timer().schedule(tt, new Date(System.currentTimeMillis() + maxAge), maxAge);
    }

    public MCROAISearchManager() {
        this.config = MCRConfiguration.instance();
    }

    public void init(String configPrefix, DeletedRecordPolicy deletedRecordPolicy, MCROAIObjectManager objManager) {
        this.configPrefix = configPrefix;
        this.objManager = objManager;
        this.deletedRecordPolicy = deletedRecordPolicy;
    }

    public OAIDataList<Header> searchHeader(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        int tokenCursor = getTokenCursor(resumptionToken);
        MCROAIResults results = resultMap.get(searchId);
        if (results == null) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return getHeaderList(results, tokenCursor);
    }

    public OAIDataList<Record> searchRecord(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        int tokenCursor = getTokenCursor(resumptionToken);
        MCROAIResults results = resultMap.get(searchId);
        if (results == null) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return getRecordList(results, tokenCursor);
    }

    public OAIDataList<Header> searchHeader(MetadataFormat format, Set set, Date from, Date until) {
        MCROAIResults oaiResults = search(format, set, from, until);
        resultMap.put(oaiResults.getMCRResults().getID(), oaiResults);
        return getHeaderList(oaiResults, 0);
    }

    public OAIDataList<Record> searchRecord(MetadataFormat format, Set set, Date from, Date until) {
        MCROAIResults oaiResults = search(format, set, from, until);
        resultMap.put(oaiResults.getMCRResults().getID(), oaiResults);
        return getRecordList(oaiResults, 0);
    }

    protected OAIDataList<Header> getHeaderList(MCROAIResults results, int cursor) {
        MetadataFormat metadataFormat = results.getMetadataFormat();
        OAIDataList<Header> headerList = new OAIDataList<Header>();
        MCRResults mcrResults = results.getMCRResults();
        int numHits = mcrResults.getNumHits();
        int max = Math.min(numHits, cursor + partitionSize);
        for (; cursor < max; cursor++) {
            MCRHit hit = mcrResults.getHit(cursor);
            Header header = this.objManager.getHeader(hit.getID(), metadataFormat);
            if (header != null) {
                headerList.add(header);
            }
        }
        this.setResumptionToken(headerList, results, cursor, numHits);
        return headerList;
    }

    protected OAIDataList<Record> getRecordList(MCROAIResults results, int cursor) {
        MetadataFormat metadataFormat = results.getMetadataFormat();
        OAIDataList<Record> recordList = new OAIDataList<Record>();
        final MCRResults mcrResults = results.getMCRResults();
        int numHits = mcrResults.getNumHits();
        int max = Math.min(numHits, cursor + partitionSize);
        for (; cursor < max; cursor++) {
            MCRHit hit = mcrResults.getHit(cursor);
            Record record = this.objManager.getRecord(hit.getID(), metadataFormat);
            if (record != null) {
                recordList.add(record);
            }
        }
        this.setResumptionToken(recordList, results, cursor, numHits);
        return recordList;
    }

    protected void setResumptionToken(OAIDataList<?> dataList, MCROAIResults results, int cursor, int hits) {
        if (cursor < hits) {
            DefaultResumptionToken rsToken = new DefaultResumptionToken();
            rsToken.setCompleteListSize(hits);
            rsToken.setCursor(cursor);
            rsToken.setExpirationDate(results.getExpirationDate());
            rsToken.setToken(results.getMCRResults().getID() + TOKEN_DELIMITER + String.valueOf(cursor));
            dataList.setResumptionToken(rsToken);
        }
    }

    protected MCROAIResults search(MetadataFormat format, Set set, Date from, Date until) {
        MCRAndCondition queryCondition = new MCRAndCondition();
        // restriction
        MCRCondition restriction = buildRestrictionCondition();
        if (restriction != null) {
            queryCondition.addChild(restriction);
        }
        // TODO: some objects may be invalid for a metadata format
        // set
        if (set != null) {
            queryCondition.addChild(buildSetCondition(set));
        }
        // from & until
        if (from != null) {
            queryCondition.addChild(buildFromCondition(from));
        }
        if (until != null) {
            queryCondition.addChild(buildUntilCondition(until));
        }
        // build query
        MCRQuery query = new MCRQuery(queryCondition);
        // sort
        List<MCRSortBy> sortBy = buildSortByList();
        query.setSortBy(sortBy);
        // search
        MCRResults queryResults = MCRQueryManager.search(query);
        // deleted records
        MCRResults completeResults = searchDeleted(from, until);
        if (completeResults.isReadonly()) {
            completeResults = MCRResults.union(completeResults, queryResults);
        } else {
            completeResults.addHits(queryResults);
        }
        // create new MCROAIResults
        Date expirationDate = new Date(System.currentTimeMillis() + maxAge);
        MCROAIResults oaiResults = new MCROAIResults(expirationDate, format, completeResults);
        return oaiResults;
    }

    /**
     * Returns a list with identifiers of the deleted objects within the given date boundary. If the record policy indicates that there is not support for
     * tracking deleted items empty list is returned.
     * 
     * @return a list with identifiers of the deleted objects
     */
    @SuppressWarnings("unchecked")
    protected MCRResults searchDeleted(Date from, Date until) {
        MCRResults mcrResults = new MCRResults();
        if (DeletedRecordPolicy.No.equals(deletedRecordPolicy) || DeletedRecordPolicy.Transient.equals(deletedRecordPolicy)) {
            return mcrResults;
        }
        LOGGER.info("Getting identifiers of deleted items");
        List<String> deletedItems = new Vector<String>();
        try {
            // building the query
            MCRHIBConnection conn = MCRHIBConnection.instance();
            Criteria criteria = conn.getSession().createCriteria(MCRDELETEDITEMS.class);
            criteria.setProjection(Projections.property("id.identifier"));
            if (from != null && until != null) {
                Criterion lowerBound = Restrictions.ge("id.dateDeleted", from);
                Criterion upperBound = Restrictions.le("id.dateDeleted", until);
                criteria.add(Restrictions.and(lowerBound, upperBound));
            } else if (from != null) {
                criteria.add(Restrictions.ge("id.dateDeleted", from));
            } else if (until != null) {
                criteria.add(Restrictions.le("id.dateDeleted", until));
            }
            deletedItems = criteria.list();
            for (String id : deletedItems) {
                mcrResults.addHit(new MCRHit(id));
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not retrieve identifiers of deleted objects", ex);
        }
        return mcrResults;
    }

    protected MCRCondition buildRestrictionCondition() {
        return MCROAIUtils.getDefaultRestriction(this.configPrefix);
    }

    protected MCRCondition buildSetCondition(Set set) {
        return MCROAIUtils.getDefaultSetCondition(set.getSpec(), this.configPrefix);
    }

    protected MCRCondition buildFromCondition(Date from) {
        return buildFromUntilCondition(from, ">=");
    }

    protected MCRCondition buildUntilCondition(Date until) {
        return buildFromUntilCondition(until, "<=");
    }

    private MCRCondition buildFromUntilCondition(Date date, String compareSign) {
        String fieldFromUntil = this.config.getString(this.configPrefix + "Search.FromUntil", "modified");
        String[] fields = fieldFromUntil.split(" *, *");
        MCROrCondition orCond = new MCROrCondition();
        for (String fDef : fields) {
            MCRFieldDef d = MCRFieldDef.getDef(fDef);
            orCond.addChild(new MCRQueryCondition(d, compareSign, DateUtils.formatUTCSecond(date)));
        }
        return orCond;
    }

    protected List<MCRSortBy> buildSortByList() {
        return MCROAIUtils.getSortByList(this.configPrefix + "Search.SortBy", "modified descending, id descending");
    }

    public String getSearchId(String token) throws BadResumptionTokenException {
        try {
            return token.split(TOKEN_DELIMITER)[0];
        } catch (Exception exc) {
            throw new BadResumptionTokenException(token);
        }
    }

    public int getTokenCursor(String token) throws BadResumptionTokenException {
        try {
            String[] tokenParts = token.split(TOKEN_DELIMITER);
            String cursorPart = tokenParts[tokenParts.length - 1];
            return Integer.valueOf(cursorPart);
        } catch (Exception exc) {
            throw new BadResumptionTokenException(token);
        }
    }

}
