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

import java.util.ArrayList;
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
import org.mycore.orcid2.auth.MCRORCIDOAuthClient;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.validation.MCRORCIDValidationHelper;
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

    public static final String ATTR_ORCID_ID = ATTR_ID_PREFIX + "orcid";

    private final MCRUser user;

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
     * @throws MCRORCIDException if ORCID iD is invalid
     */
    public void addORCID(String orcid) throws MCRORCIDException {
        if (!MCRORCIDValidationHelper.validateORCID(orcid)) {
            throw new MCRORCIDException("Invalid ORCID iD");
        }
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
     * @throws MCRORCIDException if crededentials are invalid
     * @see MCRORCIDUser#addORCID
     */
    public void storeCredentials(MCRORCIDCredentials credentials) throws MCRORCIDException {
        final String orcid = credentials.getORCID();
        addORCID(orcid);
        if (!MCRORCIDValidationHelper.validateCredentials(credentials)) {
            throw new MCRORCIDException("Credentials are invalid");
        }
        String credentialsString = null;
        try {
            credentialsString = serializeCredentials(credentials);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credentials are invalid");
        }
        user.setUserAttribute(ATTR_ORCID_CREDENTIALS + orcid, credentialsString);
        MCRUserManager.updateUser(user);
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
    }

    /**
     * Removes MCROCIDCredentials by orcid if exists.
     * 
     * @param orcid the orcid
     */
    public void removeCredentials(String orcid) {
        final SortedSet<MCRUserAttribute> attributes = user.getAttributes();
        final SortedSet<MCRUserAttribute> toKeep = new TreeSet<MCRUserAttribute>();
        for (MCRUserAttribute attribute : attributes) {
            if (!attribute.getName().equals(ATTR_ORCID_CREDENTIALS + orcid)) {
                toKeep.add(attribute);
            }
        }
        user.setAttributes(toKeep); // because of hibernate issues
        MCRUserManager.updateUser(user);
    }

    /**
     * Checks if user has MCRORCIDCredentials.
     * 
     * @return true if user has credentials
     */
    public boolean hasCredentials() {
        return user.getAttributes().stream()
            .filter(attribute -> attribute.getName().startsWith(ATTR_ORCID_CREDENTIALS)).findAny().isPresent();
    }

    /** 
     * Gets user's MCRORCIDCredentials from user attributes.
     * 
     * @return List of credentials
     * @throws MCRORCIDException if the are corrupt credentials
     */
    public List<MCRORCIDCredentials> listCredentials() throws MCRORCIDException {
        final List<MCRUserAttribute> attributes = user.getAttributes().stream()
            .filter(attribute -> attribute.getName().startsWith(ATTR_ORCID_CREDENTIALS)).toList();
        if (attributes.isEmpty()) {
            return List.of();
        }
        final List<MCRORCIDCredentials> credentials = new ArrayList<MCRORCIDCredentials>();
        for (MCRUserAttribute attribute : attributes) {
            final MCRORCIDCredentials tmp = deserializeCredentials(attribute.getValue());
            final String orcid = attribute.getName().substring(ATTR_ORCID_CREDENTIALS.length());
            tmp.setORCID(orcid);
        }
        return credentials;
    }

    /**
     * Gets user's MCRORCIDCredentials by orcid.
     * 
     * @param orcid the orcid
     * @return credentials or null
     * @throws MCRORCIDException if the credentials are corrupt
     */
    public MCRORCIDCredentials getCredentials(String orcid) throws MCRORCIDException {
        final String credentialsString = user.getUserAttribute(ATTR_ORCID_CREDENTIALS + orcid);
        if (credentialsString == null) {
            return null;
        }
        MCRORCIDCredentials credentials = null;
        try {
            credentials = deserializeCredentials(credentialsString);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credentials are corrupt");
        }
        credentials.setORCID(orcid);
        return credentials;
    }

    /**
     * Revokes orcid access token by orcid.
     * 
     * @param orcid the orcid
     * @throws MCRORCIDException if revoke request fails
     */
    public void revokeCredentials(String orcid) throws MCRORCIDException {
        final MCRORCIDCredentials credentials = getCredentials(orcid);
        if (credentials == null) {
            throw new MCRORCIDException("Credentials do not exist.");
        }
        MCRORCIDOAuthClient.getInstance().revokeToken(credentials.getAccessToken());
        removeCredentials(orcid);
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
     * @throws IllegalArgumentException if serialization fails
     */
    protected static String serializeCredentials(MCRORCIDCredentials credentials) throws IllegalArgumentException {
        try {
            final MCRORCIDCredentials cloned = (MCRORCIDCredentials) credentials.clone();
            cloned.setExpiresIn(null);
            cloned.setName(null);
            cloned.setORCID(null);
            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            return mapper.writeValueAsString(cloned);
        } catch (JsonProcessingException | CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Deserializes Stirng to MCRORCIDCredentials.
     * 
     * @param credentialsString credentials as String
     * @return MCRORCIDCredentials
     * @throws IllegalArgumentException if deserialisation fails
     */
    protected static MCRORCIDCredentials deserializeCredentials(String credentialsString)
        throws IllegalArgumentException {
        try {
            return new ObjectMapper().readValue(credentialsString, MCRORCIDCredentials.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
