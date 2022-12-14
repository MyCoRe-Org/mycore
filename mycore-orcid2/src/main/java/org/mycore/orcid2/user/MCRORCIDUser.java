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

package org.mycore.orcid2.user;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

/**
 * Provides functionality to interact with MCRUser that is also an ORCID user.
 */
public class MCRORCIDUser {

    /**
     * Id prefix for user attributes.
     */
    public static final String ATTR_ID_PREFIX = "id_";

    /**
     * Prefix for orcid credentials user attribute.
     */
    public static final String ATTR_ORCID_CREDENTIALS = "orcid_credentials_";

    private static final String ATTR_ORCID_ID = ATTR_ID_PREFIX + "orcid";

    private final MCRUser user;

    private MCRORCIDCredentials credentials;

    /**
     * Wraps MCRUser to MCRORCIDUser.
     * 
     * @param user the user
     */
    public MCRORCIDUser(MCRUser user) {
        this.user = user;
    }

    /**
     * Returns MCRUser.
     *
     * @return user
     */
    public MCRUser getUser() {
        return user;
    }

    /**
     * Adds ORCID id to user's user attributes.
     * 
     * @param orcid the orcid id
     */
    public void addORCID(String orcid) {
        final MCRUserAttribute attribute = new MCRUserAttribute(ATTR_ORCID_ID, orcid);
        if (!user.getAttributes().contains(attribute)) { // allow more than one orcid id per user
            user.getAttributes().add(new MCRUserAttribute(ATTR_ORCID_ID, orcid));
            MCRUserManager.updateUser(user);
        }
    }

    /** Returns user's orcid ids.
     * 
     * @return ids as set
     */
    public Set<String> getORCIDs() {
        return user.getAttributes().stream().filter(a -> a.getName().startsWith(ATTR_ORCID_ID))
            .map(MCRUserAttribute::getValue).collect(Collectors.toSet());
    }

    /** 
     * Sets MCRORCIDCredentials to user's user attributes.
     * Also, adds ORCID id to user attributes.
     * 
     * @param credentials the credentials
     * @throws MCRORCIDException if serialization fails
     * @see MCRORCIDUser#addORCID
     */
    public void storeCredentials(MCRORCIDCredentials credentials) throws MCRORCIDException {
        final String orcid = credentials.getORCID();
        addORCID(orcid);
        final String credentialsString = serializeCredentials(credentials);
        user.setUserAttribute(ATTR_ORCID_CREDENTIALS + orcid, credentialsString);
        MCRUserManager.updateUser(user);
        this.credentials = credentials; // cache credentials
    }

    /**
     * Removes all MCRORCIDCredentials attributes if exists.
     */
    public void removeAllCredentials() {
        final SortedSet<MCRUserAttribute> attributes = user.getAttributes();
        final SortedSet<MCRUserAttribute> toKeep = new TreeSet<MCRUserAttribute>();
        for (MCRUserAttribute attribute : attributes) {
            if (!attribute.getName().startsWith(ATTR_ORCID_CREDENTIALS)) {
                toKeep.add(attribute);
            }
        }
        user.setAttributes(toKeep); // because of hibernate issues
        MCRUserManager.updateUser(user);
        this.credentials = null;
    }

    /** 
     * Gets user's MCRORCIDCredentials from user attributes.
     * 
     * @return credentials or null
     * @throws MCRORCIDException if serialization fails or there are more than one credentials
     */
    public MCRORCIDCredentials getCredentials() throws MCRORCIDException {
        if (credentials != null) { // use cached credentials
            return credentials;
        }
        final List<MCRUserAttribute> attributes = user.getAttributes().stream()
            .filter(attribute -> attribute.getName().startsWith(ATTR_ORCID_CREDENTIALS)).toList();
        if (attributes.size() > 1) { // assumption that there is only one credential
            throw new MCRORCIDException("There are more than one credentials.");
        }
        if (attributes.isEmpty()) {
            return null;
        }
        final String credentialsString = attributes.get(0).getValue();
        final MCRORCIDCredentials credentials = deserializeCredentials(credentialsString);
        final String orcid = attributes.get(0).getName().substring(ATTR_ORCID_CREDENTIALS.length());
        credentials.setORCID(orcid);
        return credentials;
    }

    /**
     * Checks if user owns object by user by name identifiers.
     * 
     * @param objectID objects id
     * @return true is user owns object
     */
    public boolean isMyPublication(MCRObjectID objectID) throws MCRPersistenceException {
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
        final Set<String> nameIdentifierKeys = MCRORCIDUtils.getNameIdentifierKeys(new MCRMODSWrapper(object));
        nameIdentifierKeys.retainAll(getIdentifierKeys());
        return !nameIdentifierKeys.isEmpty();
    }

    /**
     * Returns users identifier keys.
     * 
     * @return identifer keys as set
     * @see MCRORCIDUtils#buildIdentifierKey
     */
    public Set<String> getIdentifierKeys() {
        return user.getAttributes().stream().filter(a -> a.getName().startsWith(ATTR_ID_PREFIX))
            .map(a -> MCRORCIDUtils.buildIdentifierKey(a.getName().substring(ATTR_ID_PREFIX.length()), a.getValue()))
            .collect(Collectors.toSet());
    }

    /**
     * Serializes MCRORCIDCredentials to String.
     * 
     * @param credentials credentials
     * @return MCRORCIDCredentials as String
     * @throws MCRORCIDException if serialization fails
     */
    protected static String serializeCredentials(MCRORCIDCredentials credentials) throws MCRORCIDException {
        try {
            final MCRORCIDCredentials cloned = (MCRORCIDCredentials) credentials.clone();
            cloned.setExpiresIn(null);
            cloned.setName(null);
            cloned.setORCID(null);
            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            return mapper.writeValueAsString(cloned);
        } catch (JsonProcessingException | CloneNotSupportedException e) {
            throw new MCRORCIDException("Credentials serialization failed.");
        }
    }

    /**
     * Deserializes Stirng to MCRORCIDCredentials.
     * 
     * @param credentialsString credentials as String
     * @return MCRORCIDCredentials
     * @throws MCRORCIDException if serialization fails
     */
    protected static MCRORCIDCredentials deserializeCredentials(String credentialsString) throws MCRORCIDException {
        try {
            return new ObjectMapper().readValue(credentialsString, MCRORCIDCredentials.class);
        } catch (JsonProcessingException e) {
            throw new MCRORCIDException("Credentials deserialization failed.");
        }
    }
}
