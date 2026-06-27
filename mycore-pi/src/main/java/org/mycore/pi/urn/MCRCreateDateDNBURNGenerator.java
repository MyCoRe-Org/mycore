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

import static org.mycore.pi.util.MCRPIGeneratorUtils.formatCount;
import static org.mycore.pi.util.MCRPIGeneratorUtils.getCountPattern;
import static org.mycore.pi.util.MCRPIGeneratorUtils.getCreateDate;
import static org.mycore.pi.util.MCRPIGeneratorUtils.readCountFromDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.date.MCRDateFormatter;
import org.mycore.common.date.MCRISO8601DateFormatter;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRGenericPIGenerator;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * {@link MCRCreateDateDNBURNGenerator} is a {@link MCRPIGenerator} for {@link MCRDNBURN} identifiers
 * that generates identifiers using a given prefix and the current date and a per-date counter for the suffix.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRGenericPIGenerator#DATE_FORMATTER_KEY} can be used to
 * specify the date formatter to be used (optional, defaults to {@link MCRISO8601DateFormatter} with format
 * {@link MCRCreateDateDNBURNGenerator#DEFAULT_DATE_FORMAT} and locale
 * {@link MCRCreateDateDNBURNGenerator#DEFAULT_DATE_LOCALE}).
 * <li> The property suffix {@link MCRCreateDateDNBURNGenerator#NAMESPACE_KEY} can be used to
 * specify the namespace.
 * <li> The property suffix {@link MCRCreateDateDNBURNGenerator#DELIMITER_KEY} can be used to
 * specify a delimiter to be placed before and after the NISS (optional, defaults to the empty string).
 * <li> The property suffix {@link MCRCreateDateDNBURNGenerator#COUNT_PRECISION_KEY} can be used to
 * specify number of digits to be used for the count (optional, defaults to <code>-1</code>,
 * which uses the natural number of digits).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.urn.MCRCreateDateDNBURNGenerator
 * [...].DateFormatter.Class=org.mycore.common.date.MCRSimpleDateFormatter
 * [...].DateFormatter.Format=yyyy-MM-dd
 * [...].Prefix=10.1234
 * [...].CountPrecision=6
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCreateDateDNBURNGenerator.Factory.class)
public class MCRCreateDateDNBURNGenerator extends MCRDNBURNGeneratorBase {

    public static final String DEFAULT_DATE_FORMAT = "yyyyMMdd-HHmmss";

    public static final Locale DEFAULT_DATE_LOCALE = Locale.ENGLISH;

    public static final String DATE_FORMATTER_KEY = "Formatter";

    public static final String NAMESPACE_KEY = "Namespace";

    public static final String DELIMITER_KEY = "Delimiter";

    public static final String COUNT_PRECISION_KEY = "CountPrecision";

    private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

    private final MCRDateFormatter dateFormatter;

    private final String namespace;

    private final String delimiter;

    private final int countPrecision;

    private final String countPattern;

    public MCRCreateDateDNBURNGenerator(MCRDateFormatter dateFormatter, String namespace,
        String delimiter, int countPrecision) {
        super(namespace, delimiter);
        this.dateFormatter = Objects.requireNonNull(dateFormatter, "Date formatter must not be null");
        this.namespace = Objects.requireNonNull(namespace, "Namespace must not be null");
        this.delimiter = Objects.requireNonNull(delimiter, "Delimiter must not be null");
        this.countPrecision = countPrecision;
        this.countPattern = getCountPattern(countPrecision);
    }

    @Override
    protected String buildNISS(MCRBase base, String additional) throws MCRPersistentIdentifierException {

        String prefixWithDate = dateFormatter.format(getCreateDate(base)) + "-";
        int count = getCount(Pattern.quote(namespace + delimiter + prefixWithDate) + countPattern
            + Pattern.quote(delimiter) + "[0-9]");

        return prefixWithDate + formatCount(count, countPrecision);

    }

    private synchronized int getCount(final String pattern) {
        return PATTERN_COUNT_MAP
            .computeIfAbsent(pattern, p -> readCountFromDatabase(MCRDNBURN.TYPE, p))
            .getAndIncrement();
    }

    public static class Factory implements Supplier<MCRCreateDateDNBURNGenerator> {

        @MCRInstance(name = DATE_FORMATTER_KEY, valueClass = MCRDateFormatter.class, required = false)
        public MCRDateFormatter dateFormatter;

        @MCRProperty(name = NAMESPACE_KEY)
        public String namespace;

        @MCRProperty(name = DELIMITER_KEY, required = false)
        public String delimiter = "";

        @MCRProperty(name = COUNT_PRECISION_KEY, required = false)
        public String countPrecision = "-1";

        @Override
        public MCRCreateDateDNBURNGenerator get() {
            return new MCRCreateDateDNBURNGenerator(getDateFormatter(), namespace, delimiter,
                Integer.parseInt(countPrecision));
        }

        private MCRDateFormatter getDateFormatter() {
            return dateFormatter != null ? dateFormatter
                : new MCRISO8601DateFormatter(DEFAULT_DATE_FORMAT, DEFAULT_DATE_LOCALE);
        }

    }

}
