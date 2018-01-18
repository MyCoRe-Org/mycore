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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierMetadataManager;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.purl.MCRPersistentUniformResourceLocator;

public class MCRMODSPURLPersistentIdentifierMetadataManager
    extends MCRPersistentIdentifierMetadataManager<MCRPersistentUniformResourceLocator> {

    private static final String MODS_IDENTIFIER_TYPE_PURL = "mods:identifier[@type='purl']";

    public MCRMODSPURLPersistentIdentifierMetadataManager(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRPersistentUniformResourceLocator identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(obj);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setElement("identifier", "type", "purl", identifier.asString())
            .orElseThrow(() -> new MCRException("Could not insert purl into mods document!"));
    }

    @Override
    public void removeIdentifier(MCRPersistentUniformResourceLocator identifier, MCRBase obj, String additional) {
        // not supported
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(obj);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        Element element = wrapper.getElement(MODS_IDENTIFIER_TYPE_PURL);

        if (element == null) {
            return Optional.empty();
        }

        String purlString = element.getTextNormalize();
        try {
            return Optional.of(new MCRPersistentUniformResourceLocator(new URL(purlString)));
        } catch (MalformedURLException e) {
            return Optional.empty();
        }

    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if (!(base instanceof MCRObject)) {
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }
}
