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
import javax.xml.transform.URIResolver;

import org.mycore.access.MCRAccessManager;

/**
 * {@link URIResolver} that checks an ACL permission and returns the result as an XML boolean.
 */
public class MCRCheckPermissionURIResolver implements URIResolver {

    /**
     * Resolves whether the current user holds the given permission and returns the result as an XML boolean.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;permission&gt;
     *   &lt;scheme&gt;:&lt;id&gt;:&lt;permission&gt;
     * </pre>
     * <p>If only a permission is provided, it is checked globally. If an object ID is also
     * provided, the permission is checked for that specific object.
     * <p>Example request:
     * <pre>
     *   checkPermission:administrate
     *   checkPermission:mcr_document_00000001:read
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <boolean>true</boolean>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping an XML element {@code <boolean>} with value {@code true} or {@code false}
     * @throws IllegalArgumentException if the URI does not match either expected syntax
     */
    @Override
    public Source resolve(String href, String base) {
        final String[] split = href.split(":");
        boolean permission = switch (split.length) {
            case 2 -> MCRAccessManager.checkPermission(split[1]);
            case 3 -> MCRAccessManager.checkPermission(split[1], split[2]);
            default -> throw new IllegalArgumentException(
                "Invalid format of uri for retrieval of checkPermission: " + href);
        };
        return MCRURIResolverResponse.ofBoolean(permission);
    }

}
