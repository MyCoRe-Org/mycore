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
