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
import java.util.UUID;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierGenerator;

public class MCRUUIDDOIGenerator extends MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private final MCRDOIParser mcrdoiParser;

    private String prefix = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public MCRUUIDDOIGenerator(String generatorString) {
        super(generatorString);
        mcrdoiParser = new MCRDOIParser();
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) {
        Optional<MCRDigitalObjectIdentifier> parse = mcrdoiParser.parse(prefix + "/" + UUID.randomUUID());
        MCRPersistentIdentifier doi = parse.get();
        return (MCRDigitalObjectIdentifier) doi;
    }
}
