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

import java.util.UUID;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;

/**
 * {@link MCRUUIDURNGenerator} is a {@link MCRPIGenerator} for {@link MCRDNBURN} identifiers
 * that generates identifiers using a given namespace and a {@link UUID} as the NISS.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRUUIDURNGenerator#NAMESPACE_KEY} can be used to
 * specify the namespace.
 * <li> The property suffix {@link MCRUUIDURNGenerator#DELIMITER_KEY} can be used to
 * specify a delimiter to be placed before and after the NISS (optional, defaults to the empty string).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.urn.MCRUUIDURNGenerator
 * [...].Namespace=urn:nbn:de:gbv:xyz
 * [...].Delimiter=-
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRUUIDURNGenerator.Factory.class)
public class MCRUUIDURNGenerator extends MCRDNBURNGeneratorBase {

    public static final String NAMESPACE_KEY = "Namespace";

    public static final String DELIMITER_KEY = "Delimiter";

    public MCRUUIDURNGenerator(String namespace, String delimiter) {
        super(namespace, delimiter);
    }

    @Override
    protected String buildNISS(MCRBase base, String additional) {
        return UUID.randomUUID().toString();
    }

    public static class Factory implements Supplier<MCRUUIDURNGenerator> {

        @MCRProperty(name = NAMESPACE_KEY)
        public String namespace;

        @MCRProperty(name = DELIMITER_KEY, required = false)
        public String delimiter = "";

        @Override
        public MCRUUIDURNGenerator get() {
            return new MCRUUIDURNGenerator(namespace, delimiter);
        }

    }

}
