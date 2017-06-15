package org.mycore.oai;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.set.MCRSet;

/**
 * <p>Base class to query different types of data in the mycore system.
 * Implementations has to implement the query and the earliest time stamp
 * methods. The result of a query is a {@link MCROAIResult}.</p>
 * 
 * <p>Searchers have a unique id and a expiration time which increases
 * every time a query is fired.</p>
 * 
 * @author Matthias Eichner
 */
public abstract class MCROAISearcher {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAISearcher.class);

    protected MCROAIIdentify identify;

    protected MetadataFormat metadataFormat;

    /** The unique ID of this result set */
    protected final String id;

    protected long expire;

    /**
     * Increase every time a {@link #query(String)} is called
     */
    protected long runningExpirationTimer;

    protected int partitionSize;

    protected MCROAISetManager setManager;

    private MCROAIObjectManager objectManager;

    public MCROAISearcher() {
        Random random = new Random(System.currentTimeMillis());
        this.id = Long.toString(random.nextLong(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    public void init(MCROAIIdentify identify, MetadataFormat format, long expire, int partitionSize,
        MCROAISetManager setManager, MCROAIObjectManager objectManager) {
        this.identify = identify;
        this.metadataFormat = format;
        this.expire = expire;
        this.partitionSize = partitionSize;
        this.setManager = setManager;
        this.objectManager = objectManager;
        updateRunningExpirationTimer();
    }

    public abstract Optional<Header> getHeader(String mcrId);

    public abstract MCROAIResult query(String cursor);

    public abstract MCROAIResult query(MCRSet set, Instant from, Instant until);

    public abstract Instant getEarliestTimestamp();

    public boolean isExpired() {
        return System.currentTimeMillis() > runningExpirationTimer;
    }

    public Instant getExpirationTime() {
        return Instant.ofEpochMilli(runningExpirationTimer);
    }

    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    public String getID() {
        return id;
    }

    public MCROAISetManager getSetManager() {
        return setManager;
    }

    public MCROAIObjectManager getObjectManager() {
        return objectManager;
    }

    protected String getConfigPrefix() {
        return this.identify.getConfigPrefix();
    }

    /**
     * Updates the running expiration timer.
     */
    protected void updateRunningExpirationTimer() {
        this.runningExpirationTimer = System.currentTimeMillis() + this.expire;
    }

    protected MCRConfiguration getConfig() {
        return MCRConfiguration.instance();
    }

    public int getPartitionSize() {
        return partitionSize;
    }

}
