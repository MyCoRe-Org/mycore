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

package org.mycore.services.queuedjob;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mycore.common.MCRException;

/**
 * A queue for {@link MCRJob} instances. Provides queue logic for {@link MCRJobDAO}.
 * @author Sebastian Hofmann
 */
public class MCRJobQueue extends AbstractQueue<MCRJob> implements EventListener {
    private static final Logger LOGGER = LogManager.getLogger(MCRJobQueue.class);

    private final Class<? extends MCRJobAction> action;

    private final ReentrantLock pollLock;
    private final MCRJobConfig config;
    private final MCRJobDAO dao;
    private final List<MCRJobQueueEventListener> listeners;
    private boolean running;

    /**
     * Creates a new JobQueue instance.
     * @param action the {@link MCRJobAction} the action this queue is responsible for
     * @param config the {@link MCRJobConfig} which is used to determine if the queue is running
     * @param dao the {@link MCRJobDAO} which is used to retrieve jobs
     */
    protected MCRJobQueue(Class<? extends MCRJobAction> action, MCRJobConfig config, MCRJobDAO dao) {
        this.config = config;
        this.dao = dao;
        this.action = action;
        running = config.activated(action).orElseGet(config::activated);
        pollLock = new ReentrantLock();
        listeners = new LinkedList<>();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @param action the {@link MCRJobAction} or <code>null</code>
     * @return singleton instance of this class
     * @deprecated use {@link MCRJobQueueManager#getInstance()} and {@link MCRJobQueueManager#getJobQueue(Class)}
     * instead
     */
    @Deprecated
    public static MCRJobQueue getInstance(Class<? extends MCRJobAction> action) {
        return MCRJobQueueManager.getInstance().getJobQueue(action);
    }

    /**
     * @return next available job instance
     */
    @Override
    public MCRJob poll() {
        if (!running || !config.activated(action).orElseGet(config::activated)) {
            return null;
        }
        try {
            pollLock.lock();
            MCRJob job = dao.getNextJobs(action, 1).stream().findFirst().orElse(null);
            if (job != null) {
                job.setStart(new Date(System.currentTimeMillis()));
                job.setStatus(MCRJobStatus.PROCESSING);
                if (!dao.updateJob(job)) {
                    throw new MCRException("Could not update job " + job.getId() + " to status PROCESSING");
                }
                LOGGER.info("Receive job {} from DAO", job);
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
        if (!running) {
            return null;
        }
        MCRJob job = poll();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * get next job without modifying it state to {@link MCRJobStatus#PROCESSING}
     * @return the next job
     */
    @Override
    public MCRJob peek() {
        if (!running) {
            return null;
        }
        return dao.getNextJobs(action, 1).stream().findFirst().orElse(null);
    }

    /**
     * removes next job.
     * same as {@link #peek()} but never returns null
     * @throws NoSuchElementException if {@link #peek()} would return null
     */
    @Override
    public MCRJob element() throws NoSuchElementException {
        if (!running) {
            return null;
        }
        MCRJob job = peek();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * adds {@link MCRJob} to queue and starts {@link MCRJobThreadStarter} if the queue is activated.
     * alters date added to current time and status of job to {@link MCRJobStatus#NEW}
     * @param job the job to add
     * @return <code>true</code> if job was added
     * @throws IllegalArgumentException if job action does not match queue action
     */
    @Override
    public boolean offer(MCRJob job) {
        if (!running) {
            return false;
        }

        if (job.getAction() == null) {
            job.setAction(action);
        } else if (!job.getAction().equals(action)) {
            throw new IllegalArgumentException("Job action " + job.getAction() + " does not match queue action "
                + action);
        }

        boolean added;

        job.setAdded(new Date());
        job.setStatus(MCRJobStatus.NEW);
        job.setStart(null);

        if (job.getId() == null) {
            LOGGER.info("Adding job {} to queue {}.", job.toString(), action.getName());
            added = dao.addJob(job);
        } else {
            LOGGER.info("Update job {} in queue {}.", job.toString(), action.getName());
            added = dao.updateJob(job);
        }

        if (added) {
            this.listeners.forEach(l -> {
                l.onJobAdded(job);
            });
        }

        return added;
    }

    /**
     * Deletes all jobs no matter what the current state is.
     */
    @Override
    public void clear() {
        if (!running) {
            return;
        }

        LOGGER.info("Clearing queue {}.", action.getName());
        dao.removeJobs(action, Collections.emptyMap(), List.of(MCRJobStatus.NEW));
    }

    /**
     * iterates over jobs of status {@link MCRJobStatus#NEW}
     *
     * does not change the status.
     */
    @Override
    public Iterator<MCRJob> iterator() {
        return iterator(MCRJobStatus.NEW);
    }

    /**
     * Builds iterator for jobs with given {@link MCRJobStatus} or <code>null</code> for all jobs.
     * @param status the status or <code>null</code>
     * @return the iterator
     */
    public Iterator<MCRJob> iterator(MCRJobStatus status) {
        if (!running) {
            return Collections.emptyIterator();
        }
        List<MCRJob> jobs
            = dao.getJobs(action, Collections.emptyMap(), Stream.ofNullable(status).collect(Collectors.toList()), null,
                null);

        return jobs.listIterator();
    }

    /**
     * returns the current size of this queue
     */
    @Override
    public int size() {
        if (!running) {
            return 0;
        }
        return dao.getRemainingJobCount(action);
    }

    /**
     * Returns a specific job from given parameters or null if not found.
     *
     * @param params the parameters
     * @return the job
     * @deprecated use {@link MCRJobDAO#getJobs(Class, Map, List, Integer, Integer)} instead.
     *      * The job queue should only be used for {@link MCRJobStatus#NEW} jobs, but this method returns all jobs.
     */
    @Deprecated
    public MCRJob getJob(Map<String, String> params) {
        if (!running) {
            return null;
        }

        return dao.getJob(action, params, Collections.emptyList());
    }

    /**
     * Returns specific jobs by the given parameters or an empty list.
     *
     * @param params the parameters
     * @return a list of jobs matching the given parameters
     * @deprecated use {@link MCRJobDAO#getJobs(Class, Map, List, Integer, Integer)} instead.
     * The job queue should only be used for {@link MCRJobStatus#NEW} jobs, but this method returns all jobs.
     */
    @Deprecated
    public List<MCRJob> getJobs(Map<String, String> params) {
        return getJobs(action, params);
    }

    /**
     * Returns specific jobs by the given parameters or an empty list.
     *
     * @param action the action class
     * @param params the parameters
     *
     * @return a list of jobs matching the given parameters
     * @deprecated use {@link MCRJobDAO#getJobs(Class, Map, List, Integer, Integer)} instead
     * The job queue should only be used for {@link MCRJobStatus#NEW} jobs, but this method returns all jobs.
     */
    @Deprecated
    private List<MCRJob> getJobs(Class<? extends MCRJobAction> action, Map<String, String> params) {
        if (!running) {
            return null;
        }
        return dao.getJobs(action, params, Collections.emptyList(), null, null);
    }

    /**
     * removes specific job from queue no matter what its current status is.
     *
     * @param action - the action class
     * @param params - parameters to get jobs
     * @return the number of jobs deleted
     */
    public int remove(Class<? extends MCRJobAction> action, Map<String, String> params) {
        if (!running) {
            return 0;
        }

        return dao.removeJobs(action, params, Collections.emptyList());
    }

    /**
     * Removes all jobs from queue of specified action.
     *
     * @param action - the action class
     * @return the number of jobs deleted
     */
    public int remove(Class<? extends MCRJobAction> action) {
        if (!running) {
            return 0;
        }

        return dao.removeJobs(action, Collections.emptyMap(), Collections.emptyList());
    }

    /**
     * @return "MCRJobQueue"
     */
    @Override
    public String toString() {
        return "MCRJobQueue for " + action.getName();
    }

    /**
     * @return true if the queue is running and jobs can be added or removed
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running if true, the queue is running, otherwise it is stopped and no jobs can be added or removed
     */
    protected void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Adds a listener to this queue. The listener will be notified when a job is added.
     * @param listener the listener to add
     */
    public void addListener(MCRJobQueueEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener to this queue. The listener will not be notified anymore.
     * @param listener the listener to remove
     */
    public void removeListener(MCRJobQueueEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns the action class of this queue.
     * @return the action class
     */
    public Class<? extends MCRJobAction> getAction() {
        return action;
    }
}
