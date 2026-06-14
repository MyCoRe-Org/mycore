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

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRDNBURNGeneratorBase extends MCRPIGenerator<MCRDNBURN> {

    private static final String URN_NBN_DE = "urn:nbn:de:";

    private final String namespace;

    private final String delimiter;

    public MCRDNBURNGeneratorBase(String namespace, String delimiter) {
        this.namespace = checkNamespace(Objects.requireNonNull(namespace, "Namespace must not be null"));
        this.delimiter = Objects.requireNonNull(delimiter, "Delimiter must not be null");
    }

    private static String checkNamespace(String namespace) {
        if (namespace.startsWith(URN_NBN_DE)) {
            namespace = namespace.substring(URN_NBN_DE.length());
        }
        return namespace;
    }

    @Override
    public MCRDNBURN generate(MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        return new MCRDNBURN(namespace, delimiter + buildNISS(base, additional) + delimiter);
    }

    protected abstract String buildNISS(MCRBase base, String additional)
        throws MCRPersistentIdentifierException;

}
