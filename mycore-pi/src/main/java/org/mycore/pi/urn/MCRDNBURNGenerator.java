/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import javax.validation.constraints.NotNull;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRDNBURNGenerator extends MCRPIGenerator<MCRDNBURN> {

    private static final String URN_NBN_DE = "urn:nbn:de:";

    public MCRDNBURNGenerator(String generatorID) {
        super(generatorID);
    }

    protected abstract String buildNISS(MCRObjectID mcrID, String additional);

    /**
     * Allows the generation of a URN with a specific Namespace
     *
     * @param namespace  the namespace of the generated URN
     * @param mcrID      the mycore object for which the identifier is generated
     * @param additional additional information dedicated to the object like a mcrpath
     * @return a unique persistence identifier
     * @throws MCRPersistentIdentifierException if something goes wrong while generating
     */
    protected MCRDNBURN generate(@NotNull String namespace, MCRObjectID mcrID, String additional)
        throws MCRPersistentIdentifierException {
        Objects.requireNonNull(namespace, "Namespace for an URN must not be null!");
        return new MCRDNBURN(namespace, buildNISS(mcrID, additional));
    }

    @Override
    public MCRDNBURN generate(MCRBase mcrObj, String additional) throws MCRPersistentIdentifierException {
        return generate(getNamespace(), mcrObj.getId(), additional);
    }

    public String getNamespace() {
        String namespace = getProperties().get("Namespace").trim();

        if (namespace.startsWith(URN_NBN_DE)) {
            namespace = namespace.substring(URN_NBN_DE.length());
        }

        return namespace;
    }

}
