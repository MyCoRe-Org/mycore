/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.basket;

import java.util.List;

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
        if (derivateID != null)
            basket.setDerivateID(derivateID);

        for (Element child : (List<Element>) (xml.getChildren())) {
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
        if (comment != null)
            entry.setComment(comment);

        return entry;
    }
}
