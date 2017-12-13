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

package org.mycore.pi.urn;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifierManager;

/**
 * A Generator which helps to generate a URN with a counter inside.
 */
public abstract class MCRCountingDNBURNGenerator extends MCRDNBURNGenerator {

    private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

    MCRCountingDNBURNGenerator(String generatorID) {
        super(generatorID);
    }

    protected AtomicInteger readCountFromDatabase(String countPattern) {
        Pattern regExpPattern = Pattern.compile(countPattern);
        Predicate<String> matching = regExpPattern.asPredicate();

        List<MCRPIRegistrationInfo> list = MCRPersistentIdentifierManager.getInstance()
            .getList(MCRDNBURN.TYPE, -1, -1);

        // extract the number of the PI
        Optional<Integer> highestNumber = list.stream()
            .map(MCRPIRegistrationInfo::getIdentifier)
            .filter(matching)
            .map(pi -> {
                // extract the number of the PI
                Matcher matcher = regExpPattern.matcher(pi);
                if (matcher.find() && matcher.groupCount() == 1) {
                    String group = matcher.group(1);
                    return Integer.parseInt(group, 10);
                } else {
                    return null;
                }
            }).filter(Objects::nonNull)
            .min(Comparator.reverseOrder())
            .map(n -> n + 1);
        return new AtomicInteger(highestNumber.orElse(0));
    }

    /**
     * Gets the count for a specific pattern and increase the internal counter. If there is no internal counter it will
     * look into the Database and detect the highest count with the pattern.
     *
     * @param pattern a reg exp pattern which will be used to detect the highest count. The first group is the count.
     *                e.G. [0-9]+-mods-2017-([0-9][0-9][0-9][0-9])-[0-9] will match 31-mods-2017-0003-3 and the returned
     *                count will be 4 (3+1).
     * @return the next count
     */
    public final synchronized int getCount(String pattern) {
        AtomicInteger count = PATTERN_COUNT_MAP
            .computeIfAbsent(pattern, this::readCountFromDatabase);

        return count.getAndIncrement();
    }
}
