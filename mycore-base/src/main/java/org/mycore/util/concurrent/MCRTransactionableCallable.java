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
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Encapsulates a {@link Callable} with a mycore session and a database transaction.
 *
 * @author Matthias Eichner
 */
public class MCRTransactionableCallable<V> implements Callable<V>, MCRDecorator<Callable<V>> {

    private static final Logger LOGGER = LogManager.getLogger();

    private Callable<V> callable;

    protected MCRSession session;

    /**
     * Creates a new {@link Callable} encapsulating the {@link #call()} method with a new
     * {@link MCRSession} and a database transaction. Afterwards the transaction will
     * be committed and the session will be released and closed.
     *
     * <p>If you want to execute your callable in the context of an already existing
     * session use the {@link MCRTransactionableCallable#MCRTransactionableCallable(Callable, MCRSession)}
     * constructor instead.
     *
     * @param callable the callable to execute within a session and transaction
     */
    public MCRTransactionableCallable(Callable<V> callable) {
        this.callable = Objects.requireNonNull(callable, "callable must not be null");
    }

    /**
     * Creates a new {@link Callable} encapsulating the {@link #call()} method with a new
     * a database transaction. The transaction will be created in the context of the
     * given session. Afterwards the transaction will be committed and the session
     * will be released (but not closed!).
     *
     * @param callable the callable to execute within a session and transaction
     * @param session the session to use
     */
    public MCRTransactionableCallable(Callable<V> callable, MCRSession session) {
        this.callable = Objects.requireNonNull(callable, "callable must not be null");
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    @Override
    public V call() throws Exception {
        boolean newSession = this.session == null;
        boolean closeSession = newSession && !MCRSessionMgr.hasCurrentSession();
        if (newSession) {
            this.session = MCRSessionMgr.getCurrentSession();
        }
        MCRSessionMgr.setCurrentSession(this.session);
        try {
            session.beginTransaction();
            return this.callable.call();
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
    public Callable<V> get() {
        return callable;
    }

}
