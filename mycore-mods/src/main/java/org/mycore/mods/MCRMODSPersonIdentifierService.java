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

import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
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
public class MCRMODSPersonIdentifierService implements MCRLegalEntityService {

    private static final String MODSPERSON_ATTR_NAME = "id_modsperson";

    private static final String MODS_NAME = "name";

    private static final String MODS_NAMEIDENTIFIER = "nameIdentifier";

    private static final String TYPE = "type";


    /**
     * Gets all {@link MCRIdentifier MCRIdentifiers} of a modsperson by reference to a {@link org.mycore.user2.MCRUser}
     * and its modsperson id.
     * @param userId the user id connected to the modsperson
     * @return all known identifiers or an empty set
     *
     * @throws MCRException if the reference to the modsperson isn't found
     */
    @Override
    public Set<MCRIdentifier> findAllIdentifiers(MCRIdentifier userId) throws MCRException {
        return getIdentifiers(userId, null);
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
        MCRObject modsperson = findModspersonByUsername(userId);
        MCRObjectID modspersonId = modsperson.getId();
        MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        if (modsName == null) {
            throw new MCRException("Malformed modsperson object: " + modspersonId);
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

    /**
     * Helper method to search for identifiers in a modsperson by a user-ID.
     * @param userId the user id connected to the modsperson
     * @param identifierType optional type filter, leave null for no filter
     * @return a set of all identifiers found
     *
     * @throws MCRException if the reference to the modsperson isn't found
     */
    private Set<MCRIdentifier> getIdentifiers(MCRIdentifier userId, String identifierType) {
        MCRObject modsperson = findModspersonByUsername(userId);

        MCRMODSWrapper wrapper = new MCRMODSWrapper(modsperson);
        Element modsName = wrapper.getMODS().getChild(MODS_NAME, MCRConstants.MODS_NAMESPACE);
        MCRObjectID modspersonId = modsperson.getId();

        if (modsName == null) {
            throw new MCRException("Malformed modsperson object: " + modspersonId);
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
     * @return a modsperson object
     *
     * @throws MCRException if the modsperson cannot be found via the user id or another error occurs
     */
    private MCRObject findModspersonByUsername(MCRIdentifier userId) {
        if (userId == null || !MCRIdentifier.USER_ID_TYPE.equals(userId.getType())) {
            throw new MCRException("Invalid user id: " + userId);
        }
        MCRUser user = MCRUserManager.getUser(userId.getValue());
        if (user == null) {
            throw new MCRException("No user found with user id: " + userId);
        }
        String modspersonId = user.getUserAttribute(MODSPERSON_ATTR_NAME);
        if (modspersonId == null) {
            throw new MCRException("No modsperson found for user: " + userId);
        }
        try {
            return MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(modspersonId));
        } catch (MCRPersistenceException e) {
            throw new MCRException("Error accessing the modsperson object for id " + userId + ":", e);
        }
    }
}
