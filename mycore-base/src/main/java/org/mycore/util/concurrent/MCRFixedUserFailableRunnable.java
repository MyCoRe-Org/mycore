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

import org.apache.commons.lang3.function.FailableRunnable;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRUserInformation;

/**
 * Encapsulates a {@link FailableRunnable} with a mycore session belonging to a specific user and a database
 * transaction.
 */
public class MCRFixedUserFailableRunnable<E extends Exception> implements Runnable, MCRDecorator<FailableRunnable<E>> {

    private final FailableRunnable<E> runnable;

    private final MCRSession session;

    private final MCRUserInformation userInfo;

    /**
     * Shorthand for {@link #MCRFixedUserFailableRunnable(FailableRunnable, MCRSession, MCRUserInformation)}
     * with <code>null</code> as the session parameter.
     */
    public MCRFixedUserFailableRunnable(FailableRunnable<E> runnable, MCRUserInformation userInfo) {
        this(runnable, null, userInfo);
    }

    /**
     * Creates a new {@link FailableRunnable} encapsulating the {@link #run()} method with a new
     * a database transaction. The transaction will be created in the context of a session
     * and the privileges of the given user information.
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
     * If a provided or thread bound session is already associated with user information, 
     * that user information <strong>must be equal</strong> to the user information that
     * is given as a parameter.
     * 
     * @param runnable the callable to execute within a session and transaction
     * @param session  the session to use
     * @param userInfo specify the user this callable should run
     */
    public MCRFixedUserFailableRunnable(FailableRunnable<E> runnable, MCRSession session, MCRUserInformation userInfo) {
        this.runnable = Objects.requireNonNull(runnable);
        this.session = session;
        this.userInfo = Objects.requireNonNull(userInfo);
    }

    @Override
    public void run() {
        try {
            new MCRFixedUserCallable<>(() -> {
                runnable.run();
                return null;
            }, session, userInfo).call();
        } catch (Exception e) {
            throw new MCRException("Failed to run nested runnable with a fixed user", e);
        }
    }

    @Override
    public FailableRunnable<E> get() {
        return this.runnable;
    }

}
