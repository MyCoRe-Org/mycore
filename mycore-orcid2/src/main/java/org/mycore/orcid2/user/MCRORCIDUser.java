/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import org.mycore.access.MCRAccessException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.util.MCRORCIDJSONMapper;
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
    public static final List<String> TRUSTED_NAME_IDENTIFIER_TYPES = MCRConfiguration2
        .getString(MCRORCIDConstants.CONFIG_PREFIX + "User.TrustedNameIdentifierTypes").stream()
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

    private final MCRUser user;

    private final MCRORCIDIDAttributeHandler attributeHandlerImpl;

    /**
     * Wraps MCRUser to MCRORCIDUser.
     *
     * @param user the MCRUser
     */
    public MCRORCIDUser(MCRUser user) {
        this.user = user;
        attributeHandlerImpl = MCRConfiguration2.getInstanceOfOrThrow(
            MCRORCIDIDAttributeHandler.class, MCRORCIDConstants.CONFIG_PREFIX + "AttributeHandler.Class");
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
     * @throws MCRORCIDException if ORCID iD is invalid or ORCID iD couldn't be added successfully
     */
    public void addORCID(String orcid) {
        if (!MCRORCIDValidationHelper.validateORCID(orcid)) {
            throw new MCRORCIDException("Invalid ORCID iD");
        }
        try {
            attributeHandlerImpl.addORCID(orcid, user);
        } catch (MCRAccessException e) {
            final String userId = user.getUserID();
            throw new MCRORCIDException("Failed to add ORCID iD to user " + userId + ": ", e);
        }
    }

    /** Returns user's ORCID iDs.
     *
     * @return ORCID iDs as set
     */
    public Set<String> getORCIDs() {
        return attributeHandlerImpl.getORCIDs(user);
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
        try {
            String credentialString = MCRORCIDJSONMapper.credentialToJSON(credential);
            user.setUserAttribute(getCredentialAttributeNameByORCID(orcid), credentialString);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credential is invalid", e);
        }
    }

    /**
     * Removes all MCRORCIDCredential attributes.
     */
    public void removeAllCredentials() {
        final SortedSet<MCRUserAttribute> attributes = user.getAttributes();
        final SortedSet<MCRUserAttribute> toKeep = new TreeSet<>();
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
        final SortedSet<MCRUserAttribute> toKeep = new TreeSet<>();
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
                    a -> MCRORCIDJSONMapper.jsonToCredential(a.getValue())));
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
                .map(MCRORCIDJSONMapper::jsonToCredential).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Credential is corrupt", e);
        }
    }

    /**
     * Returns users identifiers.
     * @return Set of MCRIdentifier
     */
    public Set<MCRIdentifier> getIdentifiers() {
        return attributeHandlerImpl.getIdentifiers(user);
    }

    /**
     * Returns all trusted identifiers.
     * Trusted name identifier type  can be defined as follows:
     * <p>
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
     *
     * @param orcid the ORCID iD
     * @return MCRORCIDUserProperties
     * @throws MCRORCIDException if linked ORCID iD does not exists or properties are corrupt
     */
    public MCRORCIDUserProperties getUserPropertiesByORCID(String orcid) {
        if (!getORCIDs().contains(orcid)) {
            throw new MCRORCIDException("Linked ORCID iD " + orcid + " does not exist");
        }
        try {
            return Optional.ofNullable(getUserPropertiesAttributeValueByORCID(orcid))
                .map(MCRORCIDJSONMapper::jsonToUserProperties).orElseGet(() -> new MCRORCIDUserProperties());
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Found corrupt user properites", e);
        }
    }

    /**
     * Sets MCRORCIDUserProperties for ORCID iD.
     *
     * @param orcid the ORCID iD
     * @param userProperties the MCRORCIDUserProperties
     * @throws MCRORCIDException if linked ORCID iD does not exists or properties are invalid
     */
    public void setUserProperties(String orcid, MCRORCIDUserProperties userProperties) {
        if (!getORCIDs().contains(orcid)) {
            throw new MCRORCIDException("Linked ORCID iD " + orcid + " does not exist");
        }
        try {
            String userPropertiesString = MCRORCIDJSONMapper.userPropertiesToString(userProperties);
            user.setUserAttribute(getUserPropertiesAttributeNameByORCID(orcid), userPropertiesString);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("User properties are invalid", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
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
        final MCRORCIDUser other = (MCRORCIDUser) obj;
        return Objects.equals(user, other.user);
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
