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

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.util.MCROtherPIValueExtractor;

/**
 * {@link MCROtherPIDNBURNGenerator} is a {@link MCRPIGenerator} for {@link MCRDNBURN} identifiers
 * that generates identifiers using a given namespace and a value extracted from another PI
 * that is already assigned as the NISS.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCROtherPIDNBURNGenerator#NAMESPACE_KEY} can be used to
 * specify the namespace.
 * <li> The property suffix {@link MCROtherPIDNBURNGenerator#DELIMITER_KEY} can be used to
 * specify a delimiter to be placed before and after the NISS (optional, defaults to the empty string).
 * <li> The property suffix {@link MCROtherPIDNBURNGenerator#TYPE_KEY} can be used to
 * specify the type of the assigned PI.
 * <li> The property suffix {@link MCROtherPIDNBURNGenerator#SERVICE_KEY} can be used to
 * specify the service of the assigned PI.
 * <li> The property suffix {@link MCROtherPIDNBURNGenerator#PATTERN_KEY} can be used to
 * specify the pattern (must contain a single capture group).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.urn.MCROtherPIDNBURNGenerator
 * [...].Namespace=urn:nbn:de:gbv:xyz
 * [...].Delimiter=-
 * [...].Type=doi
 * [...].Service=Datacite
 * [...].Pattern=10.1234/(.+)
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCROtherPIDNBURNGenerator.Factory.class)
public class MCROtherPIDNBURNGenerator extends MCRDNBURNGeneratorBase {

    public static final String TYPE_KEY = "Type";

    public static final String SERVICE_KEY = "Service";

    public static final String PATTERN_KEY = "Pattern";

    public static final String NAMESPACE_KEY = "Namespace";

    public static final String DELIMITER_KEY = "Delimiter";

    private final MCROtherPIValueExtractor extractor;

    public MCROtherPIDNBURNGenerator(String namespace, String delimiter, MCROtherPIValueExtractor extractor) {
        super(namespace, delimiter);
        this.extractor = Objects.requireNonNull(extractor, "Extractor must not be null");
    }

    @Override
    protected String buildNISS(MCRBase base, String additional) {
        return extractor.extractValue(base);
    }

    public static class Factory implements Supplier<MCROtherPIDNBURNGenerator> {

        @MCRProperty(name = NAMESPACE_KEY)
        public String namespace;

        @MCRProperty(name = DELIMITER_KEY, required = false)
        public String delimiter = "";

        @MCRProperty(name = TYPE_KEY)
        public String type;

        @MCRProperty(name = SERVICE_KEY)
        public String service;

        @MCRProperty(name = PATTERN_KEY)
        public String pattern;

        @Override
        public MCROtherPIDNBURNGenerator get() {
            MCROtherPIValueExtractor extractor = new MCROtherPIValueExtractor(type, service, pattern);
            return new MCROtherPIDNBURNGenerator(namespace, delimiter, extractor);
        }

    }

}
