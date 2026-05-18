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

package org.mycore.restapi.v2;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;
import org.mycore.restapi.v2.access.MCRRestAccessManager;

/**
 * {@link URIResolver} that checks whether the current user has permission to access a REST API path.
 */
public class MCRRestCheckAPIAccessResolver implements URIResolver {

    /**
     * Checks the REST API access permission for the given path and returns the result as an XML boolean.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{path}:{permission}
     * </pre>
     * <p>Example request:
     * <pre>
     *   checkrestapiaccess:/api/v2/objects:read
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <boolean>true</boolean>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping an XML element {@code <boolean>} with value {@code true} or {@code false}
     * @throws IllegalArgumentException if the URI does not match the expected syntax
     */
    @Override
    public Source resolve(String href, String base) throws IllegalArgumentException {
        final String[] hrefParts = href.split(":");
        if (hrefParts.length == 3) {
            final String permission = hrefParts[2];
            final MCRAccessInterface acl = MCRAccessManager.getAccessImpl();
            final boolean isPermitted = MCRRestAccessManager.checkRestAPIAccess(acl, permission, hrefParts[1]);
            return MCRURIResolverResponse.ofBoolean(isPermitted);
        }
        throw new IllegalArgumentException("Invalid format of uri for retrieval of checkRestAPIAccess: " + href);
    }
}
