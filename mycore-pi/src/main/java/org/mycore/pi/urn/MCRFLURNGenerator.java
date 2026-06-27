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

import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.date.MCRDateFormatter;
import org.mycore.common.date.MCRFLDateScrambler;

/**
 * @deprecated Use {@link MCRCurrentDateDNBURNGenerator} instead.
 */
@Deprecated(forRemoval = true)
@MCRConfigurationProxy(proxyClass = MCRFLURNGenerator.Factory.class)
public class MCRFLURNGenerator extends MCRCurrentDateDNBURNGenerator {

    public MCRFLURNGenerator(MCRDateFormatter dateFormatter, String namespace, String delimiter) {
        super(dateFormatter, namespace, delimiter);
    }

    public static class Factory implements Supplier<MCRFLURNGenerator> {

        @MCRInstance(name = DATE_FORMATTER_KEY, valueClass = MCRDateFormatter.class, required = false)
        public MCRDateFormatter formatter;

        @MCRProperty(name = NAMESPACE_KEY)
        public String namespace;

        @MCRProperty(name = DELIMITER_KEY, required = false)
        public String delimiter = "";

        @Override
        public MCRFLURNGenerator get() {
            return new MCRFLURNGenerator(getFormatter(), namespace, delimiter);
        }

        private MCRDateFormatter getFormatter() {
            return formatter != null ? formatter : new MCRFLDateScrambler();
        }

    }

}
