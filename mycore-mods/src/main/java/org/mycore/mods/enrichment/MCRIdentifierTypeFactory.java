/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods.enrichment;

import java.text.MessageFormat;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifierTypeFactory {

    private static String DEFAULT_XPATH = "mods:identifier[@type=\"{0}\"]";

    private static MCRIdentifierTypeFactory INSTANCE = new MCRIdentifierTypeFactory();

    public static MCRIdentifierTypeFactory instance() {
        return INSTANCE;
    }

    private MCRCache<String, MCRIdentifierType> id2type = new MCRCache<String, MCRIdentifierType>(30,
        "identifier types");

    private MCRIdentifierTypeFactory() {
    }

    private MCRIdentifierType buildIdentifierType(String typeID) {
        MCRConfiguration config = MCRConfiguration.instance();
        String defaultXPath = MessageFormat.format(DEFAULT_XPATH, typeID);
        String xPath = config.getString("MCR.MODS.EnrichmentResolver.IdentifierType." + typeID, defaultXPath);
        return new MCRIdentifierType(typeID, xPath);
    }

    public MCRIdentifierType getType(String typeID) {
        MCRIdentifierType type = id2type.get(typeID);
        if (type == null) {
            type = buildIdentifierType(typeID);
            id2type.put(typeID, type);
        }
        return type;
    }
}
