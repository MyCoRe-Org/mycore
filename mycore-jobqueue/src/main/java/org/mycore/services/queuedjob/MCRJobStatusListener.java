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

package org.mycore.services.queuedjob;

/**
 * Interface one must implement to get notified about when an {@link MCRJob} has started, finished or failed.
 * <p>
 * Add property
 * "<code>MCR.QueuedJob.&lt;MCRJobAction.getClass().getSimpleName()&gt;.Listeners</code>"
 * to your mycore.properties and provide an appropriate class implementing the interface.
 *
 * @author shermann
 * */
public interface MCRJobStatusListener {
    /**
     * Will be called when the job has failed.
     * @param job the job that failed
     * @param e the exception that caused the failure
     */
    void onError(MCRJob job, Exception e);

    /**
     * Will be called when the job has finished successfully.
     * @param job the job that finished
     */
    void onSuccess(MCRJob job);

    /**
     * Will be called when the job has started processing.
     * @param job the job that started processing
     */
    void onProcessing(MCRJob job);
}
