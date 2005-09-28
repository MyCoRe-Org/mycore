/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.services.query;

import java.util.LinkedList;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * The agent find results for a query and informs the MCRQueryCollector about
 * it.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRQueryAgent {
    private int nThreads;

    private PoolWorker[] threads;

    private LinkedList queue;

    private MCRConfiguration conf;

    private static int vec_max_length;

    public MCRQueryAgent(int nThreads, MCRConfiguration conf) {
        this.nThreads = nThreads;
        queue = new LinkedList();
        threads = new PoolWorker[nThreads];
        this.conf = conf;
        vec_max_length = conf.getInt("MCR.query_max_results", 10);

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new PoolWorker();
            threads[i].setName("MCRQueryAgent #" + (i + 1));
            threads[i].start();
        }
    }

    /**
     * add a query to the agent mission queue.
     * 
     * @param host
     *            host to be queried
     * @param type
     *            type of the query
     * @param query
     *            the query
     * @param result
     *            the MCRXMLContainer collecting results
     * @param tc
     *            inner Class of MCRQueryCollector
     */
    public void add(String host, String type, String query, MCRXMLContainer result, MCRQueryCollector.ThreadCounter tc) {
        Mission m = new Mission(host, type, query, result, tc);

        synchronized (queue) {
            queue.addLast(m);
            queue.notify();
        }
    }

    private class Mission {
        private String host;

        private String type;

        private String query;

        private MCRXMLContainer result;

        private MCRQueryCollector.ThreadCounter tc;

        public Mission(String host, String type, String query, MCRXMLContainer result, MCRQueryCollector.ThreadCounter tc) {
            this.host = host;
            this.type = type;
            this.query = query;
            this.result = result;
            this.tc = tc;
        }

        public String getHost() {
            return this.host;
        }

        public String getType() {
            return this.type;
        }

        public String getQuery() {
            return this.query;
        }

        public MCRXMLContainer getResultContainer() {
            return this.result;
        }

        public void accomplished() {
            this.tc.decrease();
        }
    }

    private class PoolWorker extends Thread {
        private MCRQueryInterface mcr_queryint;

        private MCRXMLContainer mcr_result;

        private String mcr_type;

        private String mcr_query;

        private String hostAlias;

        public void run() {
            Mission m;

            while (true) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    m = (Mission) queue.removeFirst();
                }

                // If we don't catch RuntimeException,
                // the pool could leak threads
                try {
                    MCRXMLContainer result = m.getResultContainer();
                    result.importElements(MCRQueryCache.getResultList(m.getHost(), m.getQuery(), m.getType(), vec_max_length));
                    m.accomplished();
                } catch (RuntimeException e) {
                    // to hang not forever mark mission accomplished
                    m.accomplished();
                    throw new MCRException("Error while grabbing resultset: " + e.getMessage(), e);
                }
            }
        }
    }
}
