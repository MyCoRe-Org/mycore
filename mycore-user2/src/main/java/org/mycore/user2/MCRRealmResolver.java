/*
 * $Id$
 * $Revision: 5697 $ $Date: 20.02.2012 $
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
