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

package org.mycore.common.processing;

/**
 * The status of one {@link MCRProcessable}. Can be one of created,
 * processing, canceled, failed or successful.
 * 
 * @author Matthias Eichner
 */
public enum MCRProcessableStatus {

    /**
     * The process is created and not started yet.
     */
    created,

    /**
     * The process is currently running.
     */
    processing,

    /**
     * Canceled by the user and not by an error.
     */
    canceled,

    /**
     * An exception/error occurred while processing.
     */
    failed,

    /**
     * The process is successfully done.
     */
    successful

}
