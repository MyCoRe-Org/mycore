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

package org.mycore.mods.enrichment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Builds identifiers types as configured in mycore.properties.
 *
 * If the corresponding XPath representation is
 * mods:identifier[@type='TYPE'], no explicit configuration is needed.
 *
 * Otherwise, the XPath must be configured, e.g.
 * MCR.MODS.EnrichmentResolver.IdentifierType.shelfmark=mods:location/mods:shelfLocator
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifierTypeFactory {

    private static String DEFAULT_XPATH = "mods:identifier[@type=\"%s\"]";

    private static MCRIdentifierTypeFactory INSTANCE = new MCRIdentifierTypeFactory();

    private Map<String, MCRIdentifierType> id2type = new HashMap<>();

    static MCRIdentifierTypeFactory instance() {
        return INSTANCE;
    }

    private MCRIdentifierTypeFactory() {
    }

    private MCRIdentifierType buildIdentifierType(String typeID) {
        String configProperty = "MCR.MODS.EnrichmentResolver.IdentifierType." + typeID;
        String defaultXPath = String.format(Locale.ROOT, DEFAULT_XPATH, typeID);
        String xPath = MCRConfiguration2.getString(configProperty).orElse(defaultXPath);
        return new MCRIdentifierType(typeID, xPath);
    }

    /** Returns the identifier type with the given ID, e.g. DOI or ISBN */
    MCRIdentifierType getType(String typeID) {
        MCRIdentifierType type = id2type.get(typeID);
        if (type == null) {
            type = buildIdentifierType(typeID);
            id2type.put(typeID, type);
        }
        return type;
    }

    /** Returns all identifier types used or configured so far */
    Collection<MCRIdentifierType> getTypes() {
        return id2type.values();
    }
}
