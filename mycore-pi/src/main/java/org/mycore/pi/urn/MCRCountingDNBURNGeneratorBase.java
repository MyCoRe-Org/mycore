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

package org.mycore.pi.urn;

import static org.mycore.pi.util.MCRPIGeneratorUtils.readCountFromDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Generator which helps to generate a URN with a counter inside.
 */
public abstract class MCRCountingDNBURNGeneratorBase extends MCRDNBURNGeneratorBase {

    private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

    MCRCountingDNBURNGeneratorBase(String namespace, String delimiter) {
        super(namespace, delimiter);
    }

    MCRCountingDNBURNGeneratorBase(String namespace) {
        super(namespace, "");
    }

    /**
     * Gets the count for a specific pattern and increase the internal counter. If there is no internal counter it will
     * look into the Database and detect the highest count with the pattern.
     *
     * @param pattern a regex pattern which will be used to detect the highest count. The first group is the count.
     *                e.G. [0-9]+-mods-2017-([0-9][0-9][0-9][0-9])-[0-9] will match 31-mods-2017-0003-3 and the returned
     *                count will be 4 (3+1).
     * @return the next count
     */
    public final synchronized int getCount(String pattern) {
        return PATTERN_COUNT_MAP
            .computeIfAbsent(pattern, p -> readCountFromDatabase(MCRDNBURN.TYPE, p))
            .getAndIncrement();
    }

}
