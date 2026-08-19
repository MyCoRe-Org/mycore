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

package org.mycore.pi.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Utility class providing functions commonly used by implementations of {@link MCRPIGenerator}.
 */
public final class MCRPIGeneratorUtils {

    private MCRPIGeneratorUtils() {
    }

    public static String getCountPattern(int countPrecision) {
        String countPattern;
        if (countPrecision == -1) {
            countPattern = "([0-9]+)";
        } else {
            countPattern = "(" + "[0-9]".repeat(countPrecision) + ")";
        }
        return countPattern;
    }

    public static String formatCount(int count, int counterPrecision)
        throws MCRPersistentIdentifierException {

        if (counterPrecision == -1) {
            return String.valueOf(count);
        }

        int actualLength = String.valueOf(Math.abs(count)).length();
        if (actualLength > counterPrecision) {
            throw new MCRPersistentIdentifierException(
                "Count " + count + " (" + actualLength + " digits) exceeds counter precision of "
                    + counterPrecision + " digits.");
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        DecimalFormat decimalFormat = new DecimalFormat("0".repeat(counterPrecision), symbols);
        return decimalFormat.format(count);

    }

    public static Date getCreateDate(MCRBase base) throws MCRPersistentIdentifierException {
        Date createDate = base.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
        if (createDate == null) {
            throw new MCRPersistentIdentifierException("Object " + base.getId() + " doesn't have a create date!");
        }
        return createDate;
    }

    public interface Counter {

        /**
         * Returns the next counter value to be used for a specific type and pattern. The returned value
         * needs to be unique. Returned values should be increasing.
         * @param type    the type of persistent identifier.
         * @param pattern a regex pattern which can be used to extract count value in existing identifiers.
         *                The first capturing group captures the count.
         *                Example: <code>[0-9]+-mods-2017-([0-9][0-9][0-9][0-9])-[0-9]</code>
         *                will match <code>31-mods-2017-0003-3</code> and the returned count should be <code>4</code>
         *                (<code>3+1</code>).
         * @return the next count
         */
        int getCount(String type, String pattern);

    }

    /**
     * {@link Counter} that gets the count for a specific type and pattern and increase the internal counter.
     * If there is no internal counter, it will look into the Database and detect the highest count with the pattern.
     */
    public static final class CachingDatabaseCounter implements Counter {

        private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

        @Override
        public synchronized int getCount(String type, String pattern) {
            return PATTERN_COUNT_MAP
                .computeIfAbsent(pattern, p -> readCountFromDatabase(type, p))
                .getAndIncrement();
        }

        private static AtomicInteger readCountFromDatabase(String type, String countPattern) {

            Pattern pattern = Pattern.compile(countPattern);
            Predicate<String> matching = pattern.asPredicate();

            List<MCRPIRegistrationInfo> list = MCRPIManager.getInstance().getList(type, -1, -1);

            // extract the number of the PI
            Optional<Integer> highestNumber = list.stream()
                .map(MCRPIRegistrationInfo::getIdentifier)
                .filter(matching)
                .map(pi -> {
                    // extract the number of the PI
                    Matcher matcher = pattern.matcher(pi);
                    if (matcher.find() && matcher.groupCount() == 1) {
                        String group = matcher.group(1);
                        return Integer.parseInt(group, 10);
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(n -> n + 1);

            return new AtomicInteger(highestNumber.orElse(0));

        }

    }

}
