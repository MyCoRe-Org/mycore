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
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;

/**
 * {@link URIResolver} to resolve ORCID credential.
 */
public class MCRORCIDCredentialResolver implements URIResolver {

    /**
     * Resolves a custom URI for ORCID credential and returns either a check
     * for the existence of the credential or the credential's scope of current user.
     * The URI must follow the format "orcidCredential:{method}:{orcid}", where:
     * <ul>
     *   <li><b>method</b> is either "exists" or "scope"</li>
     *   <li><b>orcid</b> is the ORCiD of the current user to check</li>
     * </ul>
     *
     * <p>If the method is "exists", the function checks if an credential
     * for current user and the provided ORCiD exists.
     * If it exists, the response is an XML element with "true"; otherwise, "false".</p>
     *
     * <p>If the method is "scope", the function returns the credential's scope if it exists.
     *
     * @param href the URI in the syntax above
     * @param base not used
     * @return the XML result element
     * @throws IllegalArgumentException if the URI format is invalid or the input is invalid
     * @throws TransformerException if an error occurs during the transformation process
     *
     * @see javax.xml.transform.URIResolver
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String[] split = href.split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid format of uri: " + href);
        }
        final String method = split[1];
        final String orcid = split[2];

        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        final MCRORCIDCredential credential = orcidUser.getCredentialByORCID(orcid);

        Element root;
        if (Objects.equals("exists", method)) {
            root = new Element("boolean");
            root.setText(credential == null ? "false" : "true");
        } else if (Objects.equals("scope", method)) {
            if (credential != null) {
                root = new Element("string");
                root.setText(credential.getScope());
            } else {
                throw new IllegalArgumentException("Credential for " + orcid + " does not exist");
            }
        } else {
            throw new IllegalArgumentException("Invalid method: " + method);
        }
        return new JDOMSource(root);
    }
}
