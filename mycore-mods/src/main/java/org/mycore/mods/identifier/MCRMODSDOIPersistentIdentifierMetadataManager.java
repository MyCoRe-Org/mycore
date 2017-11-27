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

package org.mycore.mods.identifier;

import java.util.Objects;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierMetadataManager;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMODSDOIPersistentIdentifierMetadataManager
    extends MCRPersistentIdentifierMetadataManager<MCRDigitalObjectIdentifier> {

    public MCRMODSDOIPersistentIdentifierMetadataManager(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRDigitalObjectIdentifier identifier, MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setElement("identifier", "type", "doi", identifier.asString())
            .orElseThrow(() -> new MCRException("Could not insert doi into mods document!"));
    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if (!(base instanceof MCRObject)) {
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }

    @Override
    public void removeIdentifier(MCRDigitalObjectIdentifier identifier, MCRBase obj, String additional) {
        // not supported
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        Element element = wrapper.getElement("mods:identifier[@type='doi']");
        if (element == null) {
            return Optional.empty();
        }

        String doiText = element.getTextNormalize();

        return new MCRDOIParser()
            .parse(doiText)
            .filter(Objects::nonNull)
            .map(MCRPersistentIdentifier.class::cast);
    }
}
