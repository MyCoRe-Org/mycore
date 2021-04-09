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

import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIMetadataService;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Base class for all ModsMetadataServices. Basically in mods just the type is different, which will be resolved from
 * {@link #getIdentifierType()}.
 * <p>
 * This MetadataService has two parameters:
 * <dl>
 * <dt>Prefix</dt>
 * <dd>Will be passed to starts-with() as seconds parameter when the PI is read from the mods document.
 * So it ensures that only the right pi is read. E.g. if only DOI which start with the prefix 10.5072</dd>
 * <dt>Type</dt>
 * <dd>The type will be used in the mods:identifier@type attribute and it will be used to resolve the parser.
 * See: {@link MCRPIManager#getParserForType}</dd>
 * </dl>
 */
public class MCRAbstractMODSMetadataService
    extends MCRPIMetadataService<MCRPersistentIdentifier> {

    public static final String PREFIX_PROPERTY_KEY = "Prefix";

    @Override
    public void insertIdentifier(MCRPersistentIdentifier identifier, MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);

        Element identifierElement = wrapper.getElement(getXPath());
        final String type = getIdentifierType();

        if (identifierElement != null) {
            throw new MCRPersistentIdentifierException(
                type + "with prefix " + getProperties().get(PREFIX_PROPERTY_KEY) + " already exist!");
        }

        identifierElement = new Element("identifier", MCRConstants.MODS_NAMESPACE);
        identifierElement.setAttribute("type", type);
        identifierElement.setText(identifier.asString());

        wrapper.addElement(identifierElement);
    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if (!(base instanceof MCRObject)) {
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }

    @Override
    public void removeIdentifier(MCRPersistentIdentifier identifier, MCRBase obj, String additional) {
        // not supported
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);

        final String xPath = getXPath();
        Element element = wrapper.getElement(xPath);
        if (element == null) {
            return Optional.empty();
        }

        String text = element.getTextNormalize();

        return MCRPIManager.getInstance().getParserForType(getIdentifierType())
            .parse(text)
            .map(MCRPersistentIdentifier.class::cast);
    }

    protected String getIdentifierType() {
        return getProperties().get("Type");
    }

    private String getXPath() {
        final String prefixCondition;

        if (getProperties().containsKey(PREFIX_PROPERTY_KEY)) {
            prefixCondition = " and starts-with(text(), '" + getProperties().get(PREFIX_PROPERTY_KEY) + "')";
        } else {
            prefixCondition = "";
        }

        final String identifierType = getIdentifierType();
        return "mods:identifier[@type='" + identifierType + "'" + prefixCondition + "]";
    }
}
