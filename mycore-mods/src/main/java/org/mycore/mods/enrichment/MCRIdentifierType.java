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

import java.util.List;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

/**
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifierType {

    private String typeID;

    private XPathExpression<Element> xPath;

    public MCRIdentifierType(String typeID, String xPath) {
        this.typeID = typeID;
        this.xPath = XPathFactory.instance().compile(xPath, Filters.element(), null,
            MCRConstants.getStandardNamespaces());
    }

    public String getTypeID() {
        return typeID;
    }

    public List<Element> findIdentifiers(Element mods) {
        return xPath.evaluate(mods);
    }

    @Override
    public String toString() {
        return "identifier type " + typeID;
    }
}
