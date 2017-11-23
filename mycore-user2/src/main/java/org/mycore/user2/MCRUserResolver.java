/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

}
