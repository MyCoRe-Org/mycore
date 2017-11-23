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

package org.mycore.solr.index.statistic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrIndexStatistic {

    private static final Logger LOGGER = LogManager.getLogger();

    final AtomicInteger documents;

    final AtomicLong accumulatedTime;

    String name;

    public MCRSolrIndexStatistic(String name) {
        this.name = name;
        this.documents = new AtomicInteger();
        this.accumulatedTime = new AtomicLong();
    }

    public long addTime(long time) {
        LOGGER.debug("{}: adding {} ms", name, time);
        return accumulatedTime.addAndGet(time);
    }

    public int addDocument(int docs) {
        LOGGER.debug("{}: adding {} documents", name, documents);
        return documents.addAndGet(docs);
    }

    public long getAccumulatedTime() {
        return accumulatedTime.get();
    }

    public int getDocuments() {
        return documents.get();
    }

    /**
     * resets statistic and returns average time in ms per document.
     */
    public synchronized double reset() {
        synchronized (accumulatedTime) {
            synchronized (documents) {
                long time = accumulatedTime.getAndSet(0);
                int docs = documents.getAndSet(0);
                if (docs == 0) {
                    return time == 0 ? 0 : Double.MAX_VALUE;
                }
                return ((double) time / (double) docs);
            }
        }
    }
}
