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
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.validation.MCRORCIDValidationHelper;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

/**
 * Provides functionality to interact with MCRUser that is also an ORCID user.
 */
public class MCRORCIDUser {

    /**
     * List of trusted name identifier types.
     */
    public static final List<String> TRUSTED_NAME_IDENTIFIER_TYPES
        = MCRConfiguration2.getString(MCRORCIDConstants.CONFIG_PREFIX + "User.TrustedNameIdentifierTypes").stream()
            .flatMap(MCRConfiguration2::splitValue).collect(Collectors.toList());

    /**
     * Id prefix for user attributes.
     */
    public static final String ATTR_ID_PREFIX = "id_";

    /**
     * Prefix for orcid credential user attribute.
     */
    public static final String ATTR_ORCID_CREDENTIALS = "orcid_credential_";

    /**
     * ORCID iD user attribute name.
     */
    public static final String ATTR_ORCID_ID = ATTR_ID_PREFIX + "orcid";

    private final MCRUser user;

    /**
     * Wraps MCRUser to MCRORCIDUser.
     * 
     * @param user the MCRUser
     */
    public MCRORCIDUser(MCRUser user) {
        this.user = user;
    }

    /**
     * Returns MCRUser.
     * 
     * @return MCRUser
     */
    public MCRUser getUser() {
        return user;
    }

    /**
     * Adds ORCID iD to user's user attributes.
     * 
     * @param orcid the ORCID iD
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

    /** Returns user's ORCID iDs.
     * 
     * @return ORCID iDs as set
     */
    public Set<String> getORCIDs() {
        return user.getAttributes().stream().filter(a -> a.getName().startsWith(ATTR_ORCID_ID))
            .map(MCRUserAttribute::getValue).collect(Collectors.toSet());
    }

    /** 
     * Sets MCRORCIDUserCredential to user's MCRUserAttribute.
     * Also, adds ORCID id to user attributes.
     * 
     * @param credential the MCRORCIDUserCredential
     * @throws MCRORCIDException if crededentials are invalid
     * @see MCRORCIDUser#addORCID
     */
    public void storeCredential(MCRORCIDUserCredential credential) throws MCRORCIDException {
        final String orcid = credential.getORCID();
        addORCID(orcid);
        if (!MCRORCIDValidationHelper.validateCredential(credential)) {
            throw new MCRORCIDException("Credentials are invalid");
        }
        String credentialString = null;
        try {
            credentialString = serializeCredential(credential);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credentials are invalid");
        }
        user.setUserAttribute(ATTR_ORCID_CREDENTIALS + orcid, credentialString);
        MCRUserManager.updateUser(user);
    }

    /**
     * Removes all MCRORCIDUserCredential attributes.
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
     * Removes MCROCIDCredentials by ORCID id if exists.
     * 
     * @param orcid the ORCID iD
     */
    public void removeCredentialByORCID(String orcid) {
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
     * Checks if user has MCRORCIDUserCredential.
     * 
     * @return true if user has MCRCredentials
     */
    public boolean hasCredential() {
        return user.getAttributes().stream()
            .filter(attribute -> attribute.getName().startsWith(ATTR_ORCID_CREDENTIALS)).findAny().isPresent();
    }

    /** 
     * Lists user's MCRORCIDUserCredential from user attributes.
     * 
     * @return List of MCRCredentials
     * @throws MCRORCIDException if the are corrupt MCRCredentials
     */
    public List<MCRORCIDUserCredential> listCredentials() throws MCRORCIDException {
        final List<MCRUserAttribute> attributes = user.getAttributes().stream()
            .filter(attribute -> attribute.getName().startsWith(ATTR_ORCID_CREDENTIALS)).toList();
        if (attributes.isEmpty()) {
            return List.of();
        }
        final List<MCRORCIDUserCredential> credentials = new ArrayList<MCRORCIDUserCredential>();
        for (MCRUserAttribute attribute : attributes) {
            final MCRORCIDUserCredential tmp = deserializeCredential(attribute.getValue());
            final String orcid = attribute.getName().substring(ATTR_ORCID_CREDENTIALS.length());
            tmp.setORCID(orcid);
        }
        return credentials;
    }

    /**
     * Gets user's MCRORCIDUserCredential by ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @return MCRCredentials or null
     * @throws MCRORCIDException if the MCRCredentials are corrupt
     */
    public MCRORCIDUserCredential getCredentialByORCID(String orcid) throws MCRORCIDException {
        Optional<MCRORCIDUserCredential> credential = null;
        try {
            credential = Optional.ofNullable(getCredentialAttributeValueByORCID(orcid))
                .map(s -> deserializeCredential(s));
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credentials are corrupt");
        }
        credential.ifPresent(c -> c.setORCID(orcid));
        return credential.orElse(null);
    }

    /**
     * Checks if user owns object by user by name identifiers.
     * 
     * @param objectID objects id
     * @return true is user owns object
     */
    public boolean isMyPublication(MCRObjectID objectID) throws MCRPersistenceException {
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
        final Set<MCRIdentifier> nameIdentifiers
            = MCRORCIDUtils.getNameIdentifiers(new MCRMODSWrapper(object)); // TODO uniqueness of ids
        nameIdentifiers.retainAll(getTrustedIdentifiers());
        return !nameIdentifiers.isEmpty();
    }

    /**
     * Returns users identifiers.
     * 
     * @return Set of MCRIdentifier
     */
    public Set<MCRIdentifier> getIdentifiers() {
        return user.getAttributes().stream().filter(a -> a.getName().startsWith(ATTR_ID_PREFIX))
            .map(a -> new MCRIdentifier(a.getName().substring(ATTR_ID_PREFIX.length()), a.getValue()))
            .collect(Collectors.toSet());
    }

    /**
     * Returns all trusted identifiers.
     * Trusted name identifier type  can be defined as follows:
     *
     * MCR.ORCID2.User.TrustedNameIdentifierTypes=
     * 
     * @return Set of trusted MCRIdentifier
     */
    public Set<MCRIdentifier> getTrustedIdentifiers() {
        return getIdentifiers().stream().filter(i -> TRUSTED_NAME_IDENTIFIER_TYPES.contains(i.getType()))
            .collect(Collectors.toSet());
    }

    /**
     * Serializes MCRORCIDUserCredential to String.
     * 
     * @param credential MCRORCIDUserCredentials
     * @return MCRORCIDUserCredential as String
     * @throws IllegalArgumentException if serialization fails
     */
    protected static String serializeCredential(MCRORCIDUserCredential credential) throws IllegalArgumentException {
        try {
            final MCRORCIDUserCredential cloned = (MCRORCIDUserCredential) credential.clone();
            cloned.setExpiresIn(null);
            cloned.setName(null);
            cloned.setORCID(null);
            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.setSerializationInclusion(Include.NON_NULL);
            return mapper.writeValueAsString(cloned);
        } catch (JsonProcessingException | CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Deserializes String to MCRORCIDUserCredential.
     * 
     * @param credentialString MCRORCIDCredentials as String
     * @return MCRORCIDUserCredential
     * @throws IllegalArgumentException if deserialisation fails
     */
    protected static MCRORCIDUserCredential deserializeCredential(String credentialString)
        throws IllegalArgumentException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(credentialString, MCRORCIDUserCredential.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getCredentialAttributeValueByORCID(String orcid) {
        return user.getUserAttribute(ATTR_ORCID_CREDENTIALS + orcid);
    }
}
