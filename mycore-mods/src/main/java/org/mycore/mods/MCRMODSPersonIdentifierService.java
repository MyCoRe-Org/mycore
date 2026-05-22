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

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

/**
 * This class identifies {@link MCRUser users} by their user ID and looks up their identifiers by loading the
 * correlating modsperson metadata through the modsperson-{@link MCRUserAttribute attribute}
 * attached to the user entity.
 * If this attribute is not present, an empty set is returned.
 * New attributes are added to the modsperson metadata and are not added to the user entity.
 */
@MCRConfigurationProxy(proxyClass = MCRMODSPersonIdentifierService.Factory.class)
public class MCRMODSPersonIdentifierService implements MCRLegalEntityService {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String MODSPERSON_ATTR_NAME = "id_modsperson";

    private static final String MODS_NAME = "name";

    private static final String MODS_NAMEIDENTIFIER = "nameIdentifier";

    private static final String TYPE = "type";

    private final MCRLegalEntityService fallbackService;

    public MCRMODSPersonIdentifierService(MCRLegalEntityService fallbackService) {
        this.fallbackService = fallbackService;
    }

    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id connected to the modsperson
     * @return all known identifiers or an empty set
     *
     * @throws MCRException if the reference to the modsperson isn't found
     */
    @Override
    public Set<MCRIdentifier> getAllIdentifiers(MCRIdentifier userId) {
        return findModspersonByUsername(userId)
            .map(this::getIdentifiersFromModsperson)
            .orElseGet(() -> getAllIdentifiersFromFallback(userId));
    }

    /**
     * Adds a {@link MCRIdentifier MCRIdentifiers} to a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id connected to the modsperson
     * @param attributeToAdd the nameIdentifier to add to the modsperson
     *
     * @throws MCRException if an identifier cannot be added to the modsperson
     */
    @Override
    public void addIdentifier(MCRIdentifier userId, MCRIdentifier attributeToAdd) throws MCRException {
        Optional<MCRObject> modspersonOptional = findModspersonByUsername(userId);

        if (modspersonOptional.isPresent()) {
            addIdentifierToModsperson(userId, attributeToAdd,modspersonOptional.get());
        } else {
            addIdentifierToFallback(userId, attributeToAdd);
        }
    }

    /**
     * Takes a username and returns an Optional with the referenced modsperson.
     * @param userId the user id
     * @return an Optional of a modsperson object
     *
     * @throws MCRException if the modsperson cannot be found via the user id or another error occurs
     */
    private Optional<MCRObject> findModspersonByUsername(MCRIdentifier userId) {
        if (userId == null || !MCRIdentifier.USER_ID_TYPE.equals(userId.getType())) {
            throw new MCRException("Invalid user id: " + userId);
        }
        MCRUser user = MCRUserManager.getUser(userId.getValue());
        if (user == null) {
            throw new MCRException("No user found with user id: " + userId);
        }
        String modspersonId = user.getUserAttribute(MODSPERSON_ATTR_NAME);
        if (modspersonId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modspersonId)));
        } catch (MCRPersistenceException e) {
            LOGGER.warn("Could not retrieve modsperson object {} for user id {}. Falling back: ",
                modspersonId, userId, e);
            return Optional.empty();
        }
    }

    private Set<MCRIdentifier> getIdentifiersFromModsperson(MCRObject modsperson) {
        Element modsName = getModsName(modsperson);
        return modsName.getChildren(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
            .stream()
            .map(e -> new MCRIdentifier(e.getAttributeValue(TYPE), e.getText()))
            .collect(Collectors.toSet());
    }

    private Element getModsName(MCRObject modsperson) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        if (modsName == null) {
            throw new MCRException("Malformed modsperson object: " + modsperson.getId());
        }
        return modsName;
    }

    /**
     * Helper method to add an {@link MCRIdentifier attribute} to a given Modsperson-{@link MCRObject object}.
     * @param userId ID of the user for logging context
     * @param attributeToAdd attribute to add to the modsperson
     * @param modsperson the modsperson object
     *
     * @throws MCRException if there's a problem while updating the modsperson
     */
    private void addIdentifierToModsperson(MCRIdentifier userId, MCRIdentifier attributeToAdd, MCRObject modsperson) {
        MCRObjectID modspersonId = modsperson.getId();
        Element modsName = getModsName(modsperson);

        boolean containsAttribute = getIdentifiersFromModsperson(modsperson).contains(attributeToAdd);

        if (containsAttribute) {
            LOGGER.warn("The attribute {} already exists in {} and will not be added again",
                attributeToAdd, modspersonId);
            return;
        }

        Element nameIdentifier = new Element(MODS_NAMEIDENTIFIER, MCRConstants.MODS_NAMESPACE)
            .setAttribute(TYPE, attributeToAdd.getType())
            .setText(attributeToAdd.getValue());

        modsName.addContent(nameIdentifier);

        try {
            MCRMetadataManager.update(modsperson);
        } catch (MCRAccessException | MCRPersistenceException e) {
            throw new MCRException("Failed to update modsperson object "
                + modspersonId + " for identifier: " + userId, e);
        }
    }

    private Set<MCRIdentifier> getAllIdentifiersFromFallback(MCRIdentifier userId) {
        if (fallbackService != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No modsperson found for user id: {} . Calling fallback service {}",
                    userId, fallbackService.getClass().getSimpleName());
            }
            return fallbackService.getAllIdentifiers(userId);
        } else {
            LOGGER.warn("No modsperson found for user id: {} and no fallback configured. "
                + "Returning empty Set.", userId);
        }
        return Set.of();
    }

    private void addIdentifierToFallback(MCRIdentifier userId, MCRIdentifier attributeToAdd) {
        if (fallbackService != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No modsperson found for user id: {} . Calling fallback service {}",
                    userId, fallbackService.getClass().getSimpleName());
            }
            fallbackService.addIdentifier(userId, attributeToAdd);
        } else {
            LOGGER.warn("No modsperson found for user id: {} and no fallback configured. "
                + "Identifier not added. ", userId);
        }
    }

    public static class Factory implements Supplier<MCRMODSPersonIdentifierService> {

        @MCRInstance(name = "Fallback", valueClass = MCRLegalEntityService.class, required = false)
        public MCRLegalEntityService fallbackService;

        @Override
        public MCRMODSPersonIdentifierService get() {
            return new MCRMODSPersonIdentifierService(fallbackService);
        }
    }
}
