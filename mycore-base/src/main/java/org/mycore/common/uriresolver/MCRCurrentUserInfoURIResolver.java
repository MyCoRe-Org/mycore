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

import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.services.http.MCRURLQueryParameter;

/**
 * {@link URIResolver} that returns information about the currently authenticated user as XML.
 */
public class MCRCurrentUserInfoURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves information about the currently authenticated user and returns it as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:attribute={name}[&amp;attribute={name}...][&amp;role={name}...]
     * </pre>
     * <p>Each {@code attribute} parameter adds the corresponding user attribute to the response.
     * Each {@code role} parameter adds a boolean indicating whether the user is in that role.
     * Duplicate parameter values are ignored with a warning.
     * <p>Example request:
     * <pre>
     *   currentUserInfo:attribute=eMail&amp;attribute=realName&amp;role=administrator
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <user id="admin">
     *     <attribute name="eMail">example@mycore.de</attribute>
     *     <attribute name="realName">Administrator</attribute>
     *     <role name="administrator">true</role>
     *   </user>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <user>} element
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        MCRUserInformation userInformation = MCRSessionMgr.getCurrentSession().getUserInformation();
        String userID = userInformation.getUserID();

        String[] split = href.split(":");

        Set<String> suppliedAttributes = new HashSet<>();
        Set<String> suppliedRoles = new HashSet<>();
        Element root = new Element("user");
        root.setAttribute("id", userID);

        if (split.length == 2) {
            String req = split[1];
            MCRURLQueryParameter.parse(req).forEach(nv -> {
                if (nv.name().equals("attribute")) {
                    if (suppliedAttributes.contains(nv.value())) {
                        LOGGER.warn("Duplicate attribute {} in user info request", nv::value);
                        return;
                    }
                    suppliedAttributes.add(nv.value());
                    Element attribute = new Element("attribute");
                    attribute.setAttribute("name", nv.value());
                    attribute.setText(userInformation.getUserAttribute(nv.value()));
                    root.addContent(attribute);
                } else if (nv.name().equals("role")) {
                    if (suppliedRoles.contains(nv.value())) {
                        LOGGER.warn("Duplicate role {} in user info request", nv::value);
                        return;
                    }
                    suppliedRoles.add(nv.value());
                    Element role = new Element("role");
                    role.setAttribute("name", nv.value());
                    role.setText(String.valueOf(userInformation.isUserInRole(nv.value())));
                    root.addContent(role);
                }
            });
        }

        return new JDOMSource(root);
    }

}
