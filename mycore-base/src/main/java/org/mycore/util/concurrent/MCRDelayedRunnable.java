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

package org.mycore.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

import com.google.common.primitives.Ints;

/**
 * Encapsulates a {@link Runnable} with in a object that can be fed into a
 * DelayQueue
 * 
 * Note: This class has a natural ordering that is inconsistent with equals.
 * Note: Two objects of this class are equal, if their ids are equal
 *       (other properties are ignored).
 * 
 * @author Robert Stephan
 *
 */
public class MCRDelayedRunnable implements Delayed, Runnable, MCRDecorator<Runnable> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static AtomicLong ATOMIC_SYSTEM_TIME = new AtomicLong(System.currentTimeMillis());

    protected Runnable runnable;

    private MCRSession session;

    private long startTimeInMs = -1;

    private String id;

    /**
     * Creates a new {@link Runnable} encapsulating the {@link #run()} method with a new
     * {@link MCRSession} and a database transaction. Afterwards the transaction will
     * be committed and the session will be released and closed.
     * 
     * If you want to execute your runnable in the context of an already existing
     * session use the {@link MCRDelayedRunnable#MCRDelayedRunnable(String, long, MCRsession, Runnable)}
     * constructor instead.
     * 
     * @param id, - the id of the runnable (used for equals-check)
     * @param runnable the runnable to execute within a session and transaction
     * @param delayInMS - the time in (ms) the task should be delayed
     */
    public MCRDelayedRunnable(String id, long delayInMs, Runnable runnable) {
        init(id, delayInMs, runnable);
    }

    /**
     * Creates a new {@link Runnable} encapsulating the {@link #run()} method with a new
     * a database transaction. The transaction will be created in the context of the
     * given session. Afterwards the transaction will be committed and the session
     * will be released (but not closed!).
     * 
     * @param id, - the id of the runnable (used for equals-check)
     * @param runnable the runnable to execute within a session and transaction
     * @param delayInMS - the time in (ms) the task should be delayed
     * @param session the MCRSession to use
     */
    public MCRDelayedRunnable(String id, long delayInMs, MCRSession session, Runnable runnable) {
        init(id, delayInMs, runnable);
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    private void init(String id, long delayInMs, Runnable runnable) {
        this.id = id;
        this.runnable = Objects.requireNonNull(runnable, "runnable must not be null");

        long current = ATOMIC_SYSTEM_TIME.accumulateAndGet(System.currentTimeMillis(), (x, y) -> Math.max(x + 1, y));
        startTimeInMs = current + delayInMs;
    }

    @Override
    public void run() {
        boolean newSession = this.session == null;
        MCRSessionMgr.unlock();
        boolean closeSession = newSession && !MCRSessionMgr.hasCurrentSession();
        if (newSession) {
            this.session = MCRSessionMgr.getCurrentSession();
        }
        MCRSessionMgr.setCurrentSession(this.session);
        session.beginTransaction();
        try {
            this.runnable.run();
        } finally {
            try {
                session.commitTransaction();
            } catch (Exception commitExc) {
                LOGGER.error("Error while commiting transaction.", commitExc);
                try {
                    session.rollbackTransaction();
                } catch (Exception rollbackExc) {
                    LOGGER.error("Error while rollbacking transaction.", commitExc);
                }
            } finally {
                MCRSessionMgr.releaseCurrentSession();
                if (closeSession && session != null) {
                    session.close();
                }
            }
        }
    }

    @Override
    public Runnable get() {
        return this.runnable;
    }

    public String getId() {
        return id;
    }

    /**
     * order objects by their startTime
     */
    @Override
    public int compareTo(Delayed o) {
        return Ints.saturatedCast(this.startTimeInMs - ((MCRDelayedRunnable) o).startTimeInMs);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTimeInMs - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hash(id);
        return result;
    }

    /**
     * two objects are equal, if they have the same id
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MCRDelayedRunnable other = (MCRDelayedRunnable) obj;
        return Objects.equals(id, other.id);
    }

}
