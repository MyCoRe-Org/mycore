/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 15, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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

    AtomicInteger documents;

    AtomicLong accumulatedTime;

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
