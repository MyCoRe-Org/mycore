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

package org.mycore.access.uriresolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;

public class MCRCheckPermissionChainURIResolver implements URIResolver {

    /**
     * Checks whether the current user holds the given permission and, if so, resolves the URI.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;id&gt;:&lt;permission&gt;:&lt;uri&gt;
     * </pre>
     * <p>If {@code id} is blank, the permission is checked globally via
     * {@link MCRAccessManager#checkPermission(String)}; otherwise it is checked
     * for the given object ID via {@link MCRAccessManager#checkPermission(String, String)}.
     * <p>Example request:
     * <pre>
     *   checkPermissionChain:mcr_document_00000001:read:mcrobject:mcr_document_00000001
     *   checkPermissionChain::administrate:mcrobject:mcr_document_00000001
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return the resolved {@link Source} of the target URI if access is granted
     * @throws TransformerException if the current user does not have the required permission
     * @throws IllegalArgumentException if the URI does not match the expected syntax
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
