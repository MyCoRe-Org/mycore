/*
 * 
 * $Revision: 13085 $ $Date: 02.02.2012 22:25:14 $
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
package org.mycore.user2.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRLabelTransformer;
import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.user2.MCRGroup;
import org.mycore.user2.MCRGroupManager;
import org.mycore.user2.MCRUser;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRUserTransformer {

    /**
     * Builds an xml element containing basic information on user. 
     * This includes user ID, login name and realm.
     */
    public static Element buildBasicXML(MCRUser mcrUser) {
        Element userElement = new Element("user");
        userElement.setAttribute("name", mcrUser.getUserName());
        Element realmElement = new Element("realm");
        realmElement.setAttribute("id", mcrUser.getRealmID());
        realmElement.setText(mcrUser.getRealm().getLabel());
        userElement.addContent(realmElement);
        Element ownerElement = new Element("owner");
        ownerElement.setAttribute("id", String.valueOf(mcrUser.getOwner().getUserID()));
        userElement.addContent(ownerElement);
        return userElement;
    }

    /**
     * Builds an xml element containing detailed information on user. 
     * This includes user data, owned users and groups the user is member of.
     */
    public static Element buildXML(MCRUser mcrUser) throws Exception {
        Element userElement = buildBasicXML(mcrUser);

        addString(mcrUser, userElement, "realName", mcrUser.getRealName());
        addString(mcrUser, userElement, "eMail", mcrUser.getEMailAddress());
        addString(mcrUser, userElement, "hint", mcrUser.getHint());

        if (mcrUser.getLastLogin() != null) {
            addString(mcrUser, userElement, "lastLogin", MCRXMLFunctions.getISODate(mcrUser.getLastLogin(), MCRISO8601Format.F_COMPLETE_HH_MM_SS));
        }

        MCRUser owner = mcrUser.getOwner();
        if (owner != null) {
            Element o = userElement.getChild("owner");
            o.addContent(MCRUserTransformer.buildBasicXML(owner));
        }

        List<MCRUser> owns = mcrUser.getOwnedUsers();
        if (owns.size() > 0) {
            Element o = new Element("owns");
            for (MCRUser owned : owns)
                o.addContent(MCRUserTransformer.buildBasicXML(owned));
            userElement.addContent(o);
        }

        Collection<MCRGroup> groups = new LinkedList<MCRGroup>();
        groups.addAll(MCRGroupManager.getGroups(mcrUser.getSystemGroupIDs()));
        groups.addAll(MCRGroupManager.getGroups(mcrUser.getExternalGroupIDs()));

        if (mcrUser.getSystemGroupIDs().size() > 0 || mcrUser.getExternalGroupIDs().size() > 0) {
            Element groupsElement = new Element("groups");
            for (MCRGroup group : groups) {
                Element groupElement = new Element("group");
                groupElement.setAttribute("name", group.getName());
                for (MCRLabel label : group.getLabels()) {
                    groupElement.addContent(MCRLabelTransformer.getElement(label));
                }
                groupsElement.addContent(groupElement);
            }
            userElement.addContent(groupsElement);
        }
        return userElement;
    }

    private static void addString(MCRUser mcrUser, Element parent, String name, String value) {
        if ((value != null) && (value.trim().length() > 0))
            parent.addContent(new Element(name).setText(value.trim()));
    }

}
