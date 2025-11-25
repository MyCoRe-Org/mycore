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

package org.mycore.mods;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MCRUser2MODSPersonIdentifierService implements MCRLegalEntityService {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODSPERSON_ATTR_NAME = "id_modsperson";

    public static final String USERID = "userid";

    public static final String MODS_NAME = "name";

    public static final String MODS_NAMEIDENTIFIER = "nameIdentifier";

    public static final String TYPE = "type";


    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id connected to the modsperson
     * @return all known identifiers or an empty set
     */
    @Override
    public Set<MCRIdentifier> getAllIdentifiers(MCRIdentifier userId) {
        return getIdentifiers(userId, null);
    }

    /**
     * Gets a modsperson's {@link MCRIdentifier MCRIdentifiers} of a specified type by reference
     * to a {@link org.mycore.user2.MCRUser} and its modsperson id.
     * @param userId the user id connected to the modsperson
     * @param identifierType the type of identifier to filter for
     * @return all known identifiers of a specified type or an empty set
     */
    @Override
    public Set<MCRIdentifier> getTypedIdentifiers(MCRIdentifier userId, String identifierType) {
        return getIdentifiers(userId, identifierType);
    }

    /**
     * Adds a {@link MCRIdentifier MCRIdentifiers} to a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id connected to the modsperson
     * @param attributeToAdd the nameIdentifier to add to the modsperson
     */
    @Override
    public void addIdentifier(MCRIdentifier userId, MCRIdentifier attributeToAdd) {
        Optional<MCRObject> modspersonOptional = findModspersonByUsername(userId);
        if (modspersonOptional.isEmpty()) {
            return;
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modspersonOptional.get());
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        if (modsName == null) {
            return;
        }
        Element nameIdentifier = new Element(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
            .setAttribute(TYPE, attributeToAdd.getType())
            .setText(attributeToAdd.getValue());
        modsName.addContent(nameIdentifier);
        try {
            MCRMetadataManager.update(modspersonOptional.get());
        } catch (MCRAccessException | MCRPersistenceException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Could not update modsperson object for user id {}",
                    userId.getValue(), e);
            }
        }
    }

    /**
     * helper method to search for identifiers in a modsperson by a user-ID
     * @param userId the user id connected to the modsperson
     * @param identifierType optional type filter, leave null for no filter
     * @return a set of all identifiers found
     */
    private Set<MCRIdentifier> getIdentifiers(MCRIdentifier userId, String identifierType) {
        Optional<MCRObject> modspersonOptional = findModspersonByUsername(userId);
        if (modspersonOptional.isEmpty()) {
            return Collections.emptySet();
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modspersonOptional.get());
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        if (modsName == null) {
            return Collections.emptySet();
        }
        if (identifierType != null) {
            return modsName.getChildren(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
                .stream().filter(e -> identifierType.equals(e.getAttributeValue(TYPE)))
                .map(e -> new MCRIdentifier(e.getAttributeValue(TYPE), e.getText()))
                .collect(Collectors.toSet());
        }
        return modsName.getChildren(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
            .stream().map(e -> new MCRIdentifier(e.getAttributeValue(TYPE), e.getText()))
            .collect(Collectors.toSet());
    }

    /**
     * Takes a username and returns an Optional with the referenced modsperson.
     * @param userId the user id
     * @return a nullable Optional that might contain a modsperson
     */
    private Optional<MCRObject> findModspersonByUsername(MCRIdentifier userId) {
        if (userId == null || !USERID.equals(userId.getType())) {
            return Optional.empty();
        }
        MCRUser user = MCRUserManager.getUser(userId.getValue());
        if (user == null) {
            return Optional.empty();
        }
        String modspersonId = user.getUserAttribute(MODSPERSON_ATTR_NAME);
        if (modspersonId == null) {
            return Optional.empty();
        }
        try {
            MCRObject modsperson = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modspersonId));
            return Optional.of(modsperson);
        } catch (MCRPersistenceException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Could not retrieve modsperson object for user id {} (modspersonId={})",
                    userId.getValue(), modspersonId, e);
            }
            return Optional.empty();
        }
    }
}
