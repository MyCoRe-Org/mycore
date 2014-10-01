package org.mycore.oai;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.pmh.BadResumptionTokenException;
import org.mycore.oai.pmh.DefaultResumptionToken;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.pmh.Set;

public class MCROAISearchManager {

    protected final static Logger LOGGER = Logger.getLogger(MCROAISearchManager.class);

    protected final static String TOKEN_DELIMITER = "@";

    protected static int MAX_AGE;

    protected Map<String, MCROAISearcher> resultMap;

    protected String configPrefix;

    protected MCROAIObjectManager objManager;

    protected DeletedRecordPolicy deletedRecordPolicy;

    protected int partitionSize;

    static {
        String prefix = MCROAIAdapter.PREFIX + "ResumptionTokens.";
        MAX_AGE = getConfig().getInt(prefix + "MaxAge", 30) * 60 * 1000;
    }

    public MCROAISearchManager() {
        this.resultMap = new ConcurrentHashMap<String, MCROAISearcher>();
        TimerTask tt = new TimerTask() {
            public void run() {
                for (Map.Entry<String, MCROAISearcher> entry : resultMap.entrySet()) {
                    String searchId = entry.getKey();
                    MCROAISearcher searcher = entry.getValue();
                    if ((searcher != null) && searcher.isExpired()) {
                        LOGGER.info("Removing expired resumption token " + searchId);
                        resultMap.remove(searchId);
                    }
                }
            }
        };
        new Timer().schedule(tt, new Date(System.currentTimeMillis() + MAX_AGE), MAX_AGE);
    }

    public void init(String configPrefix, DeletedRecordPolicy deletedRecordPolicy, MCROAIObjectManager objManager,
        int partitionSize) {
        this.configPrefix = configPrefix;
        this.objManager = objManager;
        this.deletedRecordPolicy = deletedRecordPolicy;
        this.partitionSize = partitionSize;
    }

    public OAIDataList<Header> searchHeader(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        int tokenCursor = getTokenCursor(resumptionToken);
        MCROAISearcher searcher = this.resultMap.get(searchId);
        if (searcher == null || tokenCursor < 0) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        MCROAIResult result = searcher.query(tokenCursor);
        return getHeaderList(searcher, result, tokenCursor);
    }

    public OAIDataList<Record> searchRecord(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        int tokenCursor = getTokenCursor(resumptionToken);
        MCROAISearcher searcher = this.resultMap.get(searchId);
        if (searcher == null || tokenCursor < 0) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        MCROAIResult result = searcher.query(tokenCursor);
        return getRecordList(searcher, result, tokenCursor);
    }

    public OAIDataList<Header> searchHeader(MetadataFormat format, Set set, Date from, Date until) {
        MCROAISearcher searcher = getSearcher(getConfigPrefix(), format, getDeletedRecordPolicy(), getPartitionSize());
        this.resultMap.put(searcher.getID(), searcher);
        MCROAIResult result = searcher.query(set, from, until);
        return getHeaderList(searcher, result, 0);
    }

    public OAIDataList<Record> searchRecord(MetadataFormat format, Set set, Date from, Date until) {
        MCROAISearcher searcher = getSearcher(getConfigPrefix(), format, getDeletedRecordPolicy(), getPartitionSize());
        this.resultMap.put(searcher.getID(), searcher);
        MCROAIResult result = searcher.query(set, from, until);
        return getRecordList(searcher, result, 0);
    }

    protected OAIDataList<Record> getRecordList(MCROAISearcher searcher, MCROAIResult result, int cursor) {
        OAIDataList<Record> recordList = new OAIDataList<Record>();
        int numHits = result.getNumHits();
        int max = Math.min(numHits, cursor + getPartitionSize());
        for (; cursor < max; cursor++) {
            Record record = this.objManager.getRecord(result.getID(cursor), searcher.getMetadataFormat());
            if (record != null) {
                recordList.add(record);
            }
        }
        this.setResumptionToken(recordList, searcher.getID(), searcher.getExpirationDate(), cursor, numHits);
        return recordList;
    }

    protected OAIDataList<Header> getHeaderList(MCROAISearcher searcher, MCROAIResult result, int cursor) {
        OAIDataList<Header> headerList = new OAIDataList<Header>();
        int numHits = result.getNumHits();
        int max = Math.min(numHits, cursor + getPartitionSize());
        for (; cursor < max; cursor++) {
            Header header = this.objManager.getHeader(result.getID(cursor), searcher.getMetadataFormat());
            if (header != null) {
                headerList.add(header);
            }
        }
        this.setResumptionToken(headerList, searcher.getID(), searcher.getExpirationDate(), cursor, numHits);
        return headerList;
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

    protected void setResumptionToken(OAIDataList<?> dataList, String id, Date expirationDate, int cursor, int hits) {
        boolean setToken = cursor < hits;
        if (getPartitionSize() != cursor || setToken) {
            DefaultResumptionToken rsToken = new DefaultResumptionToken();
            rsToken.setCompleteListSize(hits);
            rsToken.setCursor(cursor);
            rsToken.setExpirationDate(expirationDate);
            if (setToken) {
                rsToken.setToken(id + TOKEN_DELIMITER + String.valueOf(cursor));
            }
            dataList.setResumptionToken(rsToken);
        }
    }

    public int getPartitionSize() {
        return partitionSize;
    }

    public DeletedRecordPolicy getDeletedRecordPolicy() {
        return deletedRecordPolicy;
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    protected static MCRConfiguration getConfig() {
        return MCRConfiguration.instance();
    }

    public static MCROAISearcher getSearcher(String configPrefix, MetadataFormat format,
        DeletedRecordPolicy deletedRecordPolicy, int partitionSize) {
        MCROAISearcher searcher = getConfig().<MCROAISearcher> getInstanceOf(configPrefix + "Searcher",
            "org.mycore.oai.MCROAISolrSearcher");
        searcher.init(configPrefix, format, new Date(System.currentTimeMillis() + MAX_AGE), deletedRecordPolicy,
            partitionSize);
        return searcher;
    }

}
