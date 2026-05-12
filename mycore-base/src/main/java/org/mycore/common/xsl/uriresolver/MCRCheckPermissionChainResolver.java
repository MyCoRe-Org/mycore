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

package org.mycore.common.xsl.uriresolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.access.MCRAccessManager;

public class MCRCheckPermissionChainResolver implements URIResolver {

    /**
     * Checks the permission and if granted resolve the uri
     * <p>
     * Syntax: <code>checkPermissionChain:{?id}:{permission}:{$uri}</code>
     *
     * @param href
     *            URI in the syntax above
     * @param base
     *            not used
     * @return if you have the permission then the resolved uri otherwise an Exception
     * @see javax.xml.transform.URIResolver
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String[] split = href.split(":", 4);

        if (split.length != 4) {
            throw new IllegalArgumentException(
                "Syntax needs to be checkPermissionChain:{?id}:{permission}:{uri} but was " + href);
        }

        final String permission = split[2];

        final String uri = split[3];
        final boolean hasAccess;

        if (!split[1].isBlank()) {
            hasAccess = MCRAccessManager.checkPermission(split[1], permission);
        } else {
            hasAccess = MCRAccessManager.checkPermission(permission);
        }

        if (!hasAccess) {
            throw new TransformerException("No Access to " + uri + " (" + href + " )");
        }

        return MCRURIResolver.obtainInstance().resolve(uri, base);
    }

}
