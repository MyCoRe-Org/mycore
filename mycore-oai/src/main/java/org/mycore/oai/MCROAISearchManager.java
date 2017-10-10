package org.mycore.oai;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.oai.pmh.BadResumptionTokenException;
import org.mycore.oai.pmh.DefaultResumptionToken;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.set.MCRSet;
import org.mycore.util.concurrent.MCRTransactionableRunnable;

/**
 * Search manager of the mycore OAI-PMH implementation. Creates a new
 * {@link MCROAISearcher} instance for each
 * {@link #searchHeader(MetadataFormat, MCRSet, Instant, Instant)}
 * and {@link #searchRecord(MetadataFormat, MCRSet, Instant, Instant)} call.
 * The resumption token created by those methods can be reused for
 * later calls to the same searcher. A searcher is dropped after an
 * expiration time. The time increases for each query call.
 *
 * <p>Due to token based querying it is not possible to set a current
 * position for the resumption token. Its always set to -1.</p>
 *
 * @author Matthias Eichner
 */
public class MCROAISearchManager {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAISearchManager.class);

    protected final static String TOKEN_DELIMITER = "@";

    protected static int MAX_AGE;

    protected Map<String, MCROAISearcher> resultMap;

    protected MCROAIIdentify identify;

    protected MCROAIObjectManager objManager;

    protected MCROAISetManager setManager;

    protected int partitionSize;

    private ExecutorService executorService;

    private boolean runListRecordsParallel;

    static {
        String prefix = MCROAIAdapter.PREFIX + "ResumptionTokens.";
        MAX_AGE = getConfig().getInt(prefix + "MaxAge", 30) * 60 * 1000;
    }

    public MCROAISearchManager() {
        this.resultMap = new ConcurrentHashMap<>();
        TimerTask tt = new TimerTask() {
            @Override
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
        runListRecordsParallel = getConfig().getBoolean(MCROAIAdapter.PREFIX + "RunListRecordsParallel");
        if (runListRecordsParallel) {
            executorService = Executors.newWorkStealingPool();
            MCRShutdownHandler.getInstance().addCloseable(executorService::shutdownNow);
        }
    }

    public void init(MCROAIIdentify identify, MCROAIObjectManager objManager, MCROAISetManager setManager,
        int partitionSize) {
        this.identify = identify;
        this.objManager = objManager;
        this.setManager = setManager;
        this.partitionSize = partitionSize;
    }

    public Optional<Header> getHeader(String oaiId) {
        MCROAISearcher searcher = getSearcher(this.identify, null, 1, setManager, objManager);
        return searcher.getHeader(objManager.getMyCoReId(oaiId));
    }

    public OAIDataList<Header> searchHeader(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        String tokenCursor = getTokenCursor(resumptionToken);
        MCROAISearcher searcher = this.resultMap.get(searchId);
        if (searcher == null || tokenCursor == null || tokenCursor.length() <= 0) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        MCROAIResult result = searcher.query(tokenCursor);
        return getHeaderList(searcher, result);
    }

    public OAIDataList<Record> searchRecord(String resumptionToken) throws BadResumptionTokenException {
        String searchId = getSearchId(resumptionToken);
        String tokenCursor = getTokenCursor(resumptionToken);
        MCROAISearcher searcher = this.resultMap.get(searchId);
        if (searcher == null || tokenCursor == null || tokenCursor.length() <= 0) {
            throw new BadResumptionTokenException(resumptionToken);
        }
        MCROAIResult result = searcher.query(tokenCursor);
        return getRecordList(searcher, result);
    }

    public OAIDataList<Header> searchHeader(MetadataFormat format, MCRSet set, Instant from, Instant until) {
        MCROAISearcher searcher = getSearcher(this.identify, format, getPartitionSize(), setManager, objManager);
        this.resultMap.put(searcher.getID(), searcher);
        MCROAIResult result = searcher.query(set, from, until);
        return getHeaderList(searcher, result);
    }

    public OAIDataList<Record> searchRecord(MetadataFormat format, MCRSet set, Instant from, Instant until) {
        MCROAISearcher searcher = getSearcher(this.identify, format, getPartitionSize(), setManager, objManager);
        this.resultMap.put(searcher.getID(), searcher);
        MCROAIResult result = searcher.query(set, from, until);
        return getRecordList(searcher, result);
    }

    protected OAIDataList<Record> getRecordList(MCROAISearcher searcher, MCROAIResult result) {
        OAIDataList<Record> recordList = runListRecordsParallel ? getRecordListParallel(searcher, result)
            : getRecordListSequential(searcher, result);
        this.setResumptionToken(recordList, searcher, result);
        return recordList;
    }

    private OAIDataList<Record> getRecordListSequential(MCROAISearcher searcher, MCROAIResult result) {
        OAIDataList<Record> recordList = new OAIDataList<>();
        result.list().forEach(header -> {
            Record record = this.objManager.getRecord(header, searcher.getMetadataFormat());
            if (record != null) {
                recordList.add(record);
            }
        });
        return recordList;
    }

    private OAIDataList<Record> getRecordListParallel(MCROAISearcher searcher, MCROAIResult result) {
        List<Header> headerList = result.list();
        int listSize = headerList.size();
        Record[] records = new Record[listSize];
        @SuppressWarnings("rawtypes")
        CompletableFuture[] futures = new CompletableFuture[listSize];
        MetadataFormat metadataFormat = searcher.getMetadataFormat();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        for (int i = 0; i < listSize; i++) {
            Header header = headerList.get(i);
            int resultIndex = i;
            MCRTransactionableRunnable r = new MCRTransactionableRunnable(() -> {
                Record record = this.objManager.getRecord(header, metadataFormat);
                records[resultIndex] = record;
            }, mcrSession);
            CompletableFuture<Void> future = CompletableFuture.runAsync(r, executorService);
            futures[i] = future;
        }
        CompletableFuture.allOf(futures).join();
        OAIDataList<Record> recordList = new OAIDataList<>();
        recordList.addAll(Arrays.asList(records));
        return recordList;
    }

    protected OAIDataList<Header> getHeaderList(MCROAISearcher searcher, MCROAIResult result) {
        OAIDataList<Header> headerList = new OAIDataList<>();
        headerList.addAll(result.list());
        this.setResumptionToken(headerList, searcher, result);
        return headerList;
    }

    public String getSearchId(String token) throws BadResumptionTokenException {
        try {
            return token.split(TOKEN_DELIMITER)[0];
        } catch (Exception exc) {
            throw new BadResumptionTokenException(token);
        }
    }

    public String getTokenCursor(String token) throws BadResumptionTokenException {
        try {
            String[] tokenParts = token.split(TOKEN_DELIMITER);
            return tokenParts[tokenParts.length - 1];
        } catch (Exception exc) {
            throw new BadResumptionTokenException(token);
        }
    }

    protected void setResumptionToken(OAIDataList<?> dataList, MCROAISearcher searcher, MCROAIResult result) {
        result.nextCursor().map(cursor -> {
            DefaultResumptionToken rsToken = new DefaultResumptionToken();
            rsToken.setToken(searcher.getID() + TOKEN_DELIMITER + cursor);
            rsToken.setCompleteListSize(result.getNumHits());
            rsToken.setExpirationDate(searcher.getExpirationTime());
            return rsToken;
        }).ifPresent(dataList::setResumptionToken);
    }

    public int getPartitionSize() {
        return partitionSize;
    }

    protected static MCRConfiguration getConfig() {
        return MCRConfiguration.instance();
    }

    public static MCROAISearcher getSearcher(MCROAIIdentify identify, MetadataFormat format, int partitionSize,
        MCROAISetManager setManager, MCROAIObjectManager objectManager) {
        String className = identify.getConfigPrefix() + "Searcher";
        String defaultClass = MCROAICombinedSearcher.class.getName();
        MCROAISearcher searcher = getConfig().<MCROAISearcher> getInstanceOf(className, defaultClass);
        searcher.init(identify, format, MAX_AGE, partitionSize, setManager, objectManager);
        return searcher;
    }

    @Override
    protected void finalize() throws Throwable {
        if (executorService != null && !executorService.isShutdown()) {
            MCRShutdownHandler.getInstance().removeCloseable(executorService::shutdownNow);
            executorService.shutdownNow();
        }
        super.finalize();
    }

}
