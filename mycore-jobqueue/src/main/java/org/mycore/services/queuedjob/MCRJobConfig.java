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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Holds the configuration for the {@link MCRJobQueue} and related classes.
 * @author Sebastian Hofmann
 */
public interface MCRJobConfig {

    /**
     * The time in seconds after which a job with the given action is reset to {@link MCRJobStatus#NEW} if it is in
     * status {@link MCRJobStatus#ERROR} for this time.
     * @param action the action
     * @return the time as duration
     */
    Optional<Duration> timeTillReset(Class<? extends MCRJobAction> action);

    /**
     * The count of tries that are allowed for the given action. After this count is reached, the job will be marked as 
     * {@link MCRJobStatus#MAX_TRIES} and will not be executed again.
     * @param action the action
     * @return the optional count
     */
    Optional<Integer> maxTryCount(Class<? extends MCRJobAction> action);

    /**
     * The count of job threads that can be executed in parallel for the given action.
     * @param action the action
     * @return the optional count
     */
    Optional<Integer> maxJobThreadCount(Class<? extends MCRJobAction> action);

    /**
     * A boolean value indicating if the given action is activated. Activated = false means that the job queue will not
     * return jobs for this action or accept new jobs for this action.
     * @param action the action
     * @return the optional boolean value
     */
    Optional<Boolean> activated(Class<? extends MCRJobAction> action);

    /**
     * The count of job threads that can be executed in parallel for all actions.
     * @return the count
     */
    Integer maxJobThreadCount();

    /**
     * The time in seconds after which a job is reset to {@link MCRJobStatus#NEW} if it is in status
     * {@link MCRJobStatus#ERROR} for this time.
     * @return the time as duration
     */
    Duration timeTillReset();

    /**
     * The count of tries that are allowed for all actions. After this count is reached, the job will be marked as
     * {@link MCRJobStatus#MAX_TRIES} and will not be executed again.
     * @return the count
     */
    Integer maxTryCount();

    /**
     * A boolean value indicating if the job queue is activated. Activated = false means that the job queue will not
     * return jobs or accept new jobs.
     * @return the boolean value
     */
    Boolean activated();

    /**
     * Returns the list of {@link MCRJobStatusListener} for the given action.
     * @param action the action
     * @return the list of listeners
     */
    List<MCRJobStatusListener> jobStatusListeners(Class<? extends MCRJobAction> action);
}
