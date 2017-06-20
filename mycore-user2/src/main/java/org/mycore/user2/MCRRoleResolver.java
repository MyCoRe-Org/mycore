/*
 * $Id$
 * $Revision: 5697 $ $Date: 06.03.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2;

import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRoleResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCRRoleResolver.class);

    public static Element getAssignableGroupsForUser() throws MCRAccessException {
        LOGGER.warn("Please fix http://sourceforge.net/p/mycore/bugs/568/");
        List<MCRRole> groupIDs = null;

        // The list of assignable groups depends on the privileges of the
        // current user.
        if (MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION)) {
            groupIDs = MCRRoleManager.listSystemRoles();
        } else if (MCRAccessManager.checkPermission(MCRUser2Constants.USER_CREATE_PERMISSION)) {
            final MCRUser currentUser = MCRUserManager.getCurrentUser();
            groupIDs = new ArrayList<MCRRole>(currentUser.getSystemRoleIDs().size());
            for (final String id : currentUser.getSystemRoleIDs()) {
                groupIDs.add(MCRRoleManager.getRole(id));
            }
        } else {
            throw MCRAccessException.missingPrivilege("List asignable groups for new user.",
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

    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        final String target = href.substring(href.indexOf(":") + 1);
        final String[] part = target.split(":");
        final String method = part[0];
        try {
            if ("getAssignableGroupsForUser".equals(method)) {
                return new JDOMSource(getAssignableGroupsForUser());
            }
        } catch (final MCRAccessException exc) {
            throw new TransformerException(exc);
        }
        throw new TransformerException(new IllegalArgumentException("Unknown method " + method + " in uri " + href));
    }

}
