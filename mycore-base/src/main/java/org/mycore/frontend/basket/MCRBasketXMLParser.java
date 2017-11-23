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

package org.mycore.frontend.basket;

import org.jdom2.Document;
import org.jdom2.Element;

/**
 * Parses XML representations of baskets and their entries.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketXMLParser {

    /**
     * Parses an XML document representing a basket. 
     */
    public MCRBasket parseXML(Document doc) {
        Element xml = doc.getRootElement();

        String type = xml.getAttributeValue("type");
        MCRBasket basket = new MCRBasket(type);

        String derivateID = xml.getAttributeValue("id");
        if (derivateID != null) {
            basket.setDerivateID(derivateID);
        }

        for (Element child : xml.getChildren()) {
            MCRBasketEntry entry = parseXML(child);
            basket.add(entry);
        }

        return basket;
    }

    /**
     * Parses an XML element that represents a basket entry.
     */
    public MCRBasketEntry parseXML(Element xml) {
        String id = xml.getAttributeValue("id");
        String uri = xml.getAttributeValue("uri");
        MCRBasketEntry entry = new MCRBasketEntry(id, uri);

        String comment = xml.getChildTextTrim("comment");
        if (comment != null) {
            entry.setComment(comment);
        }

        return entry;
    }
}
