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

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;

/**
 * Encapsulates a {@link Callable} with a mycore session belonging to a specific user and a database transaction.
 * 
 * @author Matthias Eichner
 */
public class MCRFixedUserCallable<V> extends MCRTransactionableCallable<V> {

    private MCRUserInformation userInfo;

    /**
     * Creates a new {@link Callable} encapsulating the {@link #call()} method with a new
     * <b>SYSTEM</b> {@link MCRSession} and a database transaction. Afterwards the transaction will
     * be committed and the session will be released and closed.
     * 
     * @param callable the callable to execute within a <b>SYSTEM</b> session and transaction
     * @param userInfo specify the user this callable should run
     */
    public MCRFixedUserCallable(Callable<V> callable, MCRUserInformation userInfo) {
        super(callable);
        this.userInfo = Objects.requireNonNull(userInfo);
    }

    @Override
    public V call() throws Exception {
        final boolean hasSession = MCRSessionMgr.hasCurrentSession();
        this.session = MCRSessionMgr.getCurrentSession();
        try {
            MCRUserInformation currentUser = this.session.getUserInformation();
            if (hasSession) {
                if (!currentUser.equals(userInfo)) {
                    throw new MCRException(
                        "MCRFixedUserCallable is bound to " + currentUser.getUserID() + " and not to "
                            + userInfo.getUserID() + ".");
                }
            } else {
                this.session.setUserInformation(userInfo);
            }

            return super.call();
        } finally {
            if (!hasSession && this.session != null) {
                this.session.close();
            }
        }
    }

}
