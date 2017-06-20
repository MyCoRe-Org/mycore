/**
 * $Revision: 23345 $ 
 * $Date: 2012-01-30 12:08:41 +0100 (Mo, 30 Jan 2012) $
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * Implements URIResolver for use in editor form user-editor.xml
 *  
 * user:{userID}
 *   returns detailed user data including owned users and groups
 * user:current
 *   returns detailed user data of the user currently logged in
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserResolver implements URIResolver {
    private static Logger LOGGER = LogManager.getLogger(MCRUserResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] hrefParts = href.split(":");
        String userID = hrefParts[1];
        MCRUser user = null;
        try {
            if ("current".equals(userID)) {
                user = MCRUserManager.getCurrentUser();
            } else if ("getOwnedUsers".equals(userID)) {
                return getOwnedUsers(hrefParts[2]);
            } else if ("getAllUsers".equals(userID)) {
                try {
                    return new JDOMSource(getAllUsers());
                } catch (MCRAccessException e) {
                    throw new TransformerException(e);
                }
            } else {
                user = MCRUserManager.getUser(userID);
            }
            if (user == null) {
                return null;
            }
            return new JAXBSource(MCRUserTransformer.JAXB_CONTEXT, user.getSafeCopy());
        } catch (JAXBException e) {
            throw new TransformerException(e);
        }
    }

    @XmlRootElement(name = "owns")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class MCROwns {
        @XmlElement(name = "user")
        MCRUser[] users;
    }

    private Source getOwnedUsers(String userName) throws JAXBException {
        MCRUser owner = MCRUserManager.getUser(userName);
        List<MCRUser> listUsers = MCRUserManager.listUsers(owner);
        MCROwns mcrOwns = new MCROwns();
        int userCount = listUsers.size();
        mcrOwns.users = new MCRUser[userCount];
        for (int i = 0; i < userCount; i++) {
            mcrOwns.users[i] = listUsers.get(i).getBasicCopy();
        }
        return new JAXBSource(MCRUserTransformer.JAXB_CONTEXT, mcrOwns);
    }

    @Deprecated
    public static Document getAllUsers() throws MCRAccessException {
        LOGGER.warn("Please fix https://sourceforge.net/tracker/?func=detail&aid=3497583&group_id=92005&atid=599192");
        if (!MCRAccessManager.checkPermission("modify-user") && !MCRAccessManager.checkPermission("modify-contact")) {
            throw MCRAccessException.missingPrivilege("List all users.", "modify-user", "modify-contact");
        }
        List<MCRUser> users = MCRUserManager.listUsers(null, null, null);
        // Loop over all assignable group IDs
        Element root = new org.jdom2.Element("items");
        for (MCRUser user : users) {
            Element item = new Element("item");
            StringBuilder label = new StringBuilder(user.getUserID());
            item.setAttribute("value", label.toString());
            if (user.getRealName() != null && user.getRealName().length() > 0) {
                label.append(" (").append(user.getRealName()).append(')');
            }
            item.setAttribute("label", label.toString());
            root.addContent(item);
        }
        return new Document(root);
    }
}
