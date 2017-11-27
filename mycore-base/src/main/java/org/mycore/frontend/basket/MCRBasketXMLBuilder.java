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
 * Builds xml representations of a basket and its entries.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketXMLBuilder {

    /** If true, the XML data representing the objects in basket entries is included too. */
    private boolean addContent;

    /**
     * Creates a new XML builder. 
     * 
     * @param addContent if true, the XML data representing the objects in basket entries is included too.
     */
    public MCRBasketXMLBuilder(boolean addContent) {
        this.addContent = addContent;
    }

    /**
     * Builds an XML representation of a basket entry.
     * Note that setContent() or resolveContent() must have been called before
     * if XML content of the basket entry's object should be included.
     */
    public Element buildXML(MCRBasketEntry entry) {
        Element xml = new Element("entry");
        xml.setAttribute("id", entry.getID());
        xml.setAttribute("uri", entry.getURI());

        if (addContent) {
            Element content = entry.getContent();
            if (content != null)
                xml.addContent(content.clone());
        }

        String comment = entry.getComment();
        if (comment != null)
            xml.addContent(new Element("comment").setText(comment));

        return xml;
    }

    /**
     * Builds an XML representation of a basket and its entries.
     */
    public Document buildXML(MCRBasket basket) {
        Element xml = new Element("basket");
        xml.setAttribute("type", basket.getType());

        String derivateID = basket.getDerivateID();
        if (derivateID != null)
            xml.setAttribute("id", derivateID);

        for (MCRBasketEntry entry : basket)
            xml.addContent(buildXML(entry));
        return new Document(xml);
    }
}
