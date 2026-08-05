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
import java.util.List;
import java.util.Locale;
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

    public static AtomicInteger readCountFromDatabase(String type, String countPattern) {

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

}
