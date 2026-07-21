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
 * {@link MCRUUIDDNBURNGenerator} is a {@link MCRPIGenerator} for {@link MCRDNBURN} identifiers
 * that generates identifiers using a given namespace and a {@link UUID} as the NISS.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRUUIDDNBURNGenerator#NAMESPACE_KEY} can be used to
 * specify the namespace.
 * <li> The property suffix {@link MCRUUIDDNBURNGenerator#DELIMITER_KEY} can be used to
 * specify a delimiter to be placed before and after the NISS (optional, defaults to the empty string).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.urn.MCRUUIDDNBURNGenerator
 * [...].Namespace=urn:nbn:de:gbv:xyz
 * [...].Delimiter=-
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRUUIDDNBURNGenerator.Factory.class)
public class MCRUUIDDNBURNGenerator extends MCRDNBURNGeneratorBase {

    public static final String NAMESPACE_KEY = "Namespace";

    public static final String DELIMITER_KEY = "Delimiter";

    public MCRUUIDDNBURNGenerator(String namespace, String delimiter) {
        super(namespace, delimiter);
    }

    @Override
    protected String buildNISS(MCRBase base, String additional) {
        return UUID.randomUUID().toString();
    }

    public static class Factory implements Supplier<MCRUUIDDNBURNGenerator> {

        @MCRProperty(name = NAMESPACE_KEY)
        public String namespace;

        @MCRProperty(name = DELIMITER_KEY, required = false)
        public String delimiter = "";

        @Override
        public MCRUUIDDNBURNGenerator get() {
            return new MCRUUIDDNBURNGenerator(namespace, delimiter);
        }

    }

}
