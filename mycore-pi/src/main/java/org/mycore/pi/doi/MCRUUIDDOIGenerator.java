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

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;

/**
 * {@link MCRUUIDDOIGenerator} is a {@link MCRPIGenerator} for {@link MCRDigitalObjectIdentifier} identifiers
 * that generates identifiers using a given prefix and a {@link UUID} as the suffix.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRUUIDDOIGenerator#PREFIX_KEY} can be used to
 * specify the prefix.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.doi.MCRUUIDDOIGenerator
 * [...].Prefix=10.1234
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRUUIDDOIGenerator.Factory.class)
public class MCRUUIDDOIGenerator extends MCRDOIGeneratorBase {

    public static final String PREFIX_KEY = "Prefix";

    private final String prefix;

    public MCRUUIDDOIGenerator(MCRDOIParser parser, String prefix) {
        super(parser);
        this.prefix = Objects.requireNonNull(prefix, "Prefix must not be null");
    }

    @Override
    protected String buildDOI(MCRBase base, String additional) {
        return prefix + "/" + UUID.randomUUID();
    }

    public static class Factory implements Supplier<MCRUUIDDOIGenerator> {

        @MCRProperty(name = PREFIX_KEY, defaultName = "MCR.DOI.Prefix")
        public String prefix;

        @Override
        public MCRUUIDDOIGenerator get() {
            return new MCRUUIDDOIGenerator(new MCRDOIParser(), prefix);
        }

    }

}
