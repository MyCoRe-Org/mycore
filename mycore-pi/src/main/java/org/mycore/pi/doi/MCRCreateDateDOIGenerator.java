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

package org.mycore.pi.doi;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.date.MCRISO8601DateFormatter;
import org.mycore.pi.MCRGenericPIGenerator;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.util.MCRPIGeneratorUtils;

/**
 * {@link MCRCreateDateDOIGenerator} is a {@link MCRPIGenerator} for {@link MCRDigitalObjectIdentifier} identifiers
 * that generates identifiers using a given prefix and the current date and a per-date counter for the suffix.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRCreateDateDOIGenerator#DATE_FORMAT_KEY} can be used to
 * specify the date format to be used.
 * <li> The property suffix {@link MCRCreateDateDOIGenerator#PREFIX_KEY} can be used to
 * specify the prefix.
 * <li> The property suffix {@link MCRCreateDateDOIGenerator#COUNT_PRECISION_KEY} can be used to
 * specify number of digits to be used for the count (optional, defaults to <code>-1</code>,
 * which uses the natural number of digits).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.doi.MCRCreateDateDOIGenerator
 * [...].DateFormat=yyyy-MM-dd
 * [...].Prefix=10.1234
 * [...].CountPrecision=6
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCreateDateDOIGenerator.Factory.class)
public class MCRCreateDateDOIGenerator extends MCRGenericPIGenerator {

    public static final String DEFAULT_PROPERTY_PREFIX = "MCR.Default.PI.Generator.CreateDate.";

    public static final String DATE_FORMAT_KEY = "DateFormat";

    public static final String PREFIX_KEY = "Prefix";

    public static final String COUNT_PRECISION_KEY = "CountPrecision";

    public MCRCreateDateDOIGenerator(MCRPIGeneratorUtils.Counter counter, String dateFormat, String prefix,
        int countPrecision) {
        super(
            counter,
            Objects.requireNonNull(prefix, "Prefix must not be null") + "/$ObjectDate-$Count",
            new MCRISO8601DateFormatter(
                Objects.requireNonNull(dateFormat, "Date format must not be null"),
                Locale.ENGLISH),
            Map.of(),
            Map.of(),
            countPrecision,
            MCRDigitalObjectIdentifier.TYPE,
            List.of());
    }

    public static class Factory implements Supplier<MCRCreateDateDOIGenerator> {

        @MCRProperty(name = DATE_FORMAT_KEY, defaultName = DEFAULT_PROPERTY_PREFIX + DATE_FORMAT_KEY)
        public String dateFormat;

        @MCRProperty(name = PREFIX_KEY, defaultName = "MCR.DOI.Prefix")
        public String prefix;

        @MCRProperty(name = COUNT_PRECISION_KEY, defaultName = DEFAULT_PROPERTY_PREFIX + COUNT_PRECISION_KEY)
        public String countPrecision = "-1";

        @Override
        public MCRCreateDateDOIGenerator get() {
            return new MCRCreateDateDOIGenerator(MCRPIGeneratorUtils.SHARED_COUNTER, dateFormat, prefix,
                Integer.parseInt(countPrecision));
        }

    }

}
