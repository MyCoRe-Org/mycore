/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

    protected static final Logger LOGGER = LogManager.getLogger(MCROAISearcher.class);

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

    /**
     * Returns the earliest created/modified record time stamp. If the earliest time stamp cannot be retrieved an
     * empty optional is returned.
     *
     * @return the earliest created/modified time stamp
     */
    public abstract Optional<Instant> getEarliestTimestamp();

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
