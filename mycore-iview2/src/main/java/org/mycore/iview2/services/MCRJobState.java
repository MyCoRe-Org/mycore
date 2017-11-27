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

package org.mycore.iview2.services;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the status of tiling jobs
 * @author Thomas Scheffler (yagee)
 *
 */
public enum MCRJobState {
    /**
     * job is added to tiling queue
     */
    NEW('n'),
    /**
     * job is currently being processed by an image tiler
     */
    PROCESSING('p'),
    /**
     * image tiling process is complete
     */
    FINISHED('f'),
    /**
     * image tiling process is complete
     */
    ERROR('e');

    private static final Set<MCRJobState> NOT_COMPLETE_STATES = Collections.unmodifiableSet(Stream
        .of(MCRJobState.ERROR, MCRJobState.PROCESSING, MCRJobState.NEW).collect(
            Collectors.toSet()));

    private static final Set<MCRJobState> COMPLETE_STATES = Collections.unmodifiableSet(Stream
        .of(MCRJobState.FINISHED).collect(
            Collectors.toSet()));

    public static Set<MCRJobState> notCompleteStates() {
        return NOT_COMPLETE_STATES;
    }

    public static Set<MCRJobState> completeStates() {
        return COMPLETE_STATES;
    }

    private char status;

    MCRJobState(char status) {
        this.status = status;
    }

    /**
     * returns character representing the status
     * @return
     * <table>
     *  <caption>returned characters depending on current state</caption>
     *  <tr>
     *   <th>{@link #NEW}</th>
     *   <td>'n'</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #PROCESSING}</th>
     *   <td>'p'</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #FINISHED}</th>
     *   <td>'f'</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #ERROR}</th>
     *   <td>'e'</td>
     *  </tr>
     * </table>
     */
    public char toChar() {
        return status;
    }
}
