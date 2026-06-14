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
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.util.MCROtherPIValueExtractor;

/**
 * {@link MCROtherPIDOIGenerator} is a {@link MCRPIGenerator} for {@link MCRDigitalObjectIdentifier} identifiers
 * that generates identifiers using a given prefix and a value extracted from another PI
 * that is already assigned as the suffix.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCROtherPIDOIGenerator#PREFIX_KEY} can be used to
 * specify the prefix.
 * <li> The property suffix {@link MCROtherPIDOIGenerator#TYPE_KEY} can be used to
 * specify the type of the assigned PI.
 * <li> The property suffix {@link MCROtherPIDOIGenerator#SERVICE_KEY} can be used to
 * specify the service of the assigned PI.
 * <li> The property suffix {@link MCROtherPIDOIGenerator#PATTERN_KEY} can be used to
 * specify the pattern (must contain a single capture group).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.doi.MCROtherPIDOIGenerator
 * [...].Prefix=10.1234
 * [...].Type=dnbUrn
 * [...].Service=DNBURN
 * [...].Pattern=urn:nbn:de:xzy-(.+)-[0-9]
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCROtherPIDOIGenerator.Factory.class)
public class MCROtherPIDOIGenerator extends MCRDOIGeneratorBase {

    public static final String TYPE_KEY = "Type";

    public static final String SERVICE_KEY = "Service";

    public static final String PATTERN_KEY = "Pattern";

    public static final String PREFIX_KEY = "Prefix";

    private final String prefix;

    private final MCROtherPIValueExtractor extractor;

    public MCROtherPIDOIGenerator(MCRDOIParser parser, String prefix, MCROtherPIValueExtractor extractor) {
        super(parser);
        this.prefix = Objects.requireNonNull(prefix, "Prefix must not be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor  must not be null");
    }

    @Override
    protected String buildDOI(MCRBase base, String additional) {
        return prefix + "/" + extractor.extractValue(base.getId());
    }

    public static class Factory implements Supplier<MCROtherPIDOIGenerator> {

        @MCRProperty(name = PREFIX_KEY, defaultName = "MCR.DOI.Prefix")
        public String prefix;

        @MCRProperty(name = TYPE_KEY)
        public String type;

        @MCRProperty(name = SERVICE_KEY)
        public String service;

        @MCRProperty(name = PATTERN_KEY)
        public String pattern;

        @Override
        public MCROtherPIDOIGenerator get() {
            MCROtherPIValueExtractor extractor = new MCROtherPIValueExtractor(type, service, pattern);
            return new MCROtherPIDOIGenerator(new MCRDOIParser(), prefix, extractor);
        }

    }

}
