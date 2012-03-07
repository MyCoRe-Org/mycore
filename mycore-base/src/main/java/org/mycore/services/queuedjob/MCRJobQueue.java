/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.queuedjob;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRJobQueue extends AbstractQueue<MCRJob> implements Closeable {
    private static Logger LOGGER = Logger.getLogger(MCRJobQueue.class);

    private static MCRJobQueue INSTANCE = null;

    private static Queue<MCRJob> preFetch;

    //TODO must be static after migration of MCRTilingQueue
    private ScheduledExecutorService StalledJobScheduler;

    private final ReentrantLock pollLock;

    private boolean running;

    private MCRJobQueue() {
        int waitTime = MCRConfiguration.instance().getInt(MCRJobMaster.CONFIG_PREFIX + "TimeTillReset", 10) * 60;
        StalledJobScheduler = Executors.newSingleThreadScheduledExecutor();
        StalledJobScheduler.scheduleAtFixedRate(MCRStalledJobResetter.getInstance(), waitTime, waitTime, TimeUnit.SECONDS);
        preFetch = new ConcurrentLinkedQueue<MCRJob>();
        running = true;
        pollLock = new ReentrantLock();
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    /**
     * @return singleton instance of this class
     */
    public static MCRJobQueue getInstance() {
        if (INSTANCE == null)
            INSTANCE = new MCRJobQueue();

        return INSTANCE;
    }

    /**
     * @return next available job instance
     */
    public MCRJob poll() {
        if (!running)
            return null;
        try {
            pollLock.lock();
            MCRJob job = getElement();
            if (job != null) {
                job.setStart(new Date(System.currentTimeMillis()));
                job.setStatus(MCRJob.Status.PROCESSING);
                if (!updateJob(job)) {
                    job = null;
                }
            }
            return job;
        } finally {
            pollLock.unlock();
        }
    }

    /**
     * removes next job.
     * same as {@link #poll()} but never returns null
     * @throws NoSuchElementException if {@link #poll()} would return null
     */
    @Override
    public MCRJob remove() throws NoSuchElementException {
        if (!running)
            return null;
        MCRJob job = poll();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * get next job without modifying it state to {@link MCRJob.Status#PROCESSING} 
     * @return the next job
     */
    public MCRJob peek() {
        if (!running)
            return null;
        MCRJob job = getElement();
        return job;
    }

    /**
     * removes next job.
     * same as {@link #peek()} but never returns null
     * @throws NoSuchElementException if {@link #peek()} would return null
     */
    @Override
    public MCRJob element() throws NoSuchElementException {
        if (!running)
            return null;
        MCRJob job = peek();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * adds {@link MCRJob} to queue.
     * alters date added to current time and status of job to {@link MCRJob.Status#NEW}
     */
    public boolean offer(MCRJob job) {
        if (!running)
            return false;
        MCRJob oldJob = getJob(job.getAction().getName(), job.getParameters());
        if (oldJob != null) {
            job = oldJob;
        } else {
            job.setAdded(new Date());
        }
        job.setStatus(MCRJob.Status.NEW);
        job.setStart(null);
        if (addJob(job)) {
            notifyListener();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Deletes all jobs no matter what the current state is.
     */
    @Override
    public void clear() {
        if (!running)
            return;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM MCRJob");
        query.executeUpdate();
    }

    /**
     * iterates of jobs of status {@link MCRJob.Status#NEW}
     * 
     * does not change the status.
     */
    @Override
    public Iterator<MCRJob> iterator() {
        if (!running) {
            List<MCRJob> empty = Collections.emptyList();
            return empty.iterator();
        }
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRJob WHERE status='" + MCRJob.Status.NEW + "' ORDER BY added ASC");
        @SuppressWarnings("unchecked")
        List<MCRJob> result = query.list();
        return result.iterator();
    }

    /**
     * returns the current size of this queue
     */
    @Override
    public int size() {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("SELECT count(*) FROM MCRJob WHERE status='" + MCRJob.Status.NEW + "'");
        return ((Number) query.iterate().next()).intValue();
    }

    /**
     * get the specific job and alters it status to {@link MCRJob.Status#PROCESSING}
     * 
     * @param params
     * @return
     * @throws NoSuchElementException
     */
    public MCRJob getElementOutOfOrder(String action, Map<String, String> params) throws NoSuchElementException {
        if (!running)
            return null;
        MCRJob job = getJob(action, params);
        if (job == null)
            return null;
        job.setStart(new Date(System.currentTimeMillis()));
        job.setStatus(MCRJob.Status.PROCESSING);
        if (!updateJob(job)) {
            throw new NoSuchElementException();
        }
        return job;
    }

    @SuppressWarnings("unchecked")
    private MCRJob getJob(String action, Map<String, String> params) {
        if (!running)
            return null;

        Session session = MCRHIBConnection.instance().getSession();

        StringBuffer qStr = new StringBuffer("SELECT job FROM MCRJob job WHERE action = '" + action + "' ");
        for (String paramKey : params.keySet()) {
            qStr.append(" AND job.parameters['" + paramKey + "'] = '" + params.get(paramKey) + "'");
        }

        Query query = session.createQuery(qStr.toString());

        Iterator<MCRJob> results = query.iterate();
        if (!results.hasNext())
            return null;

        MCRJob job = results.next();

        clearPreFetch();
        return job;
    }

    private MCRJob getElement() {
        if (!running)
            return null;
        MCRJob job = getNextPrefetchedElement();
        if (job != null) {
            return job;
        }
        LOGGER.debug("No prefetched jobs available");
        if (preFetch(100) == 0) {
            return null;
        }
        return getNextPrefetchedElement();
    }

    private MCRJob getNextPrefetchedElement() {
        MCRJob job = preFetch.poll();
        LOGGER.debug("Fetched job: " + job);
        return job;
    }

    private int preFetch(int amount) {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRJob WHERE status='" + MCRJob.Status.NEW + "' ORDER BY added ASC").setMaxResults(amount);

        @SuppressWarnings("unchecked")
        Iterator<MCRJob> queryResult = query.iterate();
        int i = 0;
        while (queryResult.hasNext()) {
            i++;
            MCRJob job = queryResult.next();
            preFetch.add(job.clone());
            session.evict(job);
        }
        LOGGER.debug("prefetched " + i + " tile jobs");
        return i;
    }

    private void clearPreFetch() {
        preFetch.clear();
    }

    private boolean updateJob(MCRJob job) {
        if (!running)
            return false;
        Session session = MCRHIBConnection.instance().getSession();
        session.update(job);
        return true;
    }

    private boolean addJob(MCRJob job) {
        if (!running)
            return false;
        Session session = MCRHIBConnection.instance().getSession();
        session.save(job);
        return true;
    }

    /**
     * every attached listener is informed that something happened to the state of the queue.
     */
    public synchronized void notifyListener() {
        this.notifyAll();
    }

    /**
     * removes specific job from queue no matter what its current status is.
     * 
     * @param action - the action class
     * @param params - parameters to get jobs
     * @return the number of jobs deleted
     */
    public int remove(String action, Map<String, String> params) {
        if (!running)
            return 0;

        Session session = MCRHIBConnection.instance().getSession();

        StringBuffer qStr = new StringBuffer("DELETE FROM MCRJob job WHERE action = '" + action + "' ");
        for (String paramKey : params.keySet()) {
            qStr.append(" AND job.parameters['" + paramKey + "'] = '" + params.get(paramKey) + "'");
        }

        Query query = session.createQuery(qStr.toString());

        @SuppressWarnings("unchecked")
        Iterator<MCRJob> results = query.iterate();
        if (!results.hasNext())
            return 0;

        MCRJob job = results.next();

        try {
            session.delete(job);
            return 1;
        } finally {
            clearPreFetch();
        }
    }
    
    /**
     * Removes all jobs from queue of specified action.
     * 
     * @param action - the action class
     * @return the number of jobs deleted
     */
    public int remove(String action) {
        if (!running)
            return 0;

        Session session = MCRHIBConnection.instance().getSession();

        Query query = session.createQuery("DELETE FROM MCRJob job WHERE action = '" + action + "'");

        @SuppressWarnings("unchecked")
        Iterator<MCRJob> results = query.iterate();
        if (!results.hasNext())
            return 0;
        try {
            int delC = 0;
            while (results.hasNext()) {
                MCRJob job = results.next();
                session.delete(job);
                delC++;
            }
            return delC;
        } finally {
            clearPreFetch();
        }
    }

    /**
     * Shuts down {@link MCRStalledJobResetter} and does not alter any job anymore.
     */
    public void prepareClose() {
        StalledJobScheduler.shutdownNow();
        running = false;
        try {
            StalledJobScheduler.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.info("Could not wait for 60 seconds...");
            StalledJobScheduler.shutdownNow();
        }
    }

    /**
     * does nothing
     */
    public void close() {
        //nothing to be done in this phase
    }

    /**
     * @return "MCRJobQueue"
     */
    @Override
    public String toString() {
        return "MCRJobQueue";
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY;
    }
}
