package org.mycore.oai;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.deleteditems.MCRDeletedItemManager;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.Set;

public abstract class MCROAISearcher {

    protected final static Logger LOGGER = LogManager.getLogger(MCROAISearcher.class);

    protected MetadataFormat metadataFormat;

    protected Date expirationDate;

    /** The unique ID of this result set */
    protected final String id;

    /**
     * increase every time a {@link #query(int)} is called
     */
    protected long runningExpirationTimer;

    protected DeletedRecordPolicy deletedRecordPolicy;

    protected String configPrefix;

    protected int partitionSize;

    protected MCROAISetManager setManager;

    public MCROAISearcher() {
        Random random = new Random(System.currentTimeMillis());
        this.id = Long.toString(random.nextLong(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    public void init(String configPrefix, MetadataFormat format, Date expirationDate,
        DeletedRecordPolicy deletedRecordPolicy, int partitionSize, MCROAISetManager setManager) {
        this.configPrefix = configPrefix;
        this.metadataFormat = format;
        this.expirationDate = expirationDate;
        this.deletedRecordPolicy = deletedRecordPolicy;
        this.partitionSize = partitionSize;
        this.setManager = setManager;
        updateRunningExpirationTimer();
    }

    public abstract MCROAIResult query(int cursor);

    public abstract MCROAIResult query(Set set, Date from, Date until);

    public abstract Date getEarliestTimestamp();

    boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        return (new Date(currentTime).compareTo(expirationDate) > 0) && currentTime > runningExpirationTimer;
    }

    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    public String getID() {
        return id;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public MCROAISetManager getSetManager() {
        return setManager;
    }

    /**
     * Set the running expiration timer.
     * 
     * TODO: make this configurable (currently five minutes in future)
     */
    protected void updateRunningExpirationTimer() {
        this.runningExpirationTimer = System.currentTimeMillis() + (1000 * 60 * 5);
    }

    protected MCRConfiguration getConfig() {
        return MCRConfiguration.instance();
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public DeletedRecordPolicy getDeletedRecordPolicy() {
        return deletedRecordPolicy;
    }

    public int getPartitionSize() {
        return partitionSize;
    }

    /**
     * Returns a list with identifiers of the deleted objects within the given date boundary. If the record policy indicates that there is not support for
     * tracking deleted an empty list is returned.
     * 
     * @return a list with identifiers of the deleted objects
     */
    protected List<String> searchDeleted(Date from, Date until) {
        if (from == null || DeletedRecordPolicy.No.equals(getDeletedRecordPolicy())
            || DeletedRecordPolicy.Transient.equals(getDeletedRecordPolicy())) {
            return new ArrayList<>();
        }
        LOGGER.info("Getting identifiers of deleted items");
        try {
            // building the query
            return MCRDeletedItemManager.getDeletedItems(ZonedDateTime.from(from.toInstant()),
                Optional.ofNullable(until).map(Date::toInstant).map(ZonedDateTime::from));
        } catch (Exception ex) {
            LOGGER.warn("Could not retrieve identifiers of deleted objects", ex);
        }
        return new ArrayList<>();
    }

}
