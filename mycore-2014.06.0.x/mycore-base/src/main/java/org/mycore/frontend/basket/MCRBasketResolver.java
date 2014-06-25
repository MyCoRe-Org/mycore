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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * Resolves entries from a basket, for example to edit the data
 * in an editor form. Syntax: basket:{typeID}:{entryID}
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketResolver implements URIResolver {
    public Source resolve(String href, String base) throws TransformerException {
        try {
            String[] tokens = href.split(":");
            String type = tokens[1];
            MCRBasket basket = MCRBasketManager.getOrCreateBasketInSession(type);
            if (tokens.length > 2) {
                String id = tokens[2];

                MCRBasketEntry entry = basket.get(id);
                entry.resolveContent();
                Element xml = new MCRBasketXMLBuilder(true).buildXML(entry);
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
