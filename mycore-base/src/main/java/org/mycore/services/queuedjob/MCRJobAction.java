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

package org.mycore.services.queuedjob;

import java.util.concurrent.ExecutionException;

/**
 * <code>MCRJobAction</code> must be extended to do some work for given {@link MCRJob}.
 * 
 * @author Ren\u00E9 Adler
 *
 */
public abstract class MCRJobAction {
    protected MCRJob job;

    /**
     * The constructor of the job action.
     */
    public MCRJobAction() {
    }

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     */
    public MCRJobAction(MCRJob job) {
        this.job = job;
    }

    /**
     * Returns if this action is activated.
     * 
     * @return <code>true</code> if activated, <code>false</code> if isn't
     */
    public abstract boolean isActivated();

    /**
     * Returns the name of the action.
     * 
     * @return the name
     */
    public abstract String name();

    /**
     * Does the work for given {@link MCRJob}.
     */
    public abstract void execute() throws ExecutionException;

    /**
     * When errors occurs during executing it can be necessary to rollback
     * performed actions
     */
    public abstract void rollback();
}
