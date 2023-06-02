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

/**
 * Possible status of a job.
 * @author René Adler
 */
public enum MCRJobStatus {
    /**
     * job added to queue
     */
    NEW,
    /**
     * job currently on processing
     */
    PROCESSING,
    /**
     * job processing is finished successfully
     */
    FINISHED,
    /**
     * job processing failed and reached maximum tries
     */
    MAX_TRIES,
    /**
     * job processing failed, but can be resetted
     */
    ERROR
}
