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

package org.mycore.restapi.v2;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.restapi.v2.access.MCRRestAPIACLPermission;
import org.mycore.restapi.v2.access.MCRRestAccessManager;

public class MCRRestCheckAPIAccessResolver implements URIResolver {

    /**
     * Checks permission for a given rest api path.
     *
     * Syntax: <code>checkRestAPIAccess:{path}:{permission}</code>
     * 
     * @param href
     *            URI in the syntax above
     * @param base
     *            not used
     * 
     * @return the root element "boolean" of the XML document with content string "true" or "false"
     * @see javax.xml.transform.URIResolver
     */
    @Override
    public Source resolve(final String href, final String base) throws TransformerException, IllegalArgumentException {
        final String[] hrefParts = href.split(":");
        if (hrefParts.length == 3) {
            final String permission = hrefParts[2];
            final MCRRestAPIACLPermission apiPermission = MCRRestAPIACLPermission.resolve(permission);
            if (apiPermission == null) {
                throw new IllegalArgumentException("Unknown permission: " + permission);
            }
            final MCRAccessInterface acl = MCRAccessManager.getAccessImpl();
            final boolean isPermitted = MCRRestAccessManager.checkRestAPIAccess(acl, apiPermission, hrefParts[1]);
            final Element resultElement = new Element("boolean");
            resultElement.setText(Boolean.toString(isPermitted));
            return new JDOMSource(resultElement);
        }
        throw new IllegalArgumentException("Invalid format of uri for retrieval of checkRestAPIAccess: " + href);
    }
}
