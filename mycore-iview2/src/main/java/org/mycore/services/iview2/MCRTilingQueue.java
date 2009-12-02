package org.mycore.services.iview2;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRTilingQueue extends AbstractQueue<MCRTileJob> implements Closeable {
    public char colours;

    private static MCRTilingQueue instance = new MCRTilingQueue();

    private static ScheduledExecutorService StalledJobScheduler;

    private static Logger LOGGER = Logger.getLogger(MCRTilingQueue.class);

    private boolean running;

    private MCRTilingQueue() {
        // periodische Ausführung von runProcess
        int waitTime = Integer.parseInt(MCRIview2Props.getProperty("TimeTillReset")) * 60;
        StalledJobScheduler = Executors.newSingleThreadScheduledExecutor();
        StalledJobScheduler.scheduleAtFixedRate(MCRStalledJobResetter.getInstance(), waitTime, waitTime, TimeUnit.SECONDS);
        running = true;
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    public static MCRTilingQueue getInstance() {
        if (!instance.running)
            return null;
        return instance;
    }

    public synchronized MCRTileJob poll() {
        if (!running)
            return null;
        MCRTileJob job = getElement();
        if (job != null) {
            job.setStart(new Date(System.currentTimeMillis()));
            job.setStatus(MCRJobState.PROCESS);
            if (!updateJob(job)) {
                job = null;
            }
        }
        return job;
    }

    @Override
    public MCRTileJob remove() throws NoSuchElementException {
        if (!running)
            return null;
        MCRTileJob job = poll();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    public MCRTileJob peek() {
        if (!running)
            return null;
        MCRTileJob job = getElement();
        return job;
    }

    @Override
    public MCRTileJob element() throws NoSuchElementException {
        if (!running)
            return null;
        MCRTileJob job = peek();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    public boolean offer(MCRTileJob job) {
        if (!running)
            return false;
        MCRTileJob oldJob = getJob(job.getDerivate(), job.getPath());
        if (oldJob != null) {
            job = oldJob;
        } else {
            job.setAdded(new Date());
        }
        job.setStatus(MCRJobState.NEW);
        job.setStart(null);
        if (addJob(job)) {
            notifyListener();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        if (!running)
            return;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM MCRTileJob");
        query.executeUpdate();
    }

    @Override
    public Iterator<MCRTileJob> iterator() {
        if (!running) {
            List<MCRTileJob> empty = Collections.emptyList();
            return empty.iterator();
        }
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' ORDER BY added ASC");
        @SuppressWarnings("unchecked")
        List<MCRTileJob> result = query.list();
        return result.iterator();
    }

    @Override
    public int size() {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("SELECT count(*) FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "'");
        return ((Number) query.iterate().next()).intValue();
    }

    public MCRTileJob getElementOutOfOrder(String derivate, String path) throws NoSuchElementException {
        if (!running)
            return null;
        MCRTileJob job = getJob(derivate, path);
        if (job == null)
            return null;
        job.setStart(new Date(System.currentTimeMillis()));
        job.setStatus(MCRJobState.PROCESS);
        if (!updateJob(job)) {
            throw new NoSuchElementException();
        }
        return job;
    }

    private MCRTileJob getJob(String derivate, String path) {
        if (!running)
            return null;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE  derivate= :derivate AND path = :path");
        query.setParameter("derivate", derivate);
        query.setParameter("path", path);
        @SuppressWarnings("unchecked")
        Iterator<MCRTileJob> results = query.iterate();
        if (!results.hasNext())
            return null;
        MCRTileJob job = results.next();
        return job;
    }

    private MCRTileJob getElement() {
        if (!running)
            return null;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' ORDER BY added ASC")
                .setMaxResults(1);

        if (query.list() == null || query.list().size() != 1) {
            return null;
        }
        return (MCRTileJob) query.iterate().next();
    }

    private boolean updateJob(MCRTileJob job) {
        if (!running)
            return false;
        Session session = MCRHIBConnection.instance().getSession();
        session.update(job);
        return true;
    }

    private boolean addJob(MCRTileJob job) {
        if (!running)
            return false;
        Session session = MCRHIBConnection.instance().getSession();
        session.save(job);
        return true;
    }

    public void notifyListener() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    public int remove(String derivate, String path) {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM " + MCRTileJob.class.getName() + " WHERE derivate = :derivate AND path = :path");
        query.setParameter("derivate", derivate);
        query.setParameter("path", path);
        return query.executeUpdate();
    }

    public int remove(String derivate) {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM " + MCRTileJob.class.getName() + " WHERE derivate = :derivate");
        query.setParameter("derivate", derivate);
        return query.executeUpdate();
    }

    public void prepareClose() {
        StalledJobScheduler.shutdownNow();
        running = false;
        try {
            StalledJobScheduler.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.info("Could not wait for 60 seconds...");
        }
    }

    public void close() {
        //nothing to be done in this phase
    }

    @Override
    public String toString() {
        return "MCRTilingQueue";
    }
}