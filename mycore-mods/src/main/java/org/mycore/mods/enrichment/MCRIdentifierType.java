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
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

/**
 * Represents a type of publication identifier, like DOI or ISBN.
 * Each type has a corresponding XPath expression
 * used to locate or build identifiers of this type within MODS.
 *
 * If the corresponding XPath representation is
 * mods:identifier[@type='TYPE'], no explicit configuration is needed.
 *
 * Otherwise, the XPath must be configured, e.g.
 * MCR.MODS.EnrichmentResolver.IdentifierType.shelfmark=mods:location/mods:shelfLocator
 *
 * @author Frank Lützenkirchen
 */
public class MCRIdentifierType {

    private String typeID;

    private String xPath;

    private XPathExpression<Element> xPathExpr;

    MCRIdentifierType(String typeID, String xPath) {
        this.typeID = typeID;
        this.xPath = xPath;
        this.xPathExpr = XPathFactory.instance().compile(xPath, Filters.element(), null,
            MCRConstants.getStandardNamespaces());
    }

    public String getTypeID() {
        return typeID;
    }

    public String getXPath() {
        return xPath;
    }

    /** Returns all identifiers of this type found in the given MODS element. */
    List<MCRIdentifier> getIdentifiers(Element mods) {
        return xPathExpr.evaluate(mods).stream()
            .map(e -> new MCRIdentifier(this, e.getTextTrim()))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "identifier type " + typeID;
    }
}
