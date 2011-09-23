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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
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

    protected MCROAIAdapter oaiAdapter;

    protected MCRConfiguration config;

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

    public MCROAISearchManager(MCROAIAdapter oaiAdapter) {
        this.oaiAdapter = oaiAdapter;
        this.config = MCRConfiguration.instance();
    }

    public MCROAIAdapter getOaiAdapter() {
        return this.oaiAdapter;
    }

    public OAIDataList<Header> searchHeader(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        int tokenCursor = getTokenCursor(resumptionToken);
        MCROAIResults results = resultMap.get(searchId);
        if(results == null) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return getHeaderList(results, tokenCursor);
    }

    public OAIDataList<Record> searchRecord(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        int tokenCursor = getTokenCursor(resumptionToken);
        MCROAIResults results = resultMap.get(searchId);
        if(results == null) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        return getRecordList(results, tokenCursor);
    }

    public OAIDataList<Header> searchHeader(MetadataFormat format, Set set, Date from, Date until) {
        MCROAIResults oaiResults = search(format, set, from, until);
        resultMap.put(oaiResults.getID(), oaiResults);
        return getHeaderList(oaiResults, 0);
    }

    public OAIDataList<Record> searchRecord(MetadataFormat format, Set set, Date from, Date until) {
        MCROAIResults oaiResults = search(format, set, from, until);
        resultMap.put(oaiResults.getID(), oaiResults);
        return getRecordList(oaiResults, 0);
    }

    protected OAIDataList<Header> getHeaderList(MCROAIResults results, int cursor) {
        MetadataFormat metadataFormat = results.getMetadataFormat();
        OAIDataList<Header> headerList = new OAIDataList<Header>();
        int numHits = results.getNumHits();
        int max = Math.min(numHits, cursor + partitionSize);
        for (; cursor < max; cursor++) {
            MCRHit hit = results.getHit(cursor);
            Header header = getOaiAdapter().getObjectManager().getHeader(hit.getID(), metadataFormat);
            if(header != null) {
                headerList.add(header);
            }
        }
        this.setResumptionToken(headerList, results, cursor, numHits);
        return headerList;
    }

    protected OAIDataList<Record> getRecordList(MCROAIResults results, int cursor) {
        MetadataFormat metadataFormat = results.getMetadataFormat();
        OAIDataList<Record> recordList = new OAIDataList<Record>();
        int numHits = results.getNumHits();
        int max = Math.min(numHits, cursor + partitionSize);
        for (; cursor < max; cursor++) {
            MCRHit hit = results.getHit(cursor);
            Record record = getOaiAdapter().getObjectManager().getRecord(hit.getID(), metadataFormat);
            if(record != null) {
                recordList.add(record);
            }
        }
        this.setResumptionToken(recordList, results, cursor, numHits);
        return recordList;
    }

    protected void setResumptionToken(OAIDataList<?> dataList, MCROAIResults results, int cursor, int hits) {
        if(cursor < hits) {
            DefaultResumptionToken rsToken = new DefaultResumptionToken();
            rsToken.setCompleteListSize(hits);
            rsToken.setCursor(cursor);
            rsToken.setExpirationDate(results.getExpirationDate());
            rsToken.setToken(results.getID() + TOKEN_DELIMITER + String.valueOf(cursor));
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
        MCRResults results = MCRQueryManager.search(query);
        // deleted records
        List<String> deletedList = searchDeleted(from, until);
        // create new MCROAIResults
        Date expirationDate = new Date(System.currentTimeMillis() + maxAge);
        MCROAIResults oaiResults = new MCROAIResults(expirationDate, format);
        oaiResults.add(results);
        oaiResults.add(deletedList);
        return oaiResults;
    }

    /**
     * Returns a list with identifiers of the deleted objects within the given date boundary. If the record policy indicates that there is not support
     * for tracking deleted items empty list is returned.
     * 
     * @return a list with identifiers of the deleted objects
     */
    protected List<String> searchDeleted(Date from, Date until) {
        DeletedRecordPolicy policy = getOaiAdapter().getIdentify().getDeletedRecordPolicy();
        if (DeletedRecordPolicy.No.equals(policy) || DeletedRecordPolicy.Transient.equals(policy)) {
            return new ArrayList<String>();
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
        } catch (Exception ex) {
            LOGGER.warn("Could not retrieve identifiers of deleted objects", ex);
        }
        return deletedItems;
    }

    protected MCRCondition buildRestrictionCondition() {
        return MCROAIUtils.getDefaultRestriction(this.oaiAdapter.getConfigPrefix());
    }

    protected MCRCondition buildSetCondition(Set set) {
        return MCROAIUtils.getDefaultSetCondition(set.getSpec(), this.oaiAdapter.getConfigPrefix());
    }

    protected MCRCondition buildFromCondition(Date from) {
        return buildFromUntilCondition(from, ">=");
    }

    protected MCRCondition buildUntilCondition(Date until) {
        return buildFromUntilCondition(until, "<=");
    }

    private MCRCondition buildFromUntilCondition(Date date, String compareSign) {
        String fieldFromUntil = this.config.getString(this.oaiAdapter.getConfigPrefix() + "Search.FromUntil", "modified");
        String[] fields = fieldFromUntil.split(" *, *");
        MCROrCondition orCond = new MCROrCondition();
        for (String fDef : fields) {
            MCRFieldDef d = MCRFieldDef.getDef(fDef);
            orCond.addChild(new MCRQueryCondition(d, compareSign, DateUtils.formatUTCSecond(date)));
        }
        return orCond;
    }

    protected List<MCRSortBy> buildSortByList() {
        List<MCRSortBy> sortBy = new ArrayList<MCRSortBy>();
        String searchSortBy = config.getString(this.oaiAdapter.getConfigPrefix() + "Search.SortBy", "modified descending, id descending");
        for (StringTokenizer st = new StringTokenizer(searchSortBy, ",;:"); st.hasMoreTokens();) {
            String token = st.nextToken().trim();
            MCRFieldDef field = MCRFieldDef.getDef(token.split(" ")[0]);
            boolean order = "ascending".equalsIgnoreCase(token.split(" ")[1]);
            sortBy.add(new MCRSortBy(field, order));
        }
        return sortBy;
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
