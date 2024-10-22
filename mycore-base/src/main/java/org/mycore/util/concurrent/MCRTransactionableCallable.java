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
import org.mycore.common.MCRTransactionManager;

/**
 * Encapsulates a {@link Callable} with a mycore session and a database transaction.
 */
public class MCRTransactionableCallable<V> implements Callable<V>, MCRDecorator<Callable<V>> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Callable<V> callable;

    private final MCRSession session;

    /**
     * Shorthand for {@link #MCRTransactionableCallable(Callable, MCRSession)}
     * with <code>null</code> as the session parameter.
     */
    public MCRTransactionableCallable(Callable<V> callable) {
        this(callable, null);
    }

    /**
     * Creates a new {@link Callable} encapsulating the {@link #call()} method with a new
     * a database transaction. The transaction will be created in the context of a session.
     * Afterward the transaction will be committed and the session will be released.
     * <ul>
     * <li>
     * If a non-null session is provided as the second parameter, that session will be used.
     * The session will <em>not</em> be closed, after it has been released.
     * </li>
     * <li>
     * Otherwise, if a session is bound to the current thread, that session will be reused.
     * The session will <em>not</em> be closed, after it has been released.
     * </li>
     * <li>
     * Otherwise, a new ephemeral session will be created and used.
     * The session will be closed, after it has been released.
     * </li>
     * </ul>
     *
     * @param callable the callable to execute within a session and transaction
     * @param session  the session to use
     */
    public MCRTransactionableCallable(Callable<V> callable, MCRSession session) {
        this.callable = Objects.requireNonNull(callable, "callable must not be null");
        this.session = session;
    }

    @Override
    public V call() throws Exception {
        TypedSession typedSession = obtainSession();
        MCRSession session = typedSession.session();
        SessionType type = typedSession.type();
        onBeforeTransaction(session, type);
        try {
            MCRTransactionManager.beginTransactions();
            return this.callable.call();
        } finally {
            try {
                MCRTransactionManager.commitTransactions();
            } catch (Exception commitException) {
                LOGGER.error("Error while committing transaction.", commitException);
                try {
                    MCRTransactionManager.rollbackTransactions();
                } catch (Exception rollbackException) {
                    LOGGER.error("Error while rolling back transaction.", rollbackException);
                }
            } finally {
                onAfterTransaction(session, type);
                MCRSessionMgr.releaseCurrentSession();
                if (type == SessionType.EPHEMERAL && session != null) {
                    session.close();
                }
            }
        }
    }

    private TypedSession obtainSession() {
        MCRSessionMgr.unlock();
        MCRSession session;
        SessionType type;
        if (this.session != null) {
            LOGGER.info("Using provided session");
            session = this.session;
            type = SessionType.PROVIDED;
        } else if (MCRSessionMgr.hasCurrentSession()) {
            LOGGER.info("Reusing existing thread bound session");
            session = MCRSessionMgr.getCurrentSession();
            type = SessionType.THREAD_BOUND;
        } else {
            LOGGER.info("Creating new ephemeral session");
            session = MCRSessionMgr.getCurrentSession();
            type = SessionType.EPHEMERAL;
        }
        MCRSessionMgr.setCurrentSession(session);
        return new TypedSession(session, type);
    }

    /**
     * Hook that is called after a session has been selected and before the transaction has begun.
     *
     * @param session the session
     * @param type    the session type
     */
    protected void onBeforeTransaction(MCRSession session, SessionType type) {
        // may be implemented by child classes
    }

    /**
     * Hook that is called after the transaction has been committed or rolled back and before the session is released.
     *
     * @param session the session
     * @param type    the session type
     */
    protected void onAfterTransaction(MCRSession session, SessionType type) {
        // may be implemented by child classes
    }

    @Override
    public Callable<V> get() {
        return callable;
    }


    protected enum SessionType {

        PROVIDED,

        THREAD_BOUND,

        EPHEMERAL;

    }

    private record TypedSession(MCRSession session, SessionType type) {

    }


}
