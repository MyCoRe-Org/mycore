package org.mycore.services.iview2;

import java.util.AbstractQueue;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;

public class MCRTilingQueue extends AbstractQueue<MCRTileJob> {
    public char colours;

    private static MCRTilingQueue instance = new MCRTilingQueue();

    private static ScheduledExecutorService scheduler;

    private static Logger LOGGER = Logger.getLogger(MCRTilingQueue.class);

    private SessionFactory sessionFactory;

    private MCRTilingQueue() {
        // periodische Ausführung von runProcess
        int waitTime = Integer.parseInt(MCRIview2Props.getProperty("TimeTillReset")) * 60;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        final ScheduledFuture check = scheduler.scheduleAtFixedRate(MCRQueueExecutor.getInstance(), waitTime, waitTime, TimeUnit.SECONDS);
        sessionFactory = MCRHIBConnection.instance().getSessionFactory();
    }

    public static MCRTilingQueue getInstance() {
        return instance;
    }

    public MCRTileJob poll() {
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

    public MCRTileJob remove() throws NoSuchElementException {
        MCRTileJob job = poll();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    public MCRTileJob peek() {
        MCRTileJob job = getElement();
        return job;
    }

    public MCRTileJob element() throws NoSuchElementException {
        MCRTileJob job = peek();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    public boolean offer(MCRTileJob job) {
        job.setAdded(new Date());
        job.setStatus(MCRJobState.NEW);
        job.setStart(null);
        if (addJob(job)) {
            notifyListener();
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM MCRTileJob");
        query.executeUpdate();
    }

    public Iterator<MCRTileJob> iterator() {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' ORDER BY added ASC");
        @SuppressWarnings("unchecked")
        List<MCRTileJob> result = query.list();
        return result.iterator();
    }

    public int size() {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("SELECT count(*) FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "'");
        return ((Number) query.iterate().next()).intValue();
    }

    public MCRTileJob getElementOutOfOrder(String derivate, String path) throws NoSuchElementException {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery(
                "FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' AND derivate='" + derivate + /*
                                                                                                              * "' AND path='"+
                                                                                                              * path +
                                                                                                              */"'").setMaxResults(2);
        LOGGER.info("bin in der gEOOO :) " + derivate);
        LOGGER.info("query.list: " + query.list() + " " + query.list().size());
        if (query.list() == null || query.list().size() != 1) {
            LOGGER.info("Bin in der If-Anweisung");
            throw new NoSuchElementException();
        }
        System.out.println("Und hier bin ich auch :)");
        MCRTileJob job = (MCRTileJob) query.iterate().next();
        job.setStart(new Date(System.currentTimeMillis()));
        job.setStatus(MCRJobState.PROCESS);
        if (!updateJob(job)) {
            throw new NoSuchElementException();
        }
        return job;
    }

    private MCRTileJob getElement() {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' ORDER BY added ASC")
                .setMaxResults(1);

        if (query.list() == null || query.list().size() != 1) {
            return null;
        }
        return (MCRTileJob) query.iterate().next();
    }

    /* private */boolean updateJob(MCRTileJob job) {
        Session session = MCRHIBConnection.instance().getSession();
        session.update(job);
        return true;
    }

    private boolean addJob(MCRTileJob job) {
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
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM MCRTileJOB WHERE derivate = :derivate AND path = :path");
        query.setParameter("derivate", derivate);
        query.setParameter("path", path);
        return query.executeUpdate();
    }

    public int remove(String derivate) {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM MCRTileJOB WHERE derivate = :derivate");
        query.setParameter("derivate", derivate);
        return query.executeUpdate();
    }
}