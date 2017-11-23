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

import java.time.Instant;

/**
 * Objects can implement this interface if they are capable of being prioritized.
 *
 * @author Matthias Eichner
 */
public interface MCRPrioritizable extends Comparable<MCRPrioritizable> {

    /**
     * Returns the priority.
     */
    int getPriority();

    Instant getCreated();

    @Override
    default int compareTo(MCRPrioritizable o) {
        if (o == null) {
            return -1;
        }
        if (o.getPriority() == getPriority()) {
            return getCreated().compareTo(o.getCreated());
        }
        return Integer.compare(o.getPriority(), getPriority());
    }

}
