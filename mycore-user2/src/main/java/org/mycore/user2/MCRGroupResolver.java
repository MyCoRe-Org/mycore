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

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRGroupResolver implements URIResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRGroupResolver.class);

    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String target = href.substring(href.indexOf(":") + 1);
        String[] part = target.split(":");
        String method = part[0];
        try {
            if ("getAssignableGroupsForUser".equals(method)) {
                return new JDOMSource(getAssignableGroupsForUser());
            }
        } catch (MCRAccessException exc) {
            throw new TransformerException(exc);
        }
        throw new TransformerException(new IllegalArgumentException("Unknown method " + method + " in uri " + href));
    }

    public static Element getAssignableGroupsForUser() throws MCRAccessException {
        LOGGER.warn("Please fix https://sourceforge.net/tracker/?func=detail&aid=3497583&group_id=92005&atid=599192");
        List<MCRGroup> groupIDs = null;

        // The list of assignable groups depends on the privileges of the
        // current user.
        try {
            if (MCRAccessManager.checkPermission("administrate-user")) {
                groupIDs = MCRGroupManager.listSystemGroups();
            } else if (MCRAccessManager.checkPermission("create-user")) {
                MCRUser currentUser = MCRUserManager.getCurrentUser();
                groupIDs = new ArrayList<MCRGroup>(currentUser.getSystemGroupIDs().size());
                for (String id : currentUser.getSystemGroupIDs()) {
                    groupIDs.add(MCRGroupManager.getGroup(id));
                }
            } else {
                throw new MCRAccessException("Not enough permissions! " + "Someone might have tried to call the new user form directly.");
            }
        } catch (MCRException exc) {
            throw new MCRAccessException("Not enough permissions", exc);
        }

        // Loop over all assignable groups
        org.jdom.Element root = new org.jdom.Element("items");

        for (MCRGroup group : groupIDs) {
            org.jdom.Element item = new org.jdom.Element("item").setAttribute("value", group.getName()).setAttribute("label", group.getLabel().getText());
            root.addContent(item);
        }
        return root;
    }

}
