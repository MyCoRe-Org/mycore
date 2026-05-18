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

package org.mycore.orcid2.util;

import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;

/**
 * {@link URIResolver} that returns ORCID credential information for the current user as XML.
 */
public class MCRORCIDCredentialResolver implements URIResolver {

    /**
     * Resolves ORCID credential information for the given ORCID and returns the result as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{method}:{orcid}
     * </pre>
     * <p>Supported methods:
     * <ul>
     *   <li>{@code exists}: whether a credential for the current user and given ORCID exists</li>
     *   <li>{@code scope}: the scope of the existing credential for the current user and given ORCID</li>
     * </ul>
     * <p>Example request:
     * <pre>
     *   orcidCredential:exists:0000-0000-0000-0001
     *   orcidCredential:scope:0000-0000-0000-0001
     * </pre>
     * <p>Example response for {@code exists}:
     * <pre>{@code
     *   <boolean>true</boolean>
     * }</pre>
     * <p>Example response for {@code scope}:
     * <pre>{@code
     *   <string>/authenticate /read-limited</string>
     * }</pre>
     *
     * @param href the URI to resolve; split by {@code :} into scheme, method, and ORCID
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the result element
     * @throws IllegalArgumentException if the URI format is invalid, the method is unknown,
     *                                  or {@code scope} is requested but no credential exists
     */
    @Override
    public Source resolve(String href, String base) {
        final String[] split = href.split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid format of uri: " + href);
        }
        final String method = split[1];
        final String orcid = split[2];

        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final MCRORCIDCredential credential = orcidUser.getCredentialByORCID(orcid);

        if (Objects.equals("exists", method)) {
            return MCRURIResolverResponse.ofBoolean(credential != null);
        } else if (Objects.equals("scope", method)) {
            if (credential != null) {
                return MCRURIResolverResponse.ofString(credential.getScope());
            } else {
                throw new IllegalArgumentException("Credential for " + orcid + " does not exist");
            }
        } else {
            throw new IllegalArgumentException("Invalid method: " + method);
        }
    }

}
