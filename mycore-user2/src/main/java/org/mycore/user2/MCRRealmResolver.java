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

package org.mycore.user2;

import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * Implements URIResolver for realms
 *  
 * realm:{realmID}
 *   returns information about this realm
 * realm:local
 *   returns information about the local realm
 * realm:all
 *   returns all realms  
 * 
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRealmResolver implements URIResolver {

    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        String realmID = href.split(":")[1];
        if ("all".equals(realmID)) {
            return MCRRealmFactory.getRealmsSource();
        } else if ("local".equals(realmID)) {
            realmID = MCRRealmFactory.getLocalRealm().getID();
        }
        return new JDOMSource(getElement(MCRRealmFactory.getRealm(realmID).getID()));
    }

    private Element getElement(final String id) {
        Document realmsDocument = MCRRealmFactory.getRealmsDocument();
        List<Element> realms = realmsDocument.getRootElement().getChildren("realm");
        return realms.stream()
            .filter(realm -> id.equals(realm.getAttributeValue("id")))
            .findAny()
            .orElse(null);
    }

}
