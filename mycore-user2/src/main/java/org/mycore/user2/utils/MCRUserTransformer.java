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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRLabelTransformer;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.user2.MCRGroup;
import org.mycore.user2.MCRGroupManager;
import org.mycore.user2.MCRPasswordHashType;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * @author Thomas Scheffler
 *
 */
public abstract class MCRUserTransformer {

    private static final String USER_ELEMENT_NAME = "user";

    private MCRUserTransformer() {
    }

    /**
     * Builds an xml element containing basic information on user. 
     * This includes user ID, login name and realm.
     */
    public static Element buildBasicXML(MCRUser mcrUser) {
        Element userElement = new Element(USER_ELEMENT_NAME);
        userElement.setAttribute("name", mcrUser.getUserName());
        Element realmElement = new Element("realm");
        realmElement.setAttribute("id", mcrUser.getRealmID());
        realmElement.setText(mcrUser.getRealm().getLabel());
        userElement.addContent(realmElement);
        if (mcrUser.getOwner() != null) {
            Element ownerElement = new Element("owner");
            ownerElement.setAttribute("name", String.valueOf(mcrUser.getOwner().getUserID()));
            ownerElement.setAttribute("realm", String.valueOf(mcrUser.getOwner().getRealmID()));
            userElement.addContent(ownerElement);
        }
        return userElement;
    }

    /**
     * Builds an xml element containing detailed information on user. 
     * This includes user data, owned users and groups the user is member of.
     */
    public static Element buildXML(MCRUser mcrUser) {
        Element userElement = buildExportableSafeXML(mcrUser);
        MCRUser owner = mcrUser.getOwner();
        if (owner != null) {
            Element o = userElement.getChild("owner");
            o.addContent(MCRUserTransformer.buildBasicXML(owner));
        }
        return userElement;
    }

    /**
     * Builds an xml element containing all information on the given user except password info.
     * same as {@link #buildXML(MCRUser)} without owned users resolved
     */
    public static Element buildExportableSafeXML(MCRUser mcrUser) {
        boolean userExists = MCRUserManager.exists(mcrUser.getUserName(), mcrUser.getRealmID());
        Element userElement = buildBasicXML(mcrUser);

        addString(mcrUser, userElement, "realName", mcrUser.getRealName());
        addString(mcrUser, userElement, "eMail", mcrUser.getEMailAddress());
        addString(mcrUser, userElement, "hint", mcrUser.getHint());

        Date lastLogin = mcrUser.getLastLogin();
        if (lastLogin != null) {
            addString(mcrUser, userElement, "lastLogin", MCRXMLFunctions.getISODate(lastLogin, MCRISO8601Format.F_COMPLETE_HH_MM_SS));
        }
        Date validUntil = mcrUser.getValidUntil();
        if (validUntil != null) {
            addString(mcrUser, userElement, "validUntil", MCRXMLFunctions.getISODate(validUntil, MCRISO8601Format.F_COMPLETE_HH_MM_SS));
        }

        Element ownsElement = new Element("owns");
        if (userExists) {
            List<MCRUser> owns = mcrUser.getOwnedUsers();
            if (owns.size() > 0) {
                for (MCRUser owned : owns) {
                    ownsElement.addContent(MCRUserTransformer.buildBasicXML(owned));
                }
                userElement.addContent(ownsElement);
            }
        } else {
            //special case user owns himself
            if (mcrUser.getOwner().equals(mcrUser)) {
                ownsElement.addContent(MCRUserTransformer.buildBasicXML(mcrUser));
            }
            userElement.addContent(ownsElement);
        }

        Collection<MCRGroup> groups = new LinkedList<MCRGroup>();
        groups.addAll(MCRGroupManager.getGroups(mcrUser.getSystemGroupIDs()));
        groups.addAll(MCRGroupManager.getGroups(mcrUser.getExternalGroupIDs()));

        if (!groups.isEmpty()) {
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
        Map<String, String> userAttrs = mcrUser.getAttributes();
        if (!userAttrs.isEmpty()) {
            Element attributesElement = new Element("attributes");
            userElement.addContent(attributesElement);
            for (Map.Entry<String, String> entry : userAttrs.entrySet()) {
                Element attributeElement = new Element("attribute");
                attributesElement.addContent(attributeElement);
                attributeElement.setAttribute("name", entry.getKey());
                attributeElement.setAttribute("value", entry.getValue());
            }
        }
        return userElement;
    }

    /**
     * Builds an xml element containing all information on the given user.
     * same as {@link #buildExportableSafeXML(MCRUser)} but with password info if available
     */
    public static Element buildExportableXML(MCRUser mcrUser) {
        Element userElement = buildExportableSafeXML(mcrUser);
        Element pwdElement = new Element("password");
        if (setAttribute(pwdElement, "salt", mcrUser.getSalt()) | setAttribute(pwdElement, "hashType", mcrUser.getHashType().toString())
                | setAttribute(pwdElement, "hash", mcrUser.getPassword())) {
            userElement.addContent(pwdElement);
        }
        return userElement;
    }

    /**
     * Builds an MCRUser instance from the given element.
     * @param element as generated by {@link #buildExportableXML(MCRUser)}. 
     */
    public static MCRUser buildMCRUser(Element element) {
        if (!element.getName().equals(USER_ELEMENT_NAME)) {
            throw new IllegalArgumentException("Element is not a mycore user element.");
        }
        //base information
        String userName = element.getAttributeValue("name");
        String realmID = element.getChild("realm").getAttributeValue("id");
        MCRUser mcrUser = new MCRUser(userName, realmID);
        //login allowed?
        String validUntilText = element.getChildTextNormalize("validUntil");
        if (validUntilText != null) {
            Date validUntil = getDateFromISOString(validUntilText);
            mcrUser.setValidUntil(validUntil);
        }
        //owner
        Element ownerElement = element.getChild("owner");
        if (ownerElement != null) {
            String ownerName = ownerElement.getAttributeValue("name");
            String ownerRealm = ownerElement.getAttributeValue("realm");
            if (userName.equals(ownerName) && realmID.equals(ownerRealm)) {
                mcrUser.setOwner(mcrUser);
            } else {
                MCRUser owner = MCRUserManager.getUser(ownerName, ownerRealm);
                if (owner == null) {
                    throw new MCRException("Could not load owning user: " + ownerName + "@" + ownerRealm);
                }
                mcrUser.setOwner(owner);
            }
        }
        //realName
        String realName = element.getChildTextTrim("realName");
        mcrUser.setRealName(realName);
        //eMail
        String eMail = element.getChildTextTrim("eMail");
        mcrUser.setEMail(eMail);
        //hint
        String hint = element.getChildTextNormalize("hint");
        mcrUser.setHint(hint);
        //lastLogin
        String lastLoginText = element.getChildTextNormalize("lastLogin");
        if (lastLoginText != null) {
            Date lastLogin = getDateFromISOString(lastLoginText);
            mcrUser.setLastLogin(lastLogin);
        }
        //groups
        Element groups = element.getChild("groups");
        if (groups != null) {
            @SuppressWarnings("unchecked")
            List<Element> groupElements = groups.getChildren("group");
            for (Element group : groupElements) {
                String groupName = group.getAttributeValue("name");
                mcrUser.addToGroup(groupName);
            }
        }
        //attributes
        Element attributes = element.getChild("attributes");
        if (attributes != null) {
            @SuppressWarnings("unchecked")
            List<Element> attributeElements = attributes.getChildren("attribute");
            for (Element attribute : attributeElements) {
                String key = attribute.getAttributeValue("name");
                String value = attribute.getAttributeValue("value");
                mcrUser.getAttributes().put(key, value);
            }
        }
        //password
        Element password = element.getChild("password");
        if (password != null) {
            String salt = password.getAttributeValue("salt");
            String hashTypeText = password.getAttributeValue("hashType");
            String hash = password.getAttributeValue("hash");
            if (hashTypeText != null) {
                MCRPasswordHashType hashType = MCRPasswordHashType.valueOf(hashTypeText);
                mcrUser.setHashType(hashType);
            }
            mcrUser.setSalt(salt);
            mcrUser.setPassword(hash);
        }
        //finished
        return mcrUser;
    }

    public static Date getDateFromISOString(String isoString) {
        MCRISO8601Date dateParser = new MCRISO8601Date(isoString);
        Date validUntil = dateParser.getDate();
        return validUntil;
    }

    private static boolean setAttribute(Element elem, String name, String value) {
        if (value == null) {
            return false;
        }
        elem.setAttribute(name, value);
        return true;
    }

    private static void addString(MCRUser mcrUser, Element parent, String name, String value) {
        if ((value != null) && (value.trim().length() > 0)) {
            parent.addContent(new Element(name).setText(value.trim()));
        }
    }

}
