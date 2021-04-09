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

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Just for testing. Provides the doi in the doi property.
 */
public class MCRStaticDOIGenerator extends MCRPIGenerator<MCRDigitalObjectIdentifier> {

    public MCRStaticDOIGenerator() {
        super();
        checkPropertyExists("doi");
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRBase mcrBase, String additional)
        throws MCRPersistentIdentifierException {
        final String doi = getProperties().get("doi");
        final Optional<MCRDigitalObjectIdentifier> generatedDOI = new MCRDOIParser().parse(doi);
        return generatedDOI
            .orElseThrow(() -> new MCRConfigurationException("The property " + doi + " is not a valid doi!"));
    }

}
