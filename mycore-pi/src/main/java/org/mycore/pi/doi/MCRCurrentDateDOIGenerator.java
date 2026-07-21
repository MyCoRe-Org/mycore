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

import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.util.MCRFLDateScrambler;

/**
 * {@link MCRCurrentDateDOIGenerator} is a {@link MCRPIGenerator} for {@link MCRDigitalObjectIdentifier} identifiers
 * that generates identifiers using a given prefix and the current date (in seconds) value as the suffix.
 * <p>
 * Only one suffix per second will be generated.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRCurrentDateDOIGenerator#PREFIX_KEY} can be used to
 * specify the prefix.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.doi.MCRCurrentDateDOIGenerator
 * [...].Prefix=10.1234
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCurrentDateDOIGenerator.Factory.class)
public class MCRCurrentDateDOIGenerator extends MCRDOIGeneratorBase {

    public static final String PREFIX_KEY = "Prefix";

    private final String prefix;

    private String lastSuffix;

    public MCRCurrentDateDOIGenerator(MCRDOIParser parser, String prefix) {
        super(parser);
        this.prefix = Objects.requireNonNull(prefix, "Prefix must not be null");
    }

    @Override
    protected synchronized String buildDOI(MCRBase base, String additional) {

        Date date = new Date((System.currentTimeMillis() / 1000) * 1000);
        String suffix = MCRFLDateScrambler.scrambleDate(date);

        if (suffix.equals(lastSuffix)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            return buildDOI(base, additional);
        }

        lastSuffix = suffix;

        return prefix + "/" + suffix;

    }

    public static class Factory implements Supplier<MCRCurrentDateDOIGenerator> {

        @MCRProperty(name = PREFIX_KEY, defaultName = "MCR.DOI.Prefix")
        public String prefix;

        @Override
        public MCRCurrentDateDOIGenerator get() {
            return new MCRCurrentDateDOIGenerator(new MCRDOIParser(), prefix);
        }

    }

}
