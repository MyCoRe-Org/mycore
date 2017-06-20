/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
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
    abstract public boolean isActivated();

    /**
     * Returns the name of the action.
     * 
     * @return the name
     */
    abstract public String name();

    /**
     * Does the work for given {@link MCRJob}.
     */
    abstract public void execute() throws ExecutionException;

    /**
     * When errors occurs during executing it can be necessary to rollback
     * performed actions
     */
    abstract public void rollback();
}
