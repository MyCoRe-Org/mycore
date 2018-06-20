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

package org.mycore.orcid.user;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid.MCRORCIDProfile;
import org.mycore.orcid.oauth.MCRTokenResponse;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.xml.sax.SAXException;

/**
 * Provides functionality to interact with MCRUser that is also an ORCID user.
 * The user's ORCID iD and access token are stored as attributes.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDUser {

    private static final Logger LOGGER = LogManager.getLogger(MCRORCIDUser.class);

    public static final String ATTR_ID_PREFIX = "id_";

    private static final String ATTR_ORCID_ID = ATTR_ID_PREFIX + "orcid";

    private static final String ATTR_ORCID_TOKEN = "token_orcid";

    private MCRUser user;

    private MCRORCIDProfile profile;

    public MCRORCIDUser(MCRUser user) {
        this.user = user;
    }

    public MCRUser getUser() {
        return user;
    }

    public MCRUserStatus getStatus() {
        return new MCRUserStatus(this);
    }

    /** Called from MCROAuthServlet to store the user's ORCID iD and token after successful OAuth authorization */
    public void store(MCRTokenResponse token) {
        user.getAttributes().put(ATTR_ORCID_ID, token.getORCID());
        user.getAttributes().put(ATTR_ORCID_TOKEN, token.getAccessToken());
        MCRUserManager.updateUser(user);
    }

    public String getORCID() {
        return user.getUserAttribute(ATTR_ORCID_ID);
    }

    public String getAccessToken() {
        return user.getUserAttribute(ATTR_ORCID_TOKEN);
    }

    public MCRORCIDProfile getProfile() {
        if ((profile == null) && (getORCID() != null)) {
            String orcid = getORCID();
            String token = getAccessToken();

            profile = new MCRORCIDProfile(orcid);
            if (token != null) {
                profile.setAccessToken(token);
            }
        }
        return profile;
    }

    public MCRPublicationStatus getPublicationStatus(MCRObjectID oid) throws JDOMException, IOException, SAXException {
        return new MCRPublicationStatus(this, oid);
    }

    public boolean isMyPublication(MCRObjectID oid) {
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(oid);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(obj);

        Set<String> nameIdentifierKeys = getNameIdentifierKeys(wrapper);
        Set<String> userIdentifierKeys = getUserIdentifierKeys();
        nameIdentifierKeys.retainAll(userIdentifierKeys);

        if (!nameIdentifierKeys.isEmpty()) {
            for (String key : nameIdentifierKeys) {
                LOGGER.info("user's identifier occurs in publication: " + key);
            }
        }
        return !nameIdentifierKeys.isEmpty();
    }

    private Set<String> getUserIdentifierKeys() {
        Set<String> identifierKeys = new HashSet<String>();
        for (String attribute : user.getAttributes().keySet()) {
            if (attribute.startsWith("id_")) {
                String idType = attribute.substring(3);
                String key = buildNameIdentifierKey(idType, user.getUserAttribute(attribute));
                LOGGER.info("user has name identifier: " + key);
                identifierKeys.add(key);
            }
        }
        return identifierKeys;
    }

    public static Set<String> getNameIdentifierKeys(MCRMODSWrapper wrapper) {
        Set<String> identifierKeys = new HashSet<String>();

        List<Element> nameIdentifiers = wrapper.getElements("mods:name/mods:nameIdentifier");
        for (Element nameIdentifier : nameIdentifiers) {
            String key = buildNameIdentifierKey(nameIdentifier.getAttributeValue("type"), nameIdentifier.getText());
            LOGGER.info("found name identifier in publication: " + key);
            identifierKeys.add(key);
        }
        return identifierKeys;
    }

    private static String buildNameIdentifierKey(String type, String id) {
        return type + ":" + id;
    }

}
