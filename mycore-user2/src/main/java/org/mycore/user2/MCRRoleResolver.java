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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRMissingPrivilegeException;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * {@link URIResolver} that provides role-related information for the current user as XML.
 */
public class MCRRoleResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    public static Element getAssignableGroupsForUser() throws MCRAccessException {
        LOGGER.warn("Please fix http://sourceforge.net/p/mycore/bugs/568/");
        List<MCRRole> groupIDs;

        // The list of assignable groups depends on the privileges of the
        // current user.
        if (MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION)) {
            groupIDs = MCRRoleManager.listSystemRoles();
        } else if (MCRAccessManager.checkPermission(MCRUser2Constants.USER_CREATE_PERMISSION)) {
            final MCRUser currentUser = MCRUserManager.getCurrentUser();
            groupIDs = new ArrayList<>(currentUser.getSystemRoleIDs().size());
            for (final String id : currentUser.getSystemRoleIDs()) {
                groupIDs.add(MCRRoleManager.getRole(id));
            }
        } else {
            throw new MCRMissingPrivilegeException("List asignable groups for new user.",
                MCRUser2Constants.USER_ADMIN_PERMISSION, MCRUser2Constants.USER_CREATE_PERMISSION);
        }

        // Loop over all assignable groups
        final Element root = new Element("items");

        for (final MCRRole group : groupIDs) {
            String label = group.getName();
            MCRLabel groupLabel = group.getLabel();
            if (groupLabel != null && groupLabel.getText() != null) {
                label = groupLabel.getText();
            }
            final Element item = new Element("item").setAttribute("value", group.getName()).setAttribute("label",
                label);
            root.addContent(item);
        }
        return root;
    }

    /**
     * Resolves the given role query and returns the result as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{method}
     * </pre>
     * <p>Supported methods:
     * <ul>
     *   <li>{@code getAssignableGroupsForUser} – returns all roles assignable by the current user,
     *       depending on their privileges</li>
     * </ul>
     * <p>Example request:
     * <pre>
     *   group:getAssignableGroupsForUser
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <items>
     *     <item value="admins" label="Administrators"/>
     *     <item value="editors" label="Editors"/>
     *   </items>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the result element
     * @throws TransformerException if the method is unknown or the current user lacks the required privileges
     */
    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        final String target = href.substring(href.indexOf(':') + 1);
        final String[] part = target.split(":");
        final String method = part[0];
        try {
            if (Objects.equals(method, "getAssignableGroupsForUser")) {
                return new JDOMSource(getAssignableGroupsForUser());
            }
        } catch (final MCRAccessException exc) {
            throw new TransformerException(exc);
        }
        throw new TransformerException(new IllegalArgumentException("Unknown method " + method + " in uri " + href));
    }

}
