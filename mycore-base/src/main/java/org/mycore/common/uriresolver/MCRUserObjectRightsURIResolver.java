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

package org.mycore.common.uriresolver;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.xml.MCRDOMUtils;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * {@link URIResolver} that checks object access rights and returns user or role information as XML.
 */
public class MCRUserObjectRightsURIResolver implements URIResolver {

    /**
     * Resolves the given query and returns the result as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{function}:{argument}
     * </pre>
     * <p>Supported functions:
     * <ul>
     *   <li>{@code isWorldReadable} – whether the object is world-readable</li>
     *   <li>{@code isWorldReadableComplete} – whether the object is completely world-readable</li>
     *   <li>{@code isDisplayedEnabledDerivate} – whether the derivate has display permission</li>
     *   <li>{@code isCurrentUserInRole} – whether the current user is in the given role</li>
     *   <li>{@code isCurrentUserSuperUser} – whether the current user is the superuser (argument unused)</li>
     *   <li>{@code isCurrentUserGuestUser} – whether the current user is the guest user (argument unused)</li>
     *   <li>{@code getCurrentUserAttribute} – returns the given attribute of the current user</li>
     * </ul>
     * <p>Example request:
     * <pre>
     *   userobjectrights:isWorldReadable:mcr_document_00000001
     *   userobjectrights:isCurrentUserInRole:admin
     *   userobjectrights:getCurrentUserAttribute:eMail
     * </pre>
     * <p>Example response for boolean functions:
     * <pre>{@code
     *   <boolean>true</boolean>
     * }</pre>
     * <p>Example response for {@code getCurrentUserAttribute}:
     * <pre>{@code
     *   <userattribute name="getCurrentUserAttribute">example@mycore.de</userattribute>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the result element
     * @throws TransformerException if the function name is unknown or the DOM document cannot be created
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String query = href.substring(href.indexOf(':') + 1);

        String key = query.substring(0, query.indexOf(':'));
        String value = query.substring(query.indexOf(':') + 1);

        try {
            return switch (key) {
                case "isWorldReadable" -> MCRURIResolverResponse.ofBoolean(MCRXMLFunctions.isWorldReadable(value));
                case "isWorldReadableComplete" ->
                    MCRURIResolverResponse.ofBoolean(MCRXMLFunctions.isWorldReadableComplete(value));
                case "isDisplayedEnabledDerivate" ->
                    MCRURIResolverResponse.ofBoolean(MCRAccessManager.checkDerivateDisplayPermission(value));
                case "isCurrentUserInRole" -> MCRURIResolverResponse.ofBoolean(MCRSessionMgr
                    .getCurrentSession().getUserInformation().isUserInRole(value));
                case "isCurrentUserSuperUser" -> MCRURIResolverResponse.ofBoolean(MCRSessionMgr.getCurrentSession()
                    .getUserInformation().getUserID().equals(MCRSystemUserInformation.SUPER_USER.getUserID()));
                case "isCurrentUserGuestUser" -> MCRURIResolverResponse.ofBoolean(MCRSessionMgr.getCurrentSession()
                    .getUserInformation().getUserID().equals(MCRSystemUserInformation.GUEST.getUserID()));
                case "getCurrentUserAttribute" -> {
                    Document doc = MCRDOMUtils.getDocumentBuilder().newDocument();
                    Element attr = doc.createElement("userattribute");
                    attr.setAttribute("name", key);
                    doc.appendChild(attr);
                    attr.appendChild(
                        doc.createTextNode(
                            MCRSessionMgr.getCurrentSession().getUserInformation().getUserAttribute(value)));
                    yield new DOMSource(doc);
                }
                default -> throw new TransformerException("Unknown query for MCRUserObjectRightsResolver: " + query);
            };

        } catch (ParserConfigurationException e) {
            throw new TransformerException("Error creating DOM document for " + href, e);
        }
    }

}
