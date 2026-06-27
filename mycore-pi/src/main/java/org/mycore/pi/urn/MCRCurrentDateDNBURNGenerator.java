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

import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.date.MCRDateFormatter;
import org.mycore.common.date.MCRFLDateScrambler;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;

/**
 * {@link MCRCurrentDateDNBURNGenerator} is a {@link MCRPIGenerator} for {@link MCRDNBURN} identifiers
 * that generates identifiers using a given namespace and the current date (in seconds) value as the NISS.
 * <p>
 * Only one suffix per second will be generated.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRCurrentDateDNBURNGenerator#DATE_FORMATTER_KEY} can be used to
 * specify the date formatter to be used (optional, defaults to {@link MCRFLDateScrambler}).
 * <li> The property suffix {@link MCRCurrentDateDNBURNGenerator#NAMESPACE_KEY} can be used to
 * specify the namespace.
 * <li> The property suffix {@link MCRCurrentDateDNBURNGenerator#DELIMITER_KEY} can be used to
 * specify a delimiter to be placed before and after the NISS (optional, defaults to the empty string).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.urn.MCRCurrentDateDNBURNGenerator
 * [...].DateFormatter.Class=org.mycore.common.date.MCRSimpleDateFormatter
 * [...].DateFormatter.Format=yyyy-MM-dd
 * [...].Namespace=urn:nbn:de:gbv:xyz
 * [...].Delimiter=-
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCurrentDateDNBURNGenerator.Factory.class)
public class MCRCurrentDateDNBURNGenerator extends MCRDNBURNGeneratorBase {

    public static final String DATE_FORMATTER_KEY = "DateFormatter";

    public static final String NAMESPACE_KEY = "Namespace";

    public static final String DELIMITER_KEY = "Delimiter";

    private final MCRDateFormatter dateFormatter;

    private String lastNIss;

    public MCRCurrentDateDNBURNGenerator(MCRDateFormatter dateFormatter, String namespace, String delimiter) {
        super(namespace, delimiter);
        this.dateFormatter = Objects.requireNonNull(dateFormatter, "Date formatter must not be null");
    }

    @Override
    protected synchronized String buildNISS(MCRBase base, String additional) {

        Date date = new Date((System.currentTimeMillis() / 1000) * 1000);
        String niss = dateFormatter.format(date);

        if (niss.equals(lastNIss)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            return buildNISS(base, additional);
        }

        lastNIss = niss;

        return niss;

    }

    public static class Factory implements Supplier<MCRCurrentDateDNBURNGenerator> {

        @MCRInstance(name = DATE_FORMATTER_KEY, valueClass = MCRDateFormatter.class, required = false)
        public MCRDateFormatter formatter;

        @MCRProperty(name = NAMESPACE_KEY)
        public String namespace;

        @MCRProperty(name = DELIMITER_KEY, required = false)
        public String delimiter = "";

        @Override
        public MCRCurrentDateDNBURNGenerator get() {
            return new MCRCurrentDateDNBURNGenerator(getFormatter(), namespace, delimiter);
        }

        private MCRDateFormatter getFormatter() {
            return formatter != null ? formatter : new MCRFLDateScrambler();
        }

    }

}
