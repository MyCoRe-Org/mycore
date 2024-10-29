/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;

/**
 * Encapsulates a {@link Runnable} with a mycore session and a database transaction.
 */
public class MCRTransactionableRunnable implements Runnable, MCRDecorator<Runnable> {

    private final Runnable runnable;

    private final MCRSession session;

    /**
     * Shorthand for {@link #MCRTransactionableRunnable(Runnable, MCRSession)}
     * with <code>null</code> as the session parameter.
     */
    public MCRTransactionableRunnable(Runnable runnable) {
        this(runnable, null);
    }

    /**
     * Creates a new {@link Runnable} encapsulating the {@link #run()} method with a new
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
     * Otherwise, a new session will be used.
     * The session will be closed, after it has been released.
     * </li>
     * </ul>
     *
     * @param runnable the runnable to execute within a session and transaction
     * @param session  the session to use
     */
    public MCRTransactionableRunnable(Runnable runnable, MCRSession session) {
        this.runnable = Objects.requireNonNull(runnable, "runnable must not be null");
        this.session = session;
    }

    @Override
    public void run() {
        try {
            new MCRTransactionableCallable<>(() -> {
                runnable.run();
                return null;
            }, session).call();
        } catch (Exception e) {
            throw new MCRException("Failed to run nested runnable in a transaction", e);
        }
    }

    @Override
    public Runnable get() {
        return this.runnable;
    }

}
