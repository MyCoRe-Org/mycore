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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRJobQueue extends AbstractQueue<MCRJob> implements Closeable {
    private static Logger LOGGER = LogManager.getLogger(MCRJobQueue.class);

    protected static Map<String, MCRJobQueue> INSTANCES = new ConcurrentHashMap<>();

    protected static String CONFIG_PREFIX = "MCR.QueuedJob.";

    protected static boolean singleQueue = MCRConfiguration.instance().getBoolean(CONFIG_PREFIX + "SingleQueue", true);

    protected String CONFIG_PREFIX_ADD = "";

    private Class<? extends MCRJobAction> action;

    private Queue<MCRJob> preFetch;

    private ScheduledExecutorService StalledJobScheduler;

    private final ReentrantLock pollLock;

    private boolean running;

    private MCRJobQueue(Class<? extends MCRJobAction> action) {
        int waitTime = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "TimeTillReset", 10);
        if (!singleQueue && action != null) {
            this.action = action;
            CONFIG_PREFIX_ADD = action.getSimpleName();
            if (CONFIG_PREFIX_ADD.length() > 0) {
                CONFIG_PREFIX_ADD = CONFIG_PREFIX_ADD.concat(".");
            }
            waitTime = MCRConfiguration.instance()
                .getInt(CONFIG_PREFIX + CONFIG_PREFIX_ADD + "TimeTillReset", waitTime);
        }
        waitTime = waitTime * 60;

        StalledJobScheduler = Executors.newSingleThreadScheduledExecutor();
        StalledJobScheduler.scheduleAtFixedRate(MCRStalledJobResetter.getInstance(this.action), waitTime, waitTime,
            TimeUnit.SECONDS);
        preFetch = new ConcurrentLinkedQueue<>();
        running = true;
        pollLock = new ReentrantLock();
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    /**
     * Returns an singleton instance of this class.
     *
     * @param action the {@link MCRJobAction} or <code>null</code>
     * @return singleton instance of this class
     */
    public static MCRJobQueue getInstance(Class<? extends MCRJobAction> action) {
        String key = action != null && !singleQueue ? action.getName() : "single";
        MCRJobQueue queue = INSTANCES.computeIfAbsent(key, k -> new MCRJobQueue(singleQueue ? null : action));

        if (!queue.running) {
            return null;
        }
        return queue;
    }

    /**
     * @return next available job instance
     */
    @Override
    public MCRJob poll() {
        if (!running) {
            return null;
        }
        try {
            pollLock.lock();
            MCRJob job = getElement();
            if (job != null) {
                job.setStart(new Date(System.currentTimeMillis()));
                job.setStatus(MCRJobStatus.PROCESSING);
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
        return getElement();
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
     * adds {@link MCRJob} to queue and starts {@link MCRJobMaster} if
     * <code>"MCR.QueuedJob.autostart"</code> is set <code>true</code>.
     * alters date added to current time and status of job to {@link MCRJobStatus#NEW}
     */
    @Override
    public boolean offer(MCRJob job) {
        if (!running) {
            return false;
        }

        if (job.getAction() == null && action != null) {
            job.setAction(action);
        }

        MCRJob oldJob = getJob(job.getAction(), job.getParameters());
        if (oldJob != null) {
            job = oldJob;
        } else {
            job.setAdded(new Date());
        }
        job.setStatus(MCRJobStatus.NEW);
        job.setStart(null);
        if ((job.getId() == 0 && addJob(job)) || (updateJob(job))) {
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
        if (!running) {
            return;
        }

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        StringBuilder sb = new StringBuilder("DELETE FROM MCRJob");
        if (action != null) {
            sb.append(" WHERE action='").append(action.getName()).append('\'');
        }

        Query query = em.createQuery(sb.toString());
        query.executeUpdate();
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
     */
    public Iterator<MCRJob> iterator(MCRJobStatus status) {
        if (!running) {
            List<MCRJob> empty = Collections.emptyList();
            return empty.iterator();
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRJob> cq = cb.createQuery(MCRJob.class);
        Root<MCRJob> root = cq.from(MCRJob.class);

        List<Predicate> predicates = new ArrayList<>();
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (action != null) {
            predicates.add(cb.equal(root.get("action"), action));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[] {})));
        cq.orderBy(cb.asc(root.get("added")));
        cq.distinct(true);

        TypedQuery<MCRJob> query = em.createQuery(cq);

        return query.getResultList().iterator();
    }

    /**
     * returns the current size of this queue
     */
    @Override
    public int size() {
        if (!running) {
            return 0;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        StringBuilder sb = new StringBuilder("SELECT count(*) FROM MCRJob WHERE ");
        if (action != null) {
            sb.append("action='").append(action.getName()).append("' AND ");
        }
        sb.append("status='" + MCRJobStatus.NEW + "'");

        return em.createQuery(sb.toString(), Number.class).getSingleResult().intValue();
    }

    /**
     * get the specific job and alters it status to {@link MCRJobStatus#PROCESSING}
     *
     * @param action the {@link MCRJobAction}
     */
    public MCRJob getElementOutOfOrder(Class<? extends MCRJobAction> action, Map<String, String> params)
        throws NoSuchElementException {
        if (!running) {
            return null;
        }
        MCRJob job = getJob(action, params);
        if (job == null) {
            return null;
        }
        job.setStart(new Date(System.currentTimeMillis()));
        job.setStatus(MCRJobStatus.PROCESSING);
        if (!updateJob(job)) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * returns a specific job from given parameters or null if not found.
     *
     * @param params the parameters
     * @return the job
     */
    public MCRJob getJob(Map<String, String> params) {
        return getJob(action, params);
    }

    private MCRJob getJob(Class<? extends MCRJobAction> action, Map<String, String> params) {
        if (!running) {
            return null;
        }

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        StringBuilder qStr = new StringBuilder("FROM MCRJob job JOIN FETCH job.parameters WHERE action = '"
            + action.getName() + "' ");
        for (String paramKey : params.keySet()) {
            qStr.append(" AND job.parameters['")
                .append(paramKey)
                .append("'] = '")
                .append(params.get(paramKey))
                .append('\'');
        }

        TypedQuery<MCRJob> query = em.createQuery(qStr.toString(), MCRJob.class);

        try {
            MCRJob job = query.getSingleResult();
            clearPreFetch();
            return job;
        } catch (NoResultException e) {
            return null;
        }
    }

    private MCRJob getElement() {
        if (!running) {
            return null;
        }
        MCRJob job = getNextPrefetchedElement();
        if (job != null) {
            return job;
        }
        LOGGER.debug("No prefetched jobs available");
        if (preFetch(MCRConfiguration.instance().getInt(CONFIG_PREFIX + "preFetchAmount", 50)) == 0) {
            return null;
        }
        return getNextPrefetchedElement();
    }

    private MCRJob getNextPrefetchedElement() {
        MCRJob job = preFetch.poll();
        LOGGER.debug("Fetched job: {}", job);
        return job;
    }

    private int preFetch(int amount) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRJob> cq = cb.createQuery(MCRJob.class);
        Root<MCRJob> root = cq.from(MCRJob.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), MCRJobStatus.NEW));
        if (action != null) {
            predicates.add(cb.equal(root.get("action"), action));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[] {})));
        cq.orderBy(cb.asc(root.get("added")));
        cq.distinct(true);

        TypedQuery<MCRJob> query = em.createQuery(cq);
        query.setMaxResults(amount);

        List<MCRJob> jobs = query.getResultList();

        int i = 0;
        for (MCRJob job : jobs) {
            if (job.getParameters().isEmpty()) {
                continue;
            }

            i++;
            preFetch.add(job.clone());
            em.detach(job);
        }
        LOGGER.debug("prefetched {} jobs", i);
        return i;
    }

    private void clearPreFetch() {
        preFetch.clear();
    }

    private boolean updateJob(MCRJob job) {
        if (!running) {
            return false;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.merge(job);
        return true;
    }

    private boolean addJob(MCRJob job) {
        if (!running) {
            return false;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(job);
        return true;
    }

    /**
     * every attached listener is informed that something happened to the state of the queue.
     * Starts {@link MCRJobMaster} if <code>"MCR.QueuedJob.autostart"</code> is set <code>true</code>.
     */
    public synchronized void notifyListener() {
        this.notifyAll();

        boolean autostart = MCRConfiguration.instance().getBoolean(CONFIG_PREFIX + "autostart", true);
        autostart = MCRConfiguration.instance().getBoolean(CONFIG_PREFIX + CONFIG_PREFIX_ADD + "autostart", autostart);

        if (autostart) {
            MCRJobMaster.startMasterThread(action);
        }
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

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        StringBuilder qStr = new StringBuilder("FROM MCRJob job WHERE action = '" + action.getName() + "' ");
        for (String paramKey : params.keySet()) {
            qStr.append(" AND job.parameters['")
                .append(paramKey)
                .append("'] = '")
                .append(params.get(paramKey))
                .append('\'');
        }

        Query query = em.createQuery(qStr.toString());

        @SuppressWarnings("unchecked")
        Iterator<MCRJob> results = query.getResultList().iterator();
        if (!results.hasNext()) {
            return 0;
        }

        MCRJob job = results.next();

        try {
            em.remove(job);
            em.detach(job);
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
    public int remove(Class<? extends MCRJobAction> action) {
        if (!running) {
            return 0;
        }

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();

        Query query = em.createQuery("FROM MCRJob job WHERE action = '" + action.getName() + "'");

        @SuppressWarnings("unchecked")
        Iterator<MCRJob> results = query.getResultList().iterator();
        if (!results.hasNext()) {
            return 0;
        }
        try {
            int delC = 0;
            while (results.hasNext()) {
                MCRJob job = results.next();

                em.remove(job);
                em.detach(job);
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
    @Override
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
    @Override
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
