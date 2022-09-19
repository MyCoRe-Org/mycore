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

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRNodeBuilder;

/**
 * Represents a publication's identifier like DOI or ISBN
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifier {

    private MCRIdentifierType type;

    private String value;

    MCRIdentifier(MCRIdentifierType type, String value) {
        this.type = type;
        this.value = value;
    }

    MCRIdentifierType getType() {
        return type;
    }

    String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof MCRIdentifier && this.toString().equals(other.toString()));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return type.getTypeID() + " " + value;
    }

    /**
     * Builds the XML representation of this identifier within MODS.
     */
    void buildElement(Element parent) {
        MCRNodeBuilder builder = new MCRNodeBuilder();
        try {
            builder.buildElement(type.getXPath(), value, parent);
        } catch (JaxenException ex) {
            throw new MCRException(ex);
        }
    }
}
