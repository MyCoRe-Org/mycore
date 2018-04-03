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

package org.mycore.pi.doi;

import java.util.Optional;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Uses mapping from MCRObjectID base to DOI prefix to generate DOIs.
 * e.g. <code>MCR.PI.Generator.MapObjectIDDOI.Prefix.mycore_mods = 10.5072/my.</code> will map
 * <code>mycore_mods_00004711</code> to <code>10.5072/my.4711</code>
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRMapObjectIDDOIGenerator extends MCRPIGenerator<MCRDigitalObjectIdentifier> {

    private final MCRDOIParser mcrdoiParser;

    private String generatorID;

    public MCRMapObjectIDDOIGenerator(String generatorID) {
        super(generatorID);
        mcrdoiParser = new MCRDOIParser();
        this.generatorID = generatorID;
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRBase mcrObject, String additional)
        throws MCRPersistentIdentifierException {
        final MCRObjectID objectId = mcrObject.getId();
        return Optional.ofNullable(getProperties().get("Prefix." + objectId.getBase()))
            .map(prefix -> {
                final int objectIdNumberAsInteger = objectId.getNumberAsInteger();
                return prefix.contains("/") ? prefix + objectIdNumberAsInteger
                    : prefix + '/' + objectIdNumberAsInteger;
            })
            .flatMap(mcrdoiParser::parse).map(MCRDigitalObjectIdentifier.class::cast)
            .orElseThrow(() -> new MCRPersistentIdentifierException("Prefix." + objectId.getBase() +
                " is not defined in " + generatorID + "."));
    }

}
