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

import org.mycore.common.MCRSession;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUserInformation;

/**
 * Encapsulates a {@link Callable} with a mycore session belonging to a specific user and a database transaction.
 */
public class MCRFixedUserCallable<V> extends MCRTransactionableCallable<V> {

    private final MCRUserInformation userInfo;

    /**
     * Creates a new {@link Callable} encapsulating the {@link #call()} method with a new
     * database transaction. The transaction will be created in the context of a session
     * and the privileges of the given user information.
     * Afterward the transaction will be committed and the session will be released.
     * <p>
     * In order for this to work, no session must be bound to the thread in which this 
     * callable is executed.
     *
     * @param callable the callable to execute within a session and transaction
     * @param userInfo specify the user this callable should run
     */
    public MCRFixedUserCallable(Callable<V> callable, MCRUserInformation userInfo) {
        super(callable, null);
        this.userInfo = Objects.requireNonNull(userInfo);
    }

    @Override
    protected void onBeforeTransaction(MCRSession session, SessionType type) {
        if (type == SessionType.EPHEMERAL) {
            session.setUserInformation(userInfo);
        } else {
            throw new MCRUsageException("An existing session was reused.");
        }
    }

}
