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
import java.util.concurrent.Callable;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUserInformation;

/**
 * Encapsulates a {@link Runnable} with a mycore session belonging to a specific user and a database transaction.
 */
public class MCRFixedUserRunnable implements Runnable, MCRDecorator<Runnable> {

    private final Runnable runnable;

    private final MCRUserInformation userInfo;

    /**
     * Creates a new {@link Runnable} encapsulating the {@link #run()} method with a new
     * a database transaction. The transaction will be created in the context of a session
     * and the privileges of the given user information.
     * Afterward the transaction will be committed and the session will be released.
     * <p>
     * In order for this to work, no session must be bound to the thread in which this
     * callable is executed.
     *
     * @param runnable the runnable to execute within a session and transaction
     * @param userInfo specify the user this callable should run
     */
    public MCRFixedUserRunnable(Runnable runnable, MCRUserInformation userInfo) {
        this.runnable = Objects.requireNonNull(runnable);
        this.userInfo = Objects.requireNonNull(userInfo);
    }

    @Override
    public void run() {
        try {
            new MCRFixedUserCallable<>((Callable<Void>) () -> {
                runnable.run();
                return null;
            }, userInfo).call();
        } catch (Exception e) {
            throw new MCRException("Failed to run nested runnable with a fixed user", e);
        }
    }

    @Override
    public Runnable get() {
        return this.runnable;
    }

}
