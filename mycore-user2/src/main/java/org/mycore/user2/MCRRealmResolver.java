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

package org.mycore.user2;

import java.util.List;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that returns information about one or all configured realms as XML.
 */
public class MCRRealmResolver implements URIResolver {

    /**
     * Resolves the given realm ID and returns the corresponding realm data as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{realmId}
     *   &lt;scheme&gt;:local
     *   &lt;scheme&gt;:all
     * </pre>
     * <p>Example request:
     * <pre>
     *   realm:myRealm
     *   realm:local
     *   realm:all
     * </pre>
     * <p>Example response for a single realm:
     * <pre>{@code
     *   <realm id="myRealm">
     *     ...
     *   </realm>
     * }</pre>
     * <p>Example response for {@code all}:
     * <pre>{@code
     *   <realms>
     *     <realm id="local">...</realm>
     *     <realm id="myRealm">...</realm>
     *   </realms>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping either a single {@code <realm>} element or
     *         the full {@code <realms>} document
     */
    @Override
    public Source resolve(final String href, final String base) {
        String realmID = href.split(":")[1];
        if (Objects.equals(realmID, "all")) {
            return MCRRealmFactory.getRealmsSource();
        } else if (Objects.equals(realmID, "local")) {
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
