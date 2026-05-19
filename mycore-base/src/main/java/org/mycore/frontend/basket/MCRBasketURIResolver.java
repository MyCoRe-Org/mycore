/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that returns the contents of a basket or a single basket entry as XML.
 */
public class MCRBasketURIResolver implements URIResolver {

    /**
     * Resolves the basket of the given type, or a single entry within it, and returns it as an XML source.
     * <p>If no entry ID is provided, the entire basket is returned. If the entry ID is given but
     * not found in the basket, an empty {@code <entry>} element with the given ID is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{type}
     *   &lt;scheme&gt;:{type}:{id}
     * </pre>
     * <p>Example request:
     * <pre>
     *   basket:objects
     *   basket:objects:mcr_document_00000001
     * </pre>
     * <p>Example response for a single entry:
     * <pre>{@code
     *   <entry id="mcr_document_00000001">
     *     ...
     *   </entry>
     * }</pre>
     * <p>Example response for the entire basket:
     * <pre>{@code
     *   <basket type="objects">
     *     <entry id="mcr_document_00000001">...</entry>
     *   </basket>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping either a single {@code <entry>} or a {@code <basket>} element
     * @throws TransformerException if the basket content cannot be resolved
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        try {
            String[] tokens = href.split(":");
            String type = tokens[1];
            MCRBasket basket = MCRBasketManager.getOrCreateBasketInSession(type);
            if (tokens.length > 2) {
                String id = tokens[2];

                MCRBasketEntry entry = basket.get(id);
                Element xml;
                if (entry == null) {
                    xml = new Element("entry");
                    xml.setAttribute("id", id);
                } else {
                    entry.resolveContent();
                    xml = new MCRBasketXMLBuilder(true).buildXML(entry);
                }
                Document doc = new Document(xml);
                return new JDOMSource(doc);
            } else {
                //resolve entire basket
                MCRBasketXMLBuilder basketXMLBuilder = new MCRBasketXMLBuilder(false);
                Document doc = basketXMLBuilder.buildXML(basket);
                return new JDOMSource(doc);
            }
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }
    }

}
