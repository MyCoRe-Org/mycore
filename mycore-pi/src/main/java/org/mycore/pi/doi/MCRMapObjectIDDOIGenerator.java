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

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * {@link MCRMapObjectIDDOIGenerator} is a {@link MCRPIGenerator} for {@link MCRDigitalObjectIdentifier} identifiers
 * that generates identifiers by concatenating a {@link MCRObjectID#getBase}-dependent value and the numerical
 * part of the {@link MCRObjectID} of the {@link MCRBase}.
 * <p>
 * Example: Prefix mapping <code>mycore_mods=10.1234/MODS.</code> will map <code>mycore_mods_00000123</code>
 * to <code>10.1234/MODS.123</code>
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRMapObjectIDDOIGenerator#PREFIX_KEY} can be used to
 * specify the prefix mappings to be used. 
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.doi.MCRMapObjectIDDOIGenerator
 * [...].Prefix.mycore_mods=10.1234/MODS.
 * [...].Prefix.mycore_alto=10.9876/ALTO.
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRMapObjectIDDOIGenerator.Factory.class)
public class MCRMapObjectIDDOIGenerator extends MCRDOIGeneratorBase {

    public static final String PREFIX_KEY = "Prefix";

    private final Map<String, String> prefixMap;

    public MCRMapObjectIDDOIGenerator(MCRDOIParser parser, Map<String, String> prefixMap) {
        super(parser);
        this.prefixMap = Objects.requireNonNull(prefixMap, "Prefix map  must not be null");
    }

    @Override
    protected String buildDOI(MCRBase base, String additional) throws MCRPersistentIdentifierException {

        MCRObjectID objectId = base.getId();
        String prefix = prefixMap.get(objectId.getBase());

        if (prefix == null) {
            throw new MCRPersistentIdentifierException("Missing prefix for base " + objectId.getBase());
        }

        int objectIdNumberAsInteger = objectId.getNumberAsInteger();
        return prefix.contains("/") ? prefix + objectIdNumberAsInteger : prefix + '/' + objectIdNumberAsInteger;

    }

    public static class Factory implements Supplier<MCRMapObjectIDDOIGenerator> {

        @MCRPropertyMap(name = PREFIX_KEY)
        public Map<String, String> prefixMap;

        @Override
        public MCRMapObjectIDDOIGenerator get() {
            return new MCRMapObjectIDDOIGenerator(new MCRDOIParser(), prefixMap);
        }

    }
}
