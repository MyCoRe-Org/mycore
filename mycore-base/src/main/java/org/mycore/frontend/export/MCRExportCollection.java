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

package org.mycore.frontend.export;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.basket.MCRBasket;
import org.mycore.frontend.basket.MCRBasketEntry;
import org.mycore.frontend.basket.MCRBasketXMLBuilder;

/**
 * Represents a collection of XML data to export.
 * XML can be added by URI or by JDOM Element, 
 * or the contents of a complete MCRBasket can be added.
 * The collected XML data is wrapped by a root element 
 * thats name and namespace can be set.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRExportCollection {

    /** The collection to export */
    private Element collection;

    private static MCRBasketXMLBuilder basketBuilder = new MCRBasketXMLBuilder(true);

    public MCRExportCollection() {
        collection = new Element("exportCollection");
        new Document(collection);
    }

    /** Sets the name and namespace of the root element that wraps the collected data */
    public void setRootElement(String elementName, String namespaceURI) {
        collection.setName(elementName);
        if ((namespaceURI != null) && (!namespaceURI.isEmpty()))
            collection.setNamespace(Namespace.getNamespace(namespaceURI));
    }

    /**
     * Adds the contents of the given basket.
     */
    public void add(MCRBasket basketOfMODS) throws Exception {
        for (MCRBasketEntry entry : basketOfMODS) {
            collection.addContent(basketBuilder.buildXML(entry));
        }
    }

    /**
     * Adds XML data from the given URI.
     */
    public void add(String uri) {
        add(MCRURIResolver.instance().resolve(uri));
    }

    /**
     * Adds the given XML element, making a clone.
     */
    public void add(Element element) {
        collection.addContent(element.clone());
    }

    /**
     * Returns the collected XML data as MCRJDOMContent.
     */
    public MCRJDOMContent getContent() {
        return new MCRJDOMContent(collection.getDocument());
    }
}
