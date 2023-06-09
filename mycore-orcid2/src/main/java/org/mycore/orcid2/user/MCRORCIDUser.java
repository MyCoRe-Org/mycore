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
import java.util.Map;
import java.util.Objects;
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
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.validation.MCRORCIDValidationHelper;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;

/**
 * Provides functionality to interact with MCRUser that is also an ORCID user.
 * Handles the updating of user.
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
     * Prefix for ORCID credential user attribute.
     */
    public static final String ATTR_ORCID_CREDENTIAL = "orcid_credential_";

    /**
     * Prefix for ORCID user properties user attribute.
     */
    public static final String ATTR_ORCID_USER_PROPERTIES = "orcid_user_properties_";

    /**
     * ORCID iD user attribute name.
     */
    public static final String ATTR_ORCID_ID = ATTR_ID_PREFIX + "orcid";

    private static final String WORK_EVENT_HANDLER_PROPERTY_PREFIX = "MCR.ORCID2.WorkEventHandler.";

    private static final boolean ALWAYS_UPDATE
        = MCRConfiguration2.getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "AlwaysUpdateWork").orElse(false);

    private static final boolean CREATE_FIRST
        = MCRConfiguration2.getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "CreateFirstWork").orElse(false);

    private static final boolean CREATE_OWN_DUPLICATE
        = MCRConfiguration2.getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "CreateDuplicateWork").orElse(false);

    private static final boolean RECREATE_DELETED
        = MCRConfiguration2.getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "RecreateDeletedWork").orElse(false);

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
    public void addORCID(String orcid) {
        if (!MCRORCIDValidationHelper.validateORCID(orcid)) {
            throw new MCRORCIDException("Invalid ORCID iD");
        }
        final MCRUserAttribute attribute = new MCRUserAttribute(ATTR_ORCID_ID, orcid);
        // allow more than one ORCID iD per user
        if (!user.getAttributes().contains(attribute)) {
            user.getAttributes().add(new MCRUserAttribute(ATTR_ORCID_ID, orcid));
        }
    }

    /** Returns user's ORCID iDs.
     * 
     * @return ORCID iDs as set
     */
    public Set<String> getORCIDs() {
        return user.getAttributes().stream()
            .filter(a -> Objects.equals(a.getName(), ATTR_ORCID_ID))
            .map(MCRUserAttribute::getValue).collect(Collectors.toSet());
    }

    /** 
     * Sets MCRORCIDCredential to user's MCRUserAttribute.
     * Also, adds ORCID iD to user attributes.
     * 
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDException if credential is invalid
     * @see MCRORCIDUser#addORCID
     */
    public void addCredential(String orcid, MCRORCIDCredential credential) {
        addORCID(orcid);
        if (!MCRORCIDValidationHelper.validateCredential(credential)) {
            throw new MCRORCIDException("Credential is invalid");
        }
        String credentialString = null;
        try {
            credentialString = serializeCredential(credential);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credential is invalid");
        }
        user.setUserAttribute(getCredentialAttributeNameByORCID(orcid), credentialString);
    }


    /**
     * Removes all MCRORCIDCredential attributes.
     */
    public void removeAllCredentials() {
        final SortedSet<MCRUserAttribute> attributes = user.getAttributes();
        final SortedSet<MCRUserAttribute> toKeep = new TreeSet<MCRUserAttribute>();
        for (MCRUserAttribute attribute : attributes) {
            if (!attribute.getName().startsWith(ATTR_ORCID_CREDENTIAL)) {
                toKeep.add(attribute);
            }
        }
        // because of Hibernate issues
        user.setAttributes(toKeep);
    }

    /**
     * Removes MCROCIDCredential by ORCID iD if exists.
     * 
     * @param orcid the ORCID iD
     */
    public void removeCredentialByORCID(String orcid) {
        final SortedSet<MCRUserAttribute> attributes = user.getAttributes();
        final SortedSet<MCRUserAttribute> toKeep = new TreeSet<MCRUserAttribute>();
        for (MCRUserAttribute attribute : attributes) {
            if (!attribute.getName().equals(getCredentialAttributeNameByORCID(orcid))) {
                toKeep.add(attribute);
            }
        }
        // because of Hibernate issues
        user.setAttributes(toKeep);
    }

    /**
     * Checks if user has MCRORCIDCredential.
     * 
     * @return true if user has at least one MCRORCIDCredential
     */
    public boolean hasCredentials() {
        return user.getAttributes().stream()
            .filter(attribute -> attribute.getName().startsWith(ATTR_ORCID_CREDENTIAL)).findAny().isPresent();
    }

    /**
     * Checks if user MCRORCIDCredential for ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @return true if user has MCRORCIDCredential for ORCID iD
     */
    public boolean hasCredential(String orcid) {
        return user.getAttributes().stream()
            .filter(attribute -> attribute.getName().equals(getUserPropertiesAttributeNameByORCID(orcid)))
            .findAny().isPresent();
    }

    /** 
     * Returns user's MCRORCIDCredential from user attributes.
     * 
     * @return Map of MCRORCIDCredentials
     * @throws MCRORCIDException if at least one MCRORCIDCredential is corrupt
     */
    public Map<String, MCRORCIDCredential> getCredentials() {
        try {
            return user.getAttributes().stream()
                .filter(a -> a.getName().startsWith(ATTR_ORCID_CREDENTIAL))
                .collect(Collectors.toMap(a -> a.getName().substring(ATTR_ORCID_CREDENTIAL.length()),
                    a -> deserializeCredential(a.getValue())));
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Found corrupt credential", e);
        }
    }

    /**
     * Gets user's MCRORCIDCredential by ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @return MCRCredentials or null
     * @throws MCRORCIDException if the MCRORCIDCredential is corrupt
     */
    public MCRORCIDCredential getCredentialByORCID(String orcid) {
        try {
            return Optional.ofNullable(getCredentialAttributeValueByORCID(orcid))
                .map(s -> deserializeCredential(s)).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credential is corrupt", e);
        }
    }

    /**
     * Checks if user owns object by user by name identifiers.
     * 
     * @param objectID objects id
     * @return true is user owns object
     * @throws MCRORCIDException if check fails
     */
    public boolean isMyPublication(MCRObjectID objectID) {
        MCRObject object = null;
        try {
            object = MCRMetadataManager.retrieveMCRObject(objectID);
        } catch (MCRPersistenceException e) {
            throw new MCRORCIDException("Cannot check publication", e);
        }
        // TODO uniqueness of IDs
        final Set<MCRIdentifier> nameIdentifiers
            = MCRORCIDUtils.getNameIdentifiers(new MCRMODSWrapper(object));
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
     * Returns MCRORCIDUserProperties by ORCID iD.
     * Takes system properties as fallback.
     * 
     * @param orcid the ORCID iD
     * @return MCRORCIDUserProperties
     */
    public MCRORCIDUserProperties getUserPropertiesByORCID(String orcid) {
        try {
            return Optional.ofNullable(getUserPropertiesAttributeValueByORCID(orcid))
                .map(p -> deserializeUserProperties(p))
                .orElse(new MCRORCIDUserProperties(ALWAYS_UPDATE, CREATE_OWN_DUPLICATE, CREATE_FIRST,
                    RECREATE_DELETED));
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Found corrupt user properites", e);
        }
    }

    /**
     * Sets MCRORCIDUserProperties for ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @param userProperties the MCRORCIDUserProperties
     */ 
    public void setUserProperties(String orcid, MCRORCIDUserProperties userProperties) {
        String userPropertiesString = null;
        try {
            userPropertiesString = serializeUserProperties(userProperties);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("User properties are invalid");
        }
        user.setUserAttribute(getUserPropertiesAttributeNameByORCID(orcid), userPropertiesString);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MCRORCIDUser other = (MCRORCIDUser) obj;
        return Objects.equals(user, other.user);
    }

    /**
     * Serializes MCRORCIDCredential to String.
     * 
     * @param credential MCRORCIDCredential
     * @return MCRORCIDCredential as String
     * @throws IllegalArgumentException if serialization fails
     */
    protected static String serializeCredential(MCRORCIDCredential credential) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.setSerializationInclusion(Include.NON_NULL);
            return mapper.writeValueAsString(credential);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Deserializes String to MCRORCIDCredential.
     * 
     * @param credentialString MCRORCIDCredential as String
     * @return MCRORCIDCredential
     * @throws IllegalArgumentException if deserialization fails
     */
    protected static MCRORCIDCredential deserializeCredential(String credentialString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(credentialString, MCRORCIDCredential.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String serializeUserProperties(MCRORCIDUserProperties userProperties) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.setSerializationInclusion(Include.NON_NULL);
            return mapper.writeValueAsString(userProperties);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static MCRORCIDUserProperties deserializeUserProperties(String userPropertiesString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(userPropertiesString, MCRORCIDUserProperties.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getCredentialAttributeValueByORCID(String orcid) {
        return user.getUserAttribute(getCredentialAttributeNameByORCID(orcid));
    }

    private static String getCredentialAttributeNameByORCID(String orcid) {
        return ATTR_ORCID_CREDENTIAL + orcid;
    }

    private String getUserPropertiesAttributeValueByORCID(String orcid) {
        return user.getUserAttribute(getUserPropertiesAttributeNameByORCID(orcid));
    }

    private static String getUserPropertiesAttributeNameByORCID(String orcid) {
        return ATTR_ORCID_USER_PROPERTIES + orcid;
    }
}
